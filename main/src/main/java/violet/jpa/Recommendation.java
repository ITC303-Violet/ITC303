package violet.jpa;

import java.util.Date;

import javax.persistence.*;

@Entity
@Table(
	uniqueConstraints = {
		@UniqueConstraint(columnNames={"USER_ID", "GAME_ID"})
	}
)
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
	
	private Double weightedRating;
	
	public Recommendation() {
		date = new Date();
	}
	
	public Recommendation(User user, Game game, Double weightedRating) {
		this();
		setUser(user);
		setGame(game);
		setWeightedRating(weightedRating);
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
	
	public Double getWeightedRating() {
		return weightedRating;
	}
	
	public void setWeightedRating(Double weightedRating) {
		this.weightedRating = weightedRating;
	}
}