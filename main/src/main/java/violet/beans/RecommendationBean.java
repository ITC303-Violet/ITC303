package violet.beans;

import java.util.Date;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import javax.persistence.EntityManager;
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
	private static final Double MINIMUM_WEIGHTED_RATING = 5.0D;
	private static final int MAX_RECOMMENDATION_CHUNK = 1; // The number of recommendations to generate and store for a user at a time (can be done in batches to relieve server load)
	
	private static final int SUGGESTED_NUMBER_OF_RATINGS = 5; // The number of ratings we suggest a user have so we can generate better recommendations
	
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
	
	public int getSuggestedRatings() {
		return SUGGESTED_NUMBER_OF_RATINGS;
	}
	
	public String gotoNextRecommendation() {
		if(!userBean.isAuthenticated())
			return null;
		
		User user = userBean.getUser();
		
		FactoryManager.pullTransaction();
		try {
			EntityManager em = FactoryManager.getCommonEM();
			user = em.merge(user);
			userBean.setUser(user);
			
			Recommendation nextRecommendation = null;
			if(user.getRemainingRecommendations() == 0 && (nextRecommendation = generateRecommendations(user)) == null) {
				user.setCurrentRecommendation(null);
				return "no-recommendations.xhtml?faces-redirect=true";
			}
			
			if(nextRecommendation == null)
				nextRecommendation = user.getNextRecommendation();
			user.setCurrentRecommendation(nextRecommendation);
			
			if(nextRecommendation == null)
				return "no-recommendations.xhtml?faces-redirect=true";
		} finally {
			FactoryManager.popTransaction();
		}
		
		return gotoRecommendation();
	}
	
	private final int initial_offset = 9;
	private void setEngineParameters(Query q, User user) {
		q.setParameter(1, user.getId());
		q.setParameter(2, MATCHED_GENRE_WEIGHT);
		q.setParameter(3, UNMATCHED_GENRE_WEIGHT);
		q.setParameter(4, MATCHED_CHARACTERISTIC_WEIGHT);
		q.setParameter(5, UNMATCHED_CHARACTERISTIC_WEIGHT);
		q.setParameter(6, MINIMUM_USER_RATING_SIMILARITY);
		q.setParameter(7, MAX_RECOMMENDATION_CHUNK);
		q.setParameter(8, MINIMUM_WEIGHTED_RATING);
		
		int i=initial_offset;
		for(Genre genre : user.getFavouredGenres())
			q.setParameter(i++, genre.getIdentifier());
		for(Characteristic characteristic : user.getFavouredCharacteristics())
			q.setParameter(i++, characteristic.getId());
		for(Recommendation recommendation : user.getRecommendations())
			q.setParameter(i++, recommendation.getGame().getId());
	}
	
	@SuppressWarnings("unchecked")
	public Recommendation generateRecommendations(User user) {
		FactoryManager.pullTransaction();
		EntityManager em = FactoryManager.getCommonEM();
		try {
			int offset=initial_offset;
			
			String genreParameters = "";
			for(int i=0; i<user.getFavouredGenres().size(); i++, offset++)
				genreParameters += "?" + offset + (i == user.getFavouredGenres().size()-1 ? "" : ",");
			
			String characteristicParameters = "";
			for(int i=0; i<user.getFavouredCharacteristics().size(); i++, offset++)
				characteristicParameters += "?" + offset + (i == user.getFavouredCharacteristics().size()-1 ? "" : ",");
			
			String recommendedParameters = "";
			for(int i=0; i<user.getRecommendations().size(); i++, offset++)
				recommendedParameters += "?" + offset + (i == user.getRecommendations().size()-1 ? "" : ",");
			
			
			String genreIn =
					user.getFavouredGenres().isEmpty() ?
							"FALSE" :
							"gg.genres_identifier IN (" + genreParameters + ")";
			
			String caseCharacteristicIn = 
					user.getFavouredCharacteristics().isEmpty() ? 
							"?5" : 
							"CASE WHEN r.characteristic_id IN (" + characteristicParameters + ") THEN ?4 ELSE ?5 END";
			
			String rWhereCharacteristicIn =
					user.getFavouredCharacteristics().isEmpty() ? 
							"" : 
							"OR r.characteristic_id IN (" + characteristicParameters + ")";
			
			String cgWhereCharacteristicIn =
					user.getFavouredCharacteristics().isEmpty() ? 
							"" : 
							"OR cg.characteristics_id IN (" + characteristicParameters + ")";
			
			String ratingRecommendedIn =
					user.getRecommendations().isEmpty() ?
							"" :
							"AND r.game_id NOT IN (" + recommendedParameters + ")";
			
			String gameRecommendedIn =
					user.getRecommendations().isEmpty() ?
							"" :
							"AND g.id NOT IN (" + recommendedParameters + ")";
			
			
			Query q = em.createNativeQuery(""
					+ "SELECT a.game_id, a.weighted_rating\n"
					+ "FROM (\n"
					+ "  SELECT r.game_id, (\n"
					+ "    SUM(r.rating * r_similarity *\n"
					+ "      CASE WHEN r.characteristic_id IS NOT NULL THEN\n"
					+ "        " + caseCharacteristicIn + "\n"
					+ "      ELSE\n"
					+ "        CASE WHEN gg.games_id IS NOT NULL THEN ?2 ELSE ?3 END\n"
					+ "      END\n"
					+ "    )\n"
					+ "    /\n"
					+ "    SUM(r_similarity *\n"
					+ "      CASE WHEN r.characteristic_id IS NOT NULL THEN\n"
					+ "        " + caseCharacteristicIn + "\n"
					+ "      ELSE\n"
					+ "        CASE WHEN gg.games_id IS NOT NULL THEN ?2 ELSE ?3 END\n"
					+ "      END\n"
					+ "    )\n"
					+ "  ) AS weighted_rating \n"
					+ "  FROM (\n"
					+ "    SELECT\n"
					+ "      rb.user_id,\n"
					+ "      rb.characteristic_id,\n"
					+ "      AVG(ABS(ra.rating - rb.rating)) AS delta,\n"
					+ "      1-AVG(ABS(ra.rating - rb.rating))/10 AS r_similarity\n"
					+ "    FROM rating ra\n"
					+ "    JOIN rating rb\n"
					+ "      ON\n"
					+ "        ra.game_id = rb.game_id\n"
					+ "        AND rb.user_id IS NOT NULL\n"
					+ "        AND ra.user_id != rb.user_id\n"
					+ "        AND (\n"
					+ "          ra.characteristic_id = rb.characteristic_id\n"
					+ "          OR ra.characteristic_id IS NULL AND rb.characteristic_id IS NULL\n"
					+ "        )\n"
					+ "    WHERE ra.user_id=?1\n"
					+ "    GROUP BY rb.user_id, rb.characteristic_id\n"
					+ "  ) deltas \n"
					+ "  JOIN rating r\n"
					+ "    ON\n"
					+ "      deltas.user_id = r.user_id\n"
					+ "      AND (\n"
					+ "        deltas.characteristic_id = r.characteristic_id\n"
					+ "        OR\n"
					+ "        deltas.characteristic_id IS NULL AND r.characteristic_id IS NULL\n"
					+ "      )\n"
					+ "      " + ratingRecommendedIn + "\n"
					+ "  LEFT JOIN rating target ON target.user_id=?1 AND r.game_id=target.game_id\n"
					+ "  LEFT JOIN (\n"
					+ "    SELECT DISTINCT games_id\n"
					+ "    FROM genre_game gg\n"
					+ "    WHERE " + genreIn + "\n"
					+ "  ) gg ON r.game_id=gg.games_id\n"
					+ "  LEFT JOIN game g ON g.id=r.game_id"
					+ "  WHERE\n"
					+ "    target.game_id IS NULL\n"
					+ "    AND r_similarity > ?6\n"
					+ "    AND (NOT g.blacklisted OR g.blacklisted IS NULL)"
					+ "  GROUP BY r.game_id\n"
					+ "  ORDER BY weighted_rating DESC\n"
					+ "  LIMIT ?7"
					+ ") a\n"
					+ "WHERE a.weighted_rating > ?8;");
			
			setEngineParameters(q, user);
			
			List<Object[]> results = (List<Object[]>)q.getResultList();
			
			if(results.size() == 0 && (user.getFavouredGenres().size() > 0 || user.getFavouredCharacteristics().size() > 0)) { // fall back to a less complex system that suggests games with genres and characteristics a user likes
				q = em.createNativeQuery(""
					+ "SELECT g.id, (\n"
					+ "  SUM(r.rating *\n"
					+ "    CASE WHEN r.characteristic_id IS NOT NULL THEN\n"
					+ "      ?4\n"
					+ "    ELSE\n"
					+ "      CASE WHEN gg.games_id IS NOT NULL THEN ?2 ELSE ?3 END\n"
					+ "    END\n"
					+ "  ) / SUM(\n"
					+ "    CASE WHEN r.characteristic_id IS NOT NULL THEN\n"
					+ "      ?4\n"
					+ "    ELSE\n"
					+ "      CASE WHEN gg.games_id IS NOT NULL THEN ?2 ELSE ?3 END\n"
					+ "    END\n"
					+ "  )\n"
					+ ") AS weighted_rating\n"
					+ "FROM game g\n"
					+ "LEFT JOIN rating r\n"
					+ "  ON\n"
					+ "    r.game_id=g.id\n"
					+ "    AND r.user_id IS NULL\n"
					+ "    AND (\n"
					+ "      r.characteristic_id IS NULL\n"
					+ "      " + rWhereCharacteristicIn + "\n"
					+ "    )\n"
					+ "LEFT JOIN (\n"
					+ "  SELECT DISTINCT gg.games_id\n"
					+ "  FROM genre_game gg\n"
					+ "  LEFT JOIN characteristic_genre cg ON gg.genres_identifier=cg.genres_identifier\n"
					+ "  WHERE " + genreIn + " " + cgWhereCharacteristicIn +"\n"
					+ ") gg ON g.id=gg.games_id\n"
					+ "LEFT JOIN rating target ON target.user_id=?1 AND target.game_id=g.id\n"
					+ "WHERE\n"
					+ "  (NOT g.blacklisted OR g.blacklisted IS NULL)\n"
					+ "  AND gg.games_id IS NOT NULL\n"
					+ "  " + gameRecommendedIn + "\n"
					+ "  AND target.game_id IS NULL\n"
					+ "GROUP BY g.id\n"
					+ "ORDER BY weighted_rating DESC NULLS LAST, g.release DESC\n"
					+ "LIMIT ?8");
				
				setEngineParameters(q, user);
				
				results = (List<Object[]>)q.getResultList();
			}
			
			user.setLastRecommendationGeneration(new Date());
			
			Recommendation firstRecommendation = null;
			for(Object[] row : results) {
				Long gameId = (Long)row[0];
				Double weightedRating = (Double)row[1];
				
				Recommendation recommendation = new Recommendation(user, Game.getGame(gameId, em), weightedRating);
				em.persist(recommendation);
				if(firstRecommendation == null)
					firstRecommendation = recommendation;
			}
			
			return firstRecommendation;
		} catch(NoResultException e) {
			return null;
		} finally {
			FactoryManager.popTransaction();
		}
	}
}
