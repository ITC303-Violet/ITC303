package violet.jpa;

import javax.persistence.*;

@Entity
public class Rating {
	@EmbeddedId
	private RatingKey id;
	
	@ManyToOne(optional=true)
	@MapsId("userId")
	private User user;
	
	@ManyToOne
	@MapsId("gameId")
	private Game game;
	
	@ManyToOne(optional=true)
	@MapsId("characteristicId")
	private Characteristic characteristic;
	
	private Double rating;
	
	public Rating() {
		
	}
	
	@PostPersist
	public void updateAverage() {
		if(game == null || user == null) // Don't wind up in a loop updating averages
			return;
		
		EntityManager em = FactoryManager.getCommonEM();
		EntityTransaction et = em.getTransaction();
		et.begin();
		if(et.isActive()) {
			Rating average;
			average = game.getAverageCharacteristicRating(characteristic, em);
			
			if(average == null) {
				average = new Rating();
				average.setGame(game);
				average.setCharacteristic(characteristic);
				average.setRating(rating);
			} else {
				average.setRating((average.getRating() + rating)/2);
				em.persist(average);
			}
		}
		et.commit();
	}

	public RatingKey getId() {
		return id;
	}

	public void setId(RatingKey id) {
		this.id = id;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
		user.addRating(this);
	}

	public Game getGame() {
		return game;
	}

	public void setGame(Game game) {
		this.game = game;
		game.addRating(this);
	}

	public Characteristic getCharacteristic() {
		return characteristic;
	}

	public void setCharacteristic(Characteristic characteristic) {
		this.characteristic = characteristic;
		characteristic.addRating(this);
	}

	public Double getRating() {
		return rating;
	}

	public void setRating(Double rating) {
		this.rating = rating;
	}
}