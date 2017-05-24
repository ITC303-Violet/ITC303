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
	
	private int rating;
	
	public Rating() {
		
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
	}

	public Game getGame() {
		return game;
	}

	public void setGame(Game game) {
		this.game = game;
	}

	public Characteristic getCharacteristic() {
		return characteristic;
	}

	public void setCharacteristic(Characteristic characteristic) {
		this.characteristic = characteristic;
	}

	public int getRating() {
		return rating;
	}

	public void setRating(int rating) {
		this.rating = rating;
	}
}