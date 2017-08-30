package violet.beans;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import javax.persistence.Query;

import violet.jpa.Characteristic;
import violet.jpa.FactoryManager;
import violet.jpa.Game;
import violet.jpa.Genre;
import violet.jpa.Recommendation;
import violet.jpa.User;

/**
 * Provides the interface to the recommendation engine
 * @author somer
 */
@ManagedBean
@RequestScoped
public class RecommendationBean {
	@ManagedProperty(value="#{userBean}")
	private UserBean userBean;
	
	@ManagedProperty(value="#{gameBean}")
	private GameBean gameBean;
	
	private static final Double MATCHED_CHARACTERISTIC_WEIGHT = 0.8D;
	private static final Double UNMATCHED_CHARACTERISTIC_WEIGHT = 0.15D;
	private static final Double MATCHED_GENRE_WEIGHT = 1.0D;
	private static final Double UNMATCHED_GENRE_WEIGHT = 0.6D;
	private static final Double MINIMUM_USER_RATING_SIMILARITY = 0.55D; // 55%
	private static final int MAX_RECOMMENDATION_CHUNK = 1; // The number of recommendations to generate and store for a user at a time (can be done in batches to relieve server load)
	
	public GameBean getGameBean() {
		return gameBean;
	}

	public void setGameBean(GameBean gameBean) {
		this.gameBean = gameBean;
	}

	public UserBean getUserBean() {
		return userBean;
	}

	public void setUserBean(UserBean userBean) {
		this.userBean = userBean;
	}
	
	public String gotoRecommendation() {
		if(!userBean.isAuthenticated())
			return null;
		
		User user = userBean.getUser();
		Recommendation recommendation = user.getCurrentRecommendation();
		if(recommendation == null)
			return gotoNextRecommendation();
		
		return "/game.xhtml?faces-redirect=true&gameid=" + recommendation.getGame().getId();
	}
	
	public String gotoNextRecommendation() {
		if(!userBean.isAuthenticated())
			return null;
		
		User user = userBean.getUser();
		
		if(user.getRemainingRecommendations() == 0 && generateRecommendations(user) == 0)
			return null;
		
		FactoryManager.pullTransaction();
		EntityManager em = FactoryManager.getCommonEM();
		user = em.merge(user);
		user.setCurrentRecommendation(user.getNextRecommendation());
		userBean.setUser(user);
		FactoryManager.popTransaction();
		
		return gotoRecommendation();
	}
	
	public int generateRecommendations(User user) {
		FactoryManager.pullTransaction();
		EntityManager em = FactoryManager.getCommonEM();
		try {
			final int initial_offset=8;
			int offset=initial_offset;
			int i;
			String genreParameters = "";
			for(i=0; i<user.getFavouredGenres().size(); i++)
				genreParameters += "?" + (offset + i) + (i == user.getFavouredGenres().size()-1 ? "" : ",");
			offset += i;
			
			String characteristicParameters = "";
			for(i=0; i<user.getFavouredCharacteristics().size(); i++)
				characteristicParameters += "?" + (offset + i) + (i == user.getFavouredCharacteristics().size()-1 ? "" : ",");
			offset += i;
			
			String recommendedParameters = "";
			for(i=0; i<user.getRecommendations().size(); i++)
				recommendedParameters += "?" + (offset + i) + (i == user.getRecommendations().size()-1 ? "" : ",");
			
			String genreIn =
					user.getFavouredGenres().isEmpty() ?
							"FALSE" :
							"genres.genres_identifier IN (" + genreParameters + ")";
			
			String characteristicIn = 
					user.getFavouredCharacteristics().isEmpty() ? 
							"?5" : 
							"CASE WHEN r.characteristic_id IN (" + characteristicParameters + ") THEN ?4 ELSE ?5 END";
			
			String recommendedIn =
					user.getRecommendations().isEmpty() ?
							"" :
							"AND r.game_id NOT IN (" + recommendedParameters + ")";
			
			
			Query q = em.createNativeQuery(""
					+ "SELECT r.game_id, (\n"
					+ "  SUM(r.rating * r_similarity *\n"
					+ "    CASE WHEN r.characteristic_id IS NOT NULL THEN\n"
					+ "      " + characteristicIn + "\n"
					+ "    ELSE\n"
					+ "      CASE WHEN g.games_id IS NOT NULL THEN ?2 ELSE ?3 END\n"
					+ "    END\n"
					+ "  )\n"
					+ "  /\n"
					+ "  SUM(r_similarity *\n"
					+ "    CASE WHEN r.characteristic_id IS NOT NULL THEN\n"
					+ "      " + characteristicIn + "\n"
					+ "    ELSE\n"
					+ "      CASE WHEN g.games_id IS NOT NULL THEN ?2 ELSE ?3 END\n"
					+ "    END\n"
					+ "  )\n"
					+ ") AS weighted_rating \n"
					+ "FROM (\n"
					+ "  SELECT\n"
					+ "    rb.user_id,\n"
					+ "    rb.characteristic_id,\n"
					+ "    AVG(ABS(ra.rating - rb.rating)) AS delta,\n"
					+ "    1-AVG(ABS(ra.rating - rb.rating))/10 AS r_similarity\n"
					+ "  FROM rating ra\n"
					+ "  JOIN rating rb\n"
					+ "    ON\n"
					+ "      ra.game_id = rb.game_id\n"
					+ "      AND rb.user_id IS NOT NULL\n"
					+ "      AND ra.user_id != rb.user_id\n"
					+ "      AND (\n"
					+ "        ra.characteristic_id = rb.characteristic_id\n"
					+ "        OR ra.characteristic_id IS NULL AND rb.characteristic_id IS NULL\n"
					+ "      )\n"
					+ "  WHERE ra.user_id=?1\n"
					+ "  GROUP BY rb.user_id, rb.characteristic_id\n"
					+ ") deltas \n"
					+ "JOIN rating r\n"
					+ "  ON\n"
					+ "    deltas.user_id = r.user_id\n"
					+ "    AND (\n"
					+ "      deltas.characteristic_id = r.characteristic_id\n"
					+ "      OR\n"
					+ "      deltas.characteristic_id IS NULL AND r.characteristic_id IS NULL\n"
					+ "    )\n"
					+ "    " + recommendedIn + "\n"
					+ "LEFT JOIN rating target ON target.user_id=?1 AND r.game_id=target.game_id\n"
					+ "LEFT JOIN (\n"
					+ "  SELECT DISTINCT games_id\n"
					+ "  FROM genre_game genres\n"
					+ "  WHERE " + genreIn + "\n"
					+ ") g ON r.game_id = g.games_id\n"
					+ "WHERE target.game_id IS NULL AND r_similarity > ?6\n"
					+ "GROUP BY r.game_id\n"
					+ "ORDER BY weighted_rating DESC\n"
					+ "LIMIT ?7;");
			
			q.setParameter(1, user.getId());
			q.setParameter(2, MATCHED_GENRE_WEIGHT);
			q.setParameter(3, UNMATCHED_GENRE_WEIGHT);
			q.setParameter(4, MATCHED_CHARACTERISTIC_WEIGHT);
			q.setParameter(5, UNMATCHED_CHARACTERISTIC_WEIGHT);
			q.setParameter(6, MINIMUM_USER_RATING_SIMILARITY);
			q.setParameter(7, MAX_RECOMMENDATION_CHUNK);
			
			i=initial_offset;
			for(Genre genre : user.getFavouredGenres())
				q.setParameter(i++, genre.getIdentifier());
			for(Characteristic characteristic : user.getFavouredCharacteristics())
				q.setParameter(i++, characteristic.getId());
			for(Recommendation recommendation : user.getRecommendations())
				q.setParameter(i++, recommendation.getGame().getId());
			
			List<Object[]> results = (List<Object[]>)q.getResultList();
			
			for(Object[] row : results) {
				Long gameId = (Long)row[0];
				Double weightedRating = (Double)row[1];
				
				Recommendation recommendation = new Recommendation(user, Game.getGame(gameId, em), weightedRating);
				em.persist(recommendation);
			}
			
			return results.size();
		} catch(NoResultException e) {
			return 0;
		} finally {
			FactoryManager.popTransaction();
		}
	}
}
