package violet.gatherers;

import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.primefaces.json.JSONArray;
import org.primefaces.json.JSONException;
import org.primefaces.json.JSONObject;

import violet.jpa.FactoryManager;
import violet.jpa.Game;
import violet.jpa.Genre;
import violet.jpa.Image;
import violet.jpa.Screenshot;

/**
 * Gathers game data from steam using the store api
 * @author Erin and implemented Gatherer by somer
 */
public class SteamGatherer extends Gatherer {
	private static final SimpleDateFormat[] dateFormatters = { // used to process release dates, attempts each one if the one before it fails
			new SimpleDateFormat("d MMM, yyyy"),
			new SimpleDateFormat("MMM d, yyyy")
	};
	
	EntityManager em;
	EntityTransaction transaction;
	
	private static final String URL_APPLIST = "http://api.steampowered.com/ISteamApps/GetAppList/v0001/";
	private static final String URL_APPDETAILS = "http://store.steampowered.com/api/appdetails?appids=";
	private static final String COLUMN_NAME = "steam_id";
	
	private AtomicInteger gamesGrabbed; // keeps track of the number of games grabbed
	
	private ExecutorService executor = null;
	
	private BlockingQueue<Integer> ids; // ids to grab
	private BlockingQueue<Integer> savedIds; // ids already saved
	
	public SteamGatherer() {
		gamesGrabbed = new AtomicInteger();
		ids = new LinkedBlockingDeque<Integer>();
		savedIds = new LinkedBlockingDeque<Integer>();
	}
	
	private static final Pattern[] patterns = {
		Pattern.compile("<a.*?>.*?</a>"),
		Pattern.compile("<img.*?>")
	};
	
	/**
	 * Removes links and images from descriptions
	 * @param description
	 * @return cleaned description
	 */
	private static String cleanDescription(String description) {
		for(int i=0; i<patterns.length; i++)
			description = patterns[i].matcher(description).replaceAll("");
		return description;
	}
	
	public void gather(int maxGames) {
		super.gather(maxGames);
		
		try {
			queryApps();
		} catch(JSONException e) {
			Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Exception thrown during gathering", e);
			return;
		} catch(IOException e) {
			Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Exception thrown during gathering", e);
			return;
		}
		
		this.maxGames = maxGames;
		
		executor = Executors.newFixedThreadPool(THREAD_COUNT);
		for(int i=0; i<THREAD_COUNT; i++) // run our requests in multiple threads to reduce time waiting on connections
			executor.execute(new SteamGathererRunnable());
		executor.shutdown();
		
		try {
			if(!executor.awaitTermination(4, TimeUnit.DAYS)) { // Let it run for 4 days before assuming it's halted or just gone too long
				Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Gatherer failed after 4 days execution");
			}
		} catch(InterruptedException e) {
			Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Execution was interrupted");
		}
	}
	
	public void interrupt() {
		super.interrupt();
		if(executor != null)
			executor.shutdownNow();
	}
	
	/**
	 * Grabs the entire list of games (as well as hardware and other unwanted things) from the steam api
	 * @throws JSONException
	 * @throws IOException
	 */
	private void queryApps() throws JSONException, IOException {
		List<Integer> existing_ids = getExistingGameIds(COLUMN_NAME, Integer.class);
		Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Query apps " + (insertOnly ? "insert only" : "update"));
		
		JSONArray apps = jsonFromURL(URL_APPLIST).getJSONObject("applist").getJSONObject("apps").getJSONArray("app");
		for(int i=apps.length()-1; i>=0; i--) {
			Integer appid = apps.getJSONObject(i).getInt("appid");
			
			if(!insertOnly || !existing_ids.contains(appid) && !ids.contains(appid)) { // if we're inserting only, ignore apps that already exist
				try {
					ids.put(appid);
				} catch(InterruptedException e) {
					return;
				}
			}
		}
	}
	
	/**
	 * Queries steam api for app details
	 * @author Erin and implemented Gatherer by somer
	 */
	private class SteamGathererRunnable implements Runnable {
		private EntityManager em;
		
		public void run() {
			em = FactoryManager.pullCommonEM();
			FactoryManager.pullTransaction();
			try {
				Integer id;
				while((id = ids.poll(QUEUE_TIMEOUT, TimeUnit.SECONDS)) != null) { // grab an id from the list to check
					Integer i = null;
					if(retryQuerySingleApp(id, em)) { // if we successfully grab a game, increment gamesGrabbed
						i = gamesGrabbed.incrementAndGet();
//						Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Saved " + id);
					}
					
					if(i != null && i > 0 && i % BATCH_SIZE == 0) { // flush the transaction if it's overdue to keep the transaction from becoming too large
						FactoryManager.flushCommonEM();
						Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Flushed cache");
					}
					
					if(maxGames > 0 && gamesGrabbed.get() >= maxGames) // if we've grabbed as many games as we need to, break the loop
						break;
				}
			} catch(InterruptedException e) {
				FactoryManager.reopenTransaction();
				Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Gathering interrupted");
			} catch(Exception e) { // if a more substantial exception was raised
				FactoryManager.rollbackTransaction();
				Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Exception thrown during gathering", e);
				
				executor.shutdownNow(); // cancel all other runners too 
			} finally {
				FactoryManager.popTransaction();
				FactoryManager.popCommonEM();
			}
		}
		
		/**
		 * Keep trying to load and process an app until the app doesn't exist, is invalid or we reach the max number of retries
		 * @param appId app id to process
		 * @param em
		 * @return true if the app is persisted
		 * @throws InterruptedException
		 */
		private boolean retryQuerySingleApp(int appId, EntityManager em) throws InterruptedException {
			int wait = 1;
			int retries = 0;
			while(true) {
				try {
					return querySingleApp(appId, em);
				} catch(JSONException e) {
					return false;
				} catch(IOException e) { // Rate limiting
					if(retries >= MAX_RETRIES)
						return false;
					
					Logger.getLogger(this.getClass().getName()).log(Level.FINE, "IOException occurred during gathering, halting for " + wait);
					TimeUnit.SECONDS.sleep(wait);
					retries++;
					wait *= 2; // wait longer and longer if we're being rate limited
				} 
			}
		}
		
		/**
		 * Queries and attempts to process a steam app
		 * @param appId app id to process
		 * @param em
		 * @return true if the app is persisted
		 * @throws JSONException
		 * @throws IOException
		 * @throws InterruptedException
		 */
		private boolean querySingleApp(int appId, EntityManager em) throws JSONException, IOException, InterruptedException {
			String stringId = Integer.toString(appId);
			
			JSONObject data = jsonFromURL(URL_APPDETAILS + appId).getJSONObject(stringId);
			if(data == null || !data.getBoolean("success")) // the app doesn't exist
				return false;
			
			return processAppData(appId, data, em); // process the app
		}
		
		/**
		 * Processes JSONData of a steam app
		 * @param appId
		 * @param data
		 * @param em
		 * @return true if the app is persisted
		 * @throws JSONException
		 * @throws IOException
		 * @throws InterruptedException
		 */
		private boolean processAppData(int appId, JSONObject data, EntityManager em) throws JSONException, IOException, InterruptedException {
			data = data.getJSONObject("data");
			if(!data.getString("type").equals("game")) return false;
			
			appId = data.getInt("steam_appid");
			if(savedIds.contains(appId))
				return false;
			else // ensure no other runners attempt to process this same app. It's worth noting that this shouldn't be necessary and I'm unsure why I've got it - somer
				savedIds.offer(appId, QUEUE_TIMEOUT, TimeUnit.SECONDS);
			
			boolean exists = false; // check if the game exists in our database or we're inserting it
			Game game = getGame(em, COLUMN_NAME, appId);
			if(game != null)
				exists = true;
			else
				game = new Game();
			
			if(game.getSteamId() != appId) // set the steamid
				game.setSteamId(appId);
			
			String value = data.getString("name");
			if(game.getName() != value) // set the name
				game.setName(value);
			
			Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Processing " + appId + ":" + value);
			
			// set the descriptions
			value = cleanDescription(data.getString("short_description"));
			if(game.getShortDescription() != value)
				game.setShortDescription(value);
			
			value = cleanDescription(data.getString("about_the_game"));
			if(game.getDescription() != value)
				game.setDescription(value);
			
			// save the hero image
			value = data.getString("header_image");
			if(!value.isEmpty()) {
				Image heroImage = Image.saveImage(new URL(value));
				if(heroImage != null)
					game.setHeroImage(heroImage);
			}
			
			JSONObject releaseDate;
			if(data.has("release_date") && (releaseDate = data.getJSONObject("release_date")) != null) {
				if(!releaseDate.getBoolean("coming_soon")) {
					value = releaseDate.getString("date");
					boolean success = false;
					for(int i=0; i<dateFormatters.length; i++) { // try to process the release date with each formatter we have entered
						try {
							Date release = dateFormatters[i].parse(value);
							game.setRelease(release);
							success = true;
							break; // the first time we succeed, break
						} catch(ParseException e) {}
					}
					
					if(!success)
						Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Failed to parse release date " + value);
				}
			}
			
			if(!exists)
				game = em.merge(game); // if the game already exists, merge it to ensure it is up to date
			
			synchronized(this.getClass()) { // make sure only one runner enters this block at a time to stop duplicate genres in the database
				boolean commit = false;
				JSONArray genres;
				if(data.has("genres") && (genres = data.getJSONArray("genres")) != null) {
					commit = commit || genres.length() > 0;
					
					for(int i=0; i<genres.length(); i++) {
						JSONObject genreData = genres.getJSONObject(i);
						
						Genre genre;
						String name = genreData.getString("description");
						
						genre = Genre.getGenre(name, true, em);
						//Logger.getLogger(this.getClass().getName()).log(Level.INFO, game.getName() + " Genre " + genre.getName());
						
						game.addGenre(genre);
					}
				}
				
				if(data.has("categories") && (genres = data.getJSONArray("categories")) != null) {
					commit = commit || genres.length() > 0;
					
					for(int i=0; i<genres.length(); i++) {
						JSONObject genreData = genres.getJSONObject(i);
						
						Genre genre;
						String name = genreData.getString("description");
						
						genre = Genre.getGenre(name, true, em);
						Logger.getLogger(this.getClass().getName()).log(Level.INFO, game.getName() + " Genre " + genre.getName());
						
						game.addGenre(genre);
					}
				}
				
				/*
				 * oddly we can't use em.flush, it seems to cause a deadlock whereas committing the transaction
				 * and starting a new one will work to ensure the genres of another thread are up to date
				 * and we don't have duplicate INSERT statements
				 */
				if(commit) {
					FactoryManager.reopenTransaction();
				}
			}
			
			// save all screenshots
			JSONArray screenshots;
			if(data.has("screenshots") && (screenshots = data.getJSONArray("screenshots")) != null) {
				for(int i=0; i<screenshots.length(); i++) {
					JSONObject screenshotData = screenshots.getJSONObject(i);
					String id = Integer.toString(screenshotData.getInt("id"));
					if(!exists || !game.hasScreenshot(id)) {
						Screenshot screenshot = new Screenshot();
						screenshot.setRemoteIdentifier(id);
						Image thumbnail = Image.saveImage(new URL(screenshotData.getString("path_thumbnail")));
						Image image = Image.saveImage(new URL(screenshotData.getString("path_full")));
						
						if(thumbnail != null && image != null) {
							screenshot.setThumbnail(thumbnail);
							screenshot.setImage(image);
							
							game.addScreenshot(screenshot);
							
							em.persist(screenshot);
						}
					}
				}
			}
			
			return true;
		}
	}
}
