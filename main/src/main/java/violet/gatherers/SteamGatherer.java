package violet.gatherers;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.primefaces.json.JSONArray;
import org.primefaces.json.JSONException;
import org.primefaces.json.JSONObject;

import violet.jpa.Game;

public class SteamGatherer extends Gatherer {
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
					Game game = null;
					game = retryQuerySingleApp(id);
					if(game != null) {
						em.persist(game);
						i++;
						gamesGrabbed.incrementAndGet();
						Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Persisted " + id);
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
		
		private Game retryQuerySingleApp(int appId) throws InterruptedException {
			int wait = 1;
			int retries = 0;
			while(true) {
				try {
					return querySingleApp(appId);
				} catch(JSONException e) {
					return null;
				} catch(IOException e) { // Rate limiting
					if(retries >= MAX_RETRIES)
						return null;
					
					Logger.getLogger(this.getClass().getName()).log(Level.FINEST, "IOException occurred during gathering, halting for " + wait);
					TimeUnit.SECONDS.sleep(wait);
					retries++;
					wait *= 2;
				} 
			}
		}
		
		private Game querySingleApp(int appId) throws JSONException, IOException, InterruptedException {
			String stringId = Integer.toString(appId);
			
			JSONObject data = jsonFromURL(URL_APPDETAILS + appId).getJSONObject(stringId);
			if(data.getBoolean("success")) {
				data = data.getJSONObject("data");
				if(!data.getString("type").equals("game")) return null;
				
				boolean dirty = false;
				Game game = getGame(COLUMN_NAME, appId);
				
				if(game.getSteamId() != appId) { 
					game.setSteamId(appId);
					dirty = true;
				}
				
				String value = data.getString("name");
				if(game.getName() != value) {
					game.setName(value);
					dirty = true;
				}
				
				value = data.getString("short_description");
				if(game.getShortDescription() != value) {
					game.setShortDescription(value);
					dirty = true;
				}
				
				value = data.getString("detailed_description");
				if(game.getDescription() != value) {
					game.setDescription(value);
					dirty = true;
				}
				
				if(dirty)
					return game;
				return null;
			}
			
			return null;
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
		
		JSONArray apps = jsonFromURL(URL_APPLIST).getJSONObject("applist").getJSONObject("apps").getJSONArray("app");
		for(int i=0; i<apps.length(); i++) {
			int appid = apps.getJSONObject(i).getInt("appid");
			if(!insertOnly || !existing_ids.contains(appid)) {
				try {
					ids.put(appid);
				} catch(InterruptedException e) {}
			}
		}
	}
}
