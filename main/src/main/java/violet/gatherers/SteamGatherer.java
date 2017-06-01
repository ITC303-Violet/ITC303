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

import violet.jpa.Game;
import violet.jpa.Image;
import violet.jpa.Screenshot;

public class SteamGatherer extends Gatherer {
	private static final SimpleDateFormat[] dateFormatters = {
			new SimpleDateFormat("d MMM, yyyy"),
			new SimpleDateFormat("MMM d, yyyy")
	};
	
	private class SteamGathererRunnable implements Runnable {
		public void run() {
			EntityManager em = getJPAEMF().createEntityManager();
			EntityTransaction transaction = null;
			try {
				transaction = em.getTransaction();
				transaction.begin();
				Integer id;
				int i = 0;
				while((id = ids.poll(QUEUE_TIMEOUT, TimeUnit.SECONDS)) != null) {
					if(retryQuerySingleApp(id, em)) {
						i++;
						gamesGrabbed.incrementAndGet();
						Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Saved " + id);
					}
					
					if(i > 0 && i % BATCH_SIZE == 0) {
						em.flush();
						em.clear();
						Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Flushed cache");
					}
					
					if(maxGames > 0 && gamesGrabbed.get() >= maxGames)
						break;
				}
				
				transaction.commit();
			} catch(InterruptedException e) {
				if(transaction != null)
					transaction.commit();
				Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Gathering interrupted");
			} catch(Exception e) {
				if(transaction != null)
					transaction.rollback();
				Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Exception thrown during gathering", e);
			} finally {
				em.close();
			}
		}
		
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
					wait *= 2;
				} 
			}
		}
		
		private boolean querySingleApp(int appId, EntityManager em) throws JSONException, IOException, InterruptedException {
			String stringId = Integer.toString(appId);
			
			JSONObject data = jsonFromURL(URL_APPDETAILS + appId).getJSONObject(stringId);
			if(data == null)
				return false;
			
			if(data.getBoolean("success")) {
				data = data.getJSONObject("data");
				if(!data.getString("type").equals("game")) return false;
				
				boolean exists = false;
				Game game = getGame(em, COLUMN_NAME, appId);
				if(game != null)
					exists = true;
				else
					game = new Game();
				
				if(game.getSteamId() != appId)
					game.setSteamId(appId);
				
				String value = data.getString("name");
				if(game.getName() != value)
					game.setName(value);
				
				value = cleanDescription(data.getString("short_description"));
				if(game.getShortDescription() != value)
					game.setShortDescription(value);
				
				value = cleanDescription(data.getString("about_the_game"));
				if(game.getDescription() != value)
					game.setDescription(value);
				
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
						for(int i=0; i<dateFormatters.length; i++) {
							try {
								Date release = dateFormatters[i].parse(value);
								game.setRelease(release);
								break;
							} catch(ParseException e) {
								Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Failed to parse release date " + value);
							}
						}
					}
				}
				
				
				if(!exists)
					em.persist(game);
				
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
			
			return false;
		}
	}
	
	private static final String URL_APPLIST = "http://api.steampowered.com/ISteamApps/GetAppList/v0001/";
	private static final String URL_APPDETAILS = "http://store.steampowered.com/api/appdetails?appids=";
	private static final String COLUMN_NAME = "steam_id";
	
	private AtomicInteger gamesGrabbed;
	
	private ExecutorService executor = null;
	
	private BlockingQueue<Integer> ids;
	
	public SteamGatherer() {
		gamesGrabbed = new AtomicInteger();
		ids = new LinkedBlockingDeque<Integer>();
	}
	
	private static final Pattern[] patterns = {
		Pattern.compile("<a.*?>.*?</a>")
	};
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
		for(int i=0; i<THREAD_COUNT; i++)
			executor.execute(new SteamGathererRunnable());
		executor.shutdown();
		
		try {
			if(!executor.awaitTermination(4, TimeUnit.DAYS)) {
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
	
	private void queryApps() throws JSONException, IOException {
		List<Integer> existing_ids = getExistingGameIds(COLUMN_NAME, Integer.class);
		Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Query apps " + (insertOnly ? "insert only" : "update"));
		
		JSONArray apps = jsonFromURL(URL_APPLIST).getJSONObject("applist").getJSONObject("apps").getJSONArray("app");
		for(int i=0; i<apps.length(); i++) {
			Integer appid = apps.getJSONObject(i).getInt("appid");
			
			if(!insertOnly || !existing_ids.contains(appid)) {
				try {
					ids.put(appid);
				} catch(InterruptedException e) {
					return;
				}
			}
		}
	}
}
