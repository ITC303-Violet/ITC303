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
}