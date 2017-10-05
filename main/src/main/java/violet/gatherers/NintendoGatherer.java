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

import org.primefaces.json.JSONException;
import org.wikibooks.ssl.SSLUtilities;

import violet.controllers.xml.NintendoListReader;
import violet.controllers.xml.XMLReader;
import violet.controllers.xml.XMLTag;
import violet.jpa.FactoryManager;
import violet.jpa.Game;
import violet.jpa.Genre;
import violet.jpa.Image;
import violet.jpa.Screenshot;

/**
 * Gathers game data from Nintendo using the Samurai API.
 * This API is the only one so far to return data as XML
 * instead of JSON.
 * 
 * @author Erin and implemented Gatherer by somer
 */
public class NintendoGatherer extends Gatherer {
	private static final SimpleDateFormat[] dateFormatters = { // used to process release dates, attempts each one if the one before it fails
			new SimpleDateFormat("d MMM, yyyy"),
			new SimpleDateFormat("MMM d, yyyy"),
			new SimpleDateFormat("yyyy-MM-dd")
	};
	
	EntityManager em;
	EntityTransaction transaction;
	
	private static final String URL_APPLIST = "https://samurai.ctr.shop.nintendo.net/samurai/ws/US/titles/?shop_id=2";
	private static final String URL_APPDETAILS = "https://samurai.ctr.shop.nintendo.net/samurai/ws/US/title/{{contentId}}/?shop_id=2";
	private static final String COLUMN_NAME = "nintendo_id";
	
	private AtomicInteger gamesGrabbed; // keeps track of the number of games grabbed
	
	private ExecutorService executor = null;
	
	private BlockingQueue<String> ids; // ids to grab
	private BlockingQueue<String> savedIds; // ids already saved
	
	public NintendoGatherer() {
		/*Call to SSLUtilties methods for avoiding the validation Exception:
		javax.net.ssl.SSLHandshakeException: sun.security.validator.ValidatorException: PKIX path building failed: sun.security.provider.certpath.SunCertPathBuilderException: unable to find valid certification path to requested target
		*/
		SSLUtilities.trustAllHostnames();
		SSLUtilities.trustAllHttpsCertificates();

		gamesGrabbed = new AtomicInteger();
		ids = new LinkedBlockingDeque<String>();
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
			executor.execute(new NintendoGathererRunnable());
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
	 * Reads the list of games from the specified URL. It will return
	 * only a set of 50 items each time. 
	 *
	 * @throws JSONException
	 * @throws IOException
	 */
	private void queryApps() throws JSONException, IOException {
		final List<Integer> existing_ids = getExistingGameIds(COLUMN_NAME, Integer.class);
		Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Query apps " + (insertOnly ? "insert only" : "update"));
		
		NintendoListReader reader=new NintendoListReader() {

			@Override
			public void onTitlesLoaded(List<XMLTag> apps) {
				for(int i=apps.size()-1; i>=0; i--) {
					String contentid = apps.get(i).getChild("title").getAttribute("id").getValue();
					
					if(!insertOnly || !existing_ids.contains(contentid) && !ids.contains(contentid)) { // if we're inserting only, ignore apps that already exist
						try {
							ids.put(contentid);
						} catch(InterruptedException e) {
							return;
						}
					}
				}
			}
		};
		reader.query();
/*		XMLReader<XMLTag> reader=new XMLReader<XMLTag>(URL_APPLIST,"content") {

			@Override
			public XMLTag parseObject(XMLTag mainTag) {
				return mainTag;
			}};
			List<XMLTag> apps = reader.readElementList();	*/
	
		
	}
	
	
	
	
	
	
	
	
	/**
	 * Queries Nintendo Samurai API for more details on titles.
	 * @author Erin and implemented Gatherer by somer
	 */
	private class NintendoGathererRunnable implements Runnable {
		private EntityManager em;
		
		public void run() {
			em = FactoryManager.pullCommonEM();
			FactoryManager.pullTransaction();
			try {
				String id;
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
		private boolean retryQuerySingleApp(String appId, EntityManager em) throws InterruptedException {
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
		 * Queries and attempts to process a Nintendo title.
		 * @param appId app id to process
		 * @param em
		 * @return true if the app is persisted
		 * @throws JSONException
		 * @throws IOException
		 * @throws InterruptedException
		 */
		private boolean querySingleApp(String appId, EntityManager em) throws JSONException, IOException, InterruptedException {
			XMLReader<XMLTag> reader=new XMLReader<XMLTag>(URL_APPDETAILS.replace("{{contentId}}", appId),"title") {

				@Override
				public XMLTag parseObject(XMLTag mainTag) {
					return mainTag;
				}};
				XMLTag data=reader.readElementList().get(0);
			if(data == null) // the app doesn't exist
				return false;
			
			return processAppData(appId, data, em); // process the app
		}
		
		private static final String WUP="WUP"; //WUP is the device ID for Nintendo Wii U titles
		private static final String CTR="CTR"; //CTR is the device ID for Nintendo 3DS titles
		
		/**
		 * Processes XML data of a Nintendo title.
		 * @param appId
		 * @param data
		 * @param em
		 * @return true if the app is persisted
		 * @throws JSONException
		 * @throws IOException
		 * @throws InterruptedException
		 */
		private boolean processAppData(String contentId, XMLTag data, EntityManager em) throws JSONException, IOException, InterruptedException {

			if(data.getChild("display_genre").getValue().equalsIgnoreCase("Update")) return false;
			String platformDevice=data.getChild("platform").getAttribute("device").getValue();
			String appCode = String.valueOf(data.getChild("product_code").getValue());
			if(savedIds.contains(contentId))
				return false;
			else // ensure no other runners attempt to process this same app. It's worth noting that this shouldn't be necessary and I'm unsure why I've got it - somer
				savedIds.offer(contentId, QUEUE_TIMEOUT, TimeUnit.SECONDS);
			
			boolean exists = false; // check if the game exists in our database or we're inserting it
			Game game = getGame(em, COLUMN_NAME, appCode);
			if(game != null)
				exists = true;
			else
				game = new Game();
			
			if(game.getNintendoId() != appCode) // set the steamid
				game.setNintendoId(appCode);
			
			String value = data.getChild("name").getValue();
			if(game.getName() != value) // set the name
				game.setName(value);
			
			Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Processing " + appCode + ":" + value);
			
			// set the description
			
			value = cleanDescription(data.getChild("description").getValue());
			if(game.getDescription() != value)
				game.setDescription(value);
			
			// save the banner or icon image
			if(data.hasChild("banner_url")) {
				value = data.getChild("banner_url").getValue();
				if(!value.isEmpty()) {
					Image heroImage = Image.saveImage(new URL(value));
					if(heroImage != null)
						game.setHeroImage(heroImage);
				}
			}
			else if(data.hasChild("icon_url")) {
				value = data.getChild("icon_url").getValue();
				if(!value.isEmpty()) {
					Image heroImage = Image.saveImage(new URL(value));
					if(heroImage != null)
						game.setHeroImage(heroImage);
				}
			}
			XMLTag releaseDate;
			if(data.hasChild("release_date_on_retail")) {
				releaseDate=data.getChild("release_date_on_retail");
					value = releaseDate.getValue();
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
			
			if(!exists)
				game = em.merge(game); // if the game already exists, merge it to ensure it is up to date
			
			synchronized(this.getClass()) { // make sure only one runner enters this block at a time to stop duplicate genres in the database
				boolean commit = false;
				XMLTag genres;
				if(data.hasChild("genres")) {
					genres=data.getChild("genres");
					commit = commit || genres.getChildren().size() > 0;
					
					for(int i=0; i<genres.getChildren().size(); i++) {
						XMLTag genreData = genres.getChildren().get(i);
						
						Genre genre;
						String name = genreData.getChild("name").getValue();
						
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
			XMLTag screenshots;
			if(data.hasChild("screenshots")) {
				screenshots=data.getChild("screenshots");
				for(int i=0; i<screenshots.getChildren().size(); i++) {
					XMLTag screenshotData = screenshots.getChildren().get(i);
					String id = Integer.toString(i+1);
					if(!exists || !game.hasScreenshot(id)) {
						Screenshot screenshot = new Screenshot();
						screenshot.setRemoteIdentifier(id);
						
						Image thumbnail;
						Image image;
						//If the device is CTR (3DS), we get upper image for image and lower image for thumbnail
						if(platformDevice.equals(CTR)) {
							image = Image.saveImage(new URL(screenshotData.getChildWithAttribute("type", "upper").getValue()));
							thumbnail = Image.saveImage(new URL(screenshotData.getChildWithAttribute("type", "upper").getValue()));
						} else {
							image = Image.saveImage(new URL(screenshotData.getChild("image_url").getValue()));
							thumbnail = Image.saveImage(new URL(screenshotData.getChild("thumbnail_url").getValue()));	
						}
						
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
