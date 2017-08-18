package violet.jpa;

import java.util.Date;

import javax.persistence.*;

@Entity
public class Recommendation {
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;
	
	@ManyToOne
	private User user;
	
	@ManyToOne
	private Game game;
	
	@Temporal(TemporalType.TIMESTAMP)
	private Date date;
	
	public Recommendation() {
		
	}

	public Long getId() {
		return id;
	}

	public User getUser() {
		return user;
	}
	
	public void setUser(User user) {
		this.user = user;
		user.addRecommendation(this);
	}

	public Game getGame() {
		return game;
	}
	
	public void setGame(Game game) {
		this.game = game;
	}
	
	public void setDate(Date date) {
		this.date = date;
	}
	
	public Date getDate() {
		return date;
	}
	
	public static boolean generateRecommendations(User user) {
		EntityManager em = FactoryManager.pullCommonEM();
		try {
			// Starts of a recommendation engine massive query:
			/*
			SELECT
			  r.game_id,
			  AVG(r.rating) AS average_rating,
			  (
			    SUM(r.rating * (perc * 1.5 +
			      CASE WHEN r.characteristic_id IS NOT NULL THEN
			          CASE WHEN r.characteristic_id IN (6301) THEN 1 ELSE 0.2 END
			      ELSE
			        CASE WHEN g.games_id IS NOT NULL THEN 1 ELSE 0.6 END
			      END
			    ))
			    /
			    SUM(perc * 1.5 +
			      CASE WHEN r.characteristic_id IS NOT NULL THEN
			        CASE WHEN r.characteristic_id IN (6301) THEN 1 ELSE 0.2 END
			      ELSE
			        CASE WHEN g.games_id IS NOT NULL THEN 1 ELSE 0.6 END
			      END
			    )
			  ) AS weighted_rating
			FROM (
			  SELECT
			    rb.user_id,
			    rb.characteristic_id,
			    AVG(ABS(ra.rating - rb.rating)) AS delta,
			    1-AVG(ABS(ra.rating - rb.rating))/10 AS perc
			  FROM rating ra
			    JOIN rating rb
			      ON
			        ra.game_id = rb.game_id
			        AND rb.user_id IS NOT NULL
			        AND ra.user_id != rb.user_id
			        AND
			        (ra.characteristic_id = rb.characteristic_id OR ra.characteristic_id IS NULL AND rb.characteristic_id IS NULL)
			  WHERE ra.user_id = 1
			  GROUP BY
			    rb.user_id, rb.characteristic_id
			  ORDER BY
			    delta ASC
			) deltas
			JOIN rating r ON deltas.user_id=r.user_id AND (deltas.characteristic_id=r.characteristic_id OR deltas.characteristic_id IS NULL AND r.characteristic_id IS NULL)
			  LEFT JOIN rating target ON target.user_id = 1 AND r.game_id=target.game_id
			LEFT JOIN (
			    SELECT DISTINCT games_id
			    FROM
			      genre_game genres
			    WHERE genres.genres_identifier IN ('rpg', 'single-player')
			    ) g ON r.game_id = g.games_id
			WHERE target.game_id IS NULL AND perc > 0.55
			GROUP BY r.game_id
			ORDER BY weighted_rating DESC;
			 */
			
			// A basic recommendation engine if we don't have enough users or ratings.
			// Simply finds and sorts games based on ratings of games with genres or characteristics
			// that the user has marked as their favourites
			
			/*List<Characteristic> favCharacteristics = userBean.getUser().getFavouredCharacteristics();
			
			List<String> favGenreIdentifiers = new ArrayList<>();
			for(Genre genre : userBean.getUser().getFavouredGenres())
				favGenreIdentifiers.add(genre.getIdentifier());
			
			TypedQuery<Game> tq = em.createQuery("SELECT g " +
					"FROM Game g " +
					"LEFT JOIN Rating r " +
					" ON " +
					"  r.game=g AND ( " +
					"   r.characteristic IS NULL " +
					(favCharacteristics.size() > 0 ? "   OR r.characteristic IN :characteristics " : "") +
					"  ) " +
					" WHERE " +
					"  (NOT g.blacklisted OR g.blacklisted IS NULL) " +
					(favGenreIdentifiers.size() > 0 ? 
						"  AND EXISTS ( " +
						"   SELECT ge FROM Genre ge WHERE ge.identifier IN :genres AND g MEMBER OF ge.games" +
						"  ) "
						: ""
					) +
					"GROUP BY g " +
					"ORDER BY AVG(r.rating) DESC NULLS LAST", Game.class);
			
			if(favCharacteristics.size() > 0)
				tq.setParameter("characteristics", favCharacteristics);
			if(favGenreIdentifiers.size() > 0)
				tq.setParameter("genres", favGenreIdentifiers);*/
			
			return true;
		} catch(NoResultException e) {
			return false;
		} finally {
			FactoryManager.popCommonEM();
		}
	}
}