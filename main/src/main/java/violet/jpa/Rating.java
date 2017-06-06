package violet.jpa;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.*;

@Entity
public class Rating {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
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
	
	@PostUpdate
	@PostPersist
	public void updateAverage() {
		if(game == null || user == null) // Don't wind up in a loop updating averages
			return;
		
		EntityManager em = FactoryManager.getEM();
		
		Double average = 0.0D;
		try {
			String query = "SELECT AVG(r.rating) FROM Rating r WHERE r.user IS NOT NULL AND r.game=:game AND r.characteristic";
			
			if(characteristic == null)
				query += " IS NULL";
			else
				query += "=:characteristic";
			
			if(id != null) // Lets us update the provided average from the db with the rating stored in this entity (postupdate doesn't fire after the commit)
				query += " AND r.id!=:id";
			
			TypedQuery<Double> tq;
			tq = em.createQuery(query, Double.class).setParameter("game", game);
			
			if(characteristic != null)
				tq.setParameter("characteristic", characteristic);
			
			if(id != null)
				tq.setParameter("id", id);
			
			average = tq.getSingleResult();
			
			if(average == null)
				average = rating;
			else
				average = (average + rating) / 2;
		} catch(NoResultException e) {
			average = rating;
		} 
		
		EntityTransaction et = em.getTransaction();
		et.begin();
		if(et.isActive()) {
			Rating averageRating;
			averageRating = game.getAverageCharacteristicRating(characteristic, em);
			
			if(averageRating == null) {
				averageRating = new Rating();
				averageRating.setGame(game);
				averageRating.setCharacteristic(characteristic);
				averageRating.setRating(average);
				em.persist(averageRating);
			} else {
				averageRating.setRating(average);
			}
		}
		et.commit();
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
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