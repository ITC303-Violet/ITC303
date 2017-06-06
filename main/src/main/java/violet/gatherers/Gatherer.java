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

public class Gatherer {
	protected static final int BATCH_SIZE = 50;
	protected static final int MAX_RETRIES = 10;
	protected static final long QUEUE_TIMEOUT = 10;
	protected static final int THREAD_COUNT = 4;
	
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
	
	protected static JSONObject jsonFromURL(String url) throws IOException, JSONException {
		HttpURLConnection connection = null;
		InputStream input = null;
		try {
			connection = (HttpURLConnection)new URL(url).openConnection();
			connection.connect();
			input = connection.getInputStream();
		} catch(IOException e) {
			if(connection != null && connection.getResponseCode() != 429)
				return null;
			throw e;
		}
		
		try {
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
	
	protected <T> List<T> getExistingGameIds(String column, Class<T> type) {
		EntityManager em = FactoryManager.getEM();
		try {
			List<T> result = getExistingGameIds(em, column, type);
			return result;
		} finally {
			em.close();
		}
	}
	
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
