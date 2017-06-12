package violet.gatherers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import org.primefaces.json.JSONException;
import org.primefaces.json.JSONObject;

import violet.jpa.FactoryManager;
import violet.jpa.Game;

/**
 * The base class for scrapers/api consumers used to gather game data from external sites
 * @author somer
 */
public class Gatherer {
	protected static final int BATCH_SIZE = 50; // The number of games to save before flushing a batch
	protected static final int MAX_RETRIES = 10; // The maximum number of times to retry grabbing a game before moving on
	protected static final long QUEUE_TIMEOUT = 10; // The amount of time to wait for the next game id before failing
	protected static final int THREAD_COUNT = 4; // The number of threads to run gatherers within
	
	protected boolean interrupted = false;

	protected boolean insertOnly = true;
	
	protected int maxGames = -1;
	
	public void gather() {
		gather(-1);
	}
	
	public void gather(boolean insertOnly) {
		gather(insertOnly, -1);
	}
	
	public void gather(int maxGames) {
		this.maxGames = maxGames;
	}
	
	public void gather(boolean insertOnly, int maxGames) {
		this.insertOnly = insertOnly;
		gather(maxGames);
	}
	
	/**
	 * 
	 * @param url
	 * @return a JSONObject downloaded from url
	 * @throws IOException if the connection is rate limited
	 * @throws JSONException if JSON data is invalid
	 */
	protected static JSONObject jsonFromURL(String url) throws IOException, JSONException {
		HttpURLConnection connection = null;
		InputStream input = null;
		
		try {
			connection = (HttpURLConnection)new URL(url).openConnection();
			connection.connect();
			input = connection.getInputStream();
		} catch(IOException e) { // connection error
			if(connection != null && connection.getResponseCode() != 429)
				return null;
			throw e; // Only raise exception if response is 429 (rate limited)
		}
		
		try { // process the json string input
			InputStreamReader streamReader = new InputStreamReader(input, Charset.forName("UTF-8"));
			BufferedReader bufferedReader = new BufferedReader(streamReader);
			String jsonString = readerToString(bufferedReader);
			return new JSONObject(jsonString);
		} finally {
			input.close();
		}
	}
	
	private static String readerToString(Reader reader) throws IOException {
		StringBuilder stringBuilder = new StringBuilder();
		
		int character;
		while((character = reader.read()) != -1) {
			stringBuilder.append((char)character);
		}
		
		return stringBuilder.toString();
	}
	
	/**
	 * Overload of getExistingGameIds(EntityManager em, String column, Class<T> type)
	 * Provides a new EntityManager
	 * @param column
	 * @param type
	 * @return
	 */
	protected <T> List<T> getExistingGameIds(String column, Class<T> type) {
		EntityManager em = FactoryManager.getEM();
		try {
			List<T> result = getExistingGameIds(em, column, type);
			return result;
		} finally {
			em.close();
		}
	}
	
	/**
	 * A list of games
	 * @param em EntityManager to use for query
	 * @param column the column name for ids used by the provider 
	 * @param type the object type used to store the column
	 * @return a list of ids of existing games
	 */
	protected <T> List<T> getExistingGameIds(EntityManager em, String column, Class<T> type) {
		try {			
			TypedQuery<T> tq = em.createQuery("SELECT g." + column + " FROM Game g WHERE g." + column + " IS NOT NULL", type);
			List<T> result = tq
					.getResultList();
			return result;
		} catch(NoResultException e) {
			return new ArrayList<T>();
		}
	}
	
	/**
	 * Find a game from a particular provider
	 * @param em EntityManager to use for the query
	 * @param column the column name for ids used by the provider
	 * @param id the id of the game
	 * @return a game with column matching id
	 */
	protected <T> Game getGame(EntityManager em, String column, T id) {
		try {
			TypedQuery<Game> tq = em.createQuery("SELECT g FROM Game g WHERE g." + column + "=:id", Game.class);
			Game result = tq
					.setParameter("id", id)
					.getSingleResult();
			return result;
		} catch(NoResultException e) {
			return null;
		}
	}

	public void interrupt() {
		interrupted = true;
	}
}
