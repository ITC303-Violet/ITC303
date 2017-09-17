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
 * Gathers game data from  Playsation Store using the Chihiro API
 * Based heavily on SteamGatherer, since it's mostly the same JSON objects query.
 * 
 * Somehow, it's still not working as expected, a bit of debugging is needed.
 * 
 * @author Erin and implemented Gatherer by somer
 */
public class PlaystationGatherer extends Gatherer {
	private static final SimpleDateFormat[] dateFormatters = { // used to process release dates, attempts each one if the one before it fails
			new SimpleDateFormat("d MMM, yyyy"),
			new SimpleDateFormat("MMM d, yyyy"),
			//Added a pattern for dates with style similar to 2018-12-31T00:00:00Z
			new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
	};
	
	EntityManager em;
	EntityTransaction transaction;
	//The URL for the Chihiro API receives the number of titles to be retrieved as "size". It gets all the info and will be revised later. (Given that
	//Steam's URL parsed the full list with only appIds and titles). We'll query only 50 each time for now.
	private static final String URL_APPLIST = "https://store.playstation.com/chihiro-api/viewfinder/US/en/19/STORE-MSF77008-ALLGAMES?size=50&gkb=1&geoCountry=US";

	private static final String COLUMN_NAME = "ps_store_id";
	
	private AtomicInteger gamesGrabbed; // keeps track of the number of games grabbed
	
	private ExecutorService executor = null;
	
	private BlockingQueue<JSONObject> fullData; // full data already queried, the list contains all the needed data.
	private BlockingQueue<String> savedIds; // ids from titles already saved
	
	public PlaystationGatherer() {
		gamesGrabbed = new AtomicInteger();
		fullData = new LinkedBlockingDeque<JSONObject>();
		savedIds = new LinkedBlockingDeque<String>();
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
			executor.execute(new PlaystationGathererRunnable());
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
	 * Grabs the list of games with the indicated size. While the way to manage this
	 * is resolved, we'll query only the first 50 games. The reasoning for this is the 
	 * list loads the full data in the list, so there's no need for querying more
	 * data once the list is loaded loaded.
	 * 
	 * @throws JSONException
	 * @throws IOException
	 */
	private void queryApps() throws JSONException, IOException {
		List<String> existing_ids = getExistingGameIds(COLUMN_NAME, String.class);
		Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Query apps " + (insertOnly ? "insert only" : "update"));
		
		JSONArray apps = jsonFromURL(URL_APPLIST).getJSONArray("links");
		for(int i=apps.length()-1; i>=0; i--) {
			JSONObject app = apps.getJSONObject(i);
			
			if(!insertOnly || !existing_ids.contains(app.getString("id")) && !fullData.contains(app)) { // if we're inserting only, ignore apps that already exist
				try {
					fullData.put(app);
				} catch(InterruptedException e) {
					return;
				}
			}
		}
	}
	
	/**
	 * Queries Chihiro api for app details
	 * @author Erin and implemented Gatherer by somer
	 */
	private class PlaystationGathererRunnable implements Runnable {
		private EntityManager em;
		
		public void run() {
			em = FactoryManager.pullCommonEM();
			FactoryManager.pullTransaction();
			try {
				JSONObject rowData;
				while((rowData = fullData.poll(QUEUE_TIMEOUT, TimeUnit.SECONDS)) != null) { // grab an id from the list to check
					Integer i = null;
					if(processAppData(rowData, em)) { // if we successfully grab a game, increment gamesGrabbed
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
		 * Processes JSONData of a Playstation app
		 * @param appId
		 * @param data
		 * @param em
		 * @return true if the app is persisted
		 * @throws JSONException
		 * @throws IOException
		 * @throws InterruptedException
		 */
		private boolean processAppData(JSONObject data, EntityManager em) throws JSONException, IOException, InterruptedException {
			
			
			if(!data.getString("bucket").equals("games")) return false;
			
			String appId = data.getString("id");
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
			
			if(game.getPSStoreId() != appId) // set the PS Store ID
				game.setPSStoreId(appId);
			
			String value = data.getString("name");
			if(game.getName() != value) // set the name
				game.setName(value);
			
			Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Processing " + appId + ":" + value);
			
			// set the description
		
			value = cleanDescription(data.getString("long_desc"));
			if(game.getDescription() != value)
				game.setDescription(value);
			
			// save the hero image
			value = data.getJSONArray("images").getJSONObject(0).getString("url");
			if(!value.isEmpty()) {
				Image heroImage = Image.saveImage(new URL(value));
				if(heroImage != null)
					game.setHeroImage(heroImage);
			}
			
			String releaseDate;
			if(data.has("release_date") && (releaseDate = data.getString("release_date")) != null) {
					/*
					 * Will implement a way to check if the date is after or before
					 * the current date, since there is no "coming_soon" flag in
					 * PS Store games.
					 *
					 *
					 * */
					value = releaseDate;
					boolean success = false;
					for(int i=0; i<dateFormatters.length; i++) { // try to process the release date with each formatter we have entered
						try {
							Date release = dateFormatters[i].parse(value);
							game.setRelease(release);
							success = true;
							break; // the first time we succeed, break
						} catch(ParseException e) {
							
							
						}
						catch(NumberFormatException ex) {
							
						}
					}
					
					if(!success)
						Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Failed to parse release date " + value);
				
			}
			
			if(!exists)
				game = em.merge(game); // if the game already exists, merge it to ensure it is up to date
			
			synchronized(this.getClass()) { // make sure only one runner enters this block at a time to stop duplicate genres in the database
				boolean commit = false;
				JSONObject metaData;
				JSONArray genres;
				if(data.has("metadata")
						&& (metaData = data.getJSONObject("metadata")) != null
						&& metaData.has("game_genre")
						&& (genres = metaData.getJSONObject("genre").getJSONArray("values")) != null) {
					commit = commit || genres.length() > 0;
					
					for(int i=0; i<genres.length(); i++) {
						
						Genre genre;
						String name = genres.getString(i);
						/*
						 * Genres are gotten as all uppercase letters, so
						 * we make the first letter uppercase, and all the others,
						 * lowercase.
						 * 
						 * */
						/*name=name.substring(0,1).toUpperCase()+
								name.substring(1,name.length()).toLowerCase();*/
						genre = Genre.getGenre(name, true, em);
						//Logger.getLogger(this.getClass().getName()).log(Level.INFO, game.getName() + " Genre " + genre.getName());
						
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
					String id = Integer.toString(screenshotData.getInt("order"));
					if(!exists || !game.hasScreenshot(id)) {
						Screenshot screenshot = new Screenshot();
						screenshot.setRemoteIdentifier(id);
						//Image thumbnail = Image.saveImage(new URL(screenshotData.getString("path_thumbnail")));
						Image image = Image.saveImage(new URL(screenshotData.getString("url")));
						
						if(image != null) {
							//screenshot.setThumbnail(thumbnail);
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
