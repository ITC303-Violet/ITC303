package violet.gatherers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.NoResultException;
import javax.persistence.Persistence;
import javax.persistence.TypedQuery;

import org.primefaces.json.JSONException;
import org.primefaces.json.JSONObject;

import violet.jpa.Game;

public class Gatherer {
	private EntityManagerFactory emf;
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
		InputStream input = new URL(url).openStream();
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
	
	protected EntityManagerFactory getJPAEMF() {
		if(emf == null)
			emf = Persistence.createEntityManagerFactory("default");
		
		return emf;
	}
	
	protected <T> List<T> getExistingGameIds(String column, Class<T> type) {
		EntityManager em = getJPAEMF().createEntityManager();
		
		try {			
			TypedQuery<T> tq = em.createQuery("SELECT g." + column + " FROM Game g WHERE g." + column + " IS NOT NULL", type);
			List<T> result = tq
					.getResultList();
			return result;
		} catch(NoResultException e) {
			return new ArrayList<T>();
		} finally {
			em.close();
		}
	}
	
	protected <T> Game getGame(String column, T id) {
		EntityManager em = getJPAEMF().createEntityManager();
		
		try {
			TypedQuery<Game> tq = em.createQuery("SELECT g FROM Game g WHERE g." + column + "=:id", Game.class);
			Game result = tq
					.setParameter("id", id)
					.getSingleResult();
			return result;
		} catch(NoResultException e) {
			return new Game();
		} finally {
			em.close();
		}
	}

	public void interrupt() {
		interrupted = true;
	}
}
