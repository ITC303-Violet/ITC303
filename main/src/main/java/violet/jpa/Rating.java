package violet.jpa;

import javax.persistence.*;

/**
 * Stores ratings our users leave on games
 * @author somer
 */
@Entity
public class Rating {
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;
	
	@ManyToOne(optional=true)
	private User user;
	
	@ManyToOne
	private Game game;
	
	@ManyToOne(optional=true)
	private Characteristic characteristic;
	
	private Double rating;
	
	public Rating() {
		
	}
	
	/**
	 * Updates the saved average rating of a game after a user updates or creates their vote
	 */
	//@PreUpdate
	//@PrePersist
	public void updateAverage() {
		if(game == null || user == null) // Don't wind up in a loop updating averages after saving an average
			return;
		
		EntityManager em = FactoryManager.getCommonEM();
		
		Double average = 0.0D;
		try {
			String query = "SELECT AVG(r.rating) FROM Rating r WHERE r.user IS NOT NULL AND r.game=:game AND r.characteristic";
			
			if(characteristic == null) // if it's an overall rating
				query += " IS NULL";
			else // or not
				query += "=:characteristic";
			
			if(id != null) // we manually add the value of the rating of "this" rating
				query += " AND r.id!=:id"; // so for the time being ignore any row with the same id as "this"
			
			TypedQuery<Double> tq;
			tq = em.createQuery(query, Double.class).setParameter("game", game);
			
			if(characteristic != null)
				tq.setParameter("characteristic", characteristic);
			
			if(id != null)
				tq.setParameter("id", id);
			
			average = tq.getSingleResult();
			
			if(average == null) // there's no other ratings
				average = rating;
			else // doctor the average with the rating found in "this"
				average = (average + rating) / 2;
		} catch(NoResultException e) {
			average = rating; // there's no other ratings
		}
		
		Rating averageRating = game.getAverageCharacteristicRating(characteristic, em);
		
		FactoryManager.pullTransaction();
		if(averageRating == null) { // the average doesn't exist, make it
			averageRating = new Rating();
			averageRating.setGame(game);
			averageRating.setCharacteristic(characteristic);
			averageRating.setRating(average);
			em.persist(averageRating);
		} else { // update the average
			averageRating.setRating(average);
		}
		FactoryManager.popTransaction();
	}

	public Long getId() {
		return id;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		setUser(user, false);
	}
	
	public void setUser(User user, boolean reflect) {
		this.user = user;
		if(reflect)
			user.addRating(this);
	}

	public Game getGame() {
		return game;
	}

	public void setGame(Game game) {
		setGame(game, false);
	}
	
	public void setGame(Game game, boolean reflect) {
		this.game = game;
		if(reflect)
			game.addRating(this);
	}

	public Characteristic getCharacteristic() {
		return characteristic;
	}

	public void setCharacteristic(Characteristic characteristic) {
		setCharacteristic(characteristic, false);
	}
	
	public void setCharacteristic(Characteristic characteristic, boolean reflect) {
		this.characteristic = characteristic;
		if(reflect)
			characteristic.addRating(this);
	}

	public Double getRating() {
		return rating;
	}

	public void setRating(Double rating) {
		this.rating = rating;
	}
}