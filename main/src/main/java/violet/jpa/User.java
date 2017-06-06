package violet.jpa;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.mindrot.jbcrypt.BCrypt;

import javax.persistence.*;

@Entity
@Table(name="VUser")
@NamedQueries({
	@NamedQuery(name="User.getOverallRating", query="SELECT r FROM Rating r WHERE r.game=:game AND r.user=:user AND r.characteristic IS NULL"),
	@NamedQuery(name="User.getCharacteristicRating", query="SELECT r FROM Rating r WHERE r.game=:game AND r.user=:user AND r.characteristic=:characteristic")
})
public class User implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Column(unique=true)
	private String username;
	
	@Column(unique=true)
	private String email;
	
	private Short age;
	private String gender;
	private String location;
	
	private String passwordHash;
	
	private boolean is_staff=true; // TODO: SWITCH BACK TO FALSE BEFORE PRODUCTION
	
	@OneToMany(mappedBy="user")
	private List<Rating> ratings;
	
	public User() {
		ratings = new ArrayList<Rating>();
		is_staff = true;
	}
	
	public User(String username, String email, String password) {
		this();
		this.username = username;
		this.email = email;
		setPassword(password);
	}
	
	public Rating getRating(Game game, Characteristic characteristic, EntityManager em) {
		TypedQuery<Rating> tq;
		if(characteristic == null)
			tq = em.createNamedQuery("User.getOverallRating", Rating.class);
		else
			tq = em.createNamedQuery("User.getCharacteristicRating", Rating.class)
				.setParameter("characteristic", characteristic);
		
		tq.setParameter("game", game)
			.setParameter("user", this);
		
		try {
			return tq.getSingleResult();
		} catch(NoResultException e) {
			return null;
		}
	}
	
	public void rateGame(Game game, Characteristic characteristic, Double rating) {
		EntityManager em = FactoryManager.getEM();
		em.getTransaction().begin();
		
		boolean created = false;
		Rating ratingObj = getRating(game, characteristic, em);
		if(ratingObj == null) {
			created = true;
			ratingObj = new Rating();
			game.addRating(ratingObj);
			addRating(ratingObj);
			if(characteristic != null)
				characteristic.addRating(ratingObj);
		}
		
		ratingObj.setRating(rating);
		
		if(created)
			em.persist(ratingObj);
		
		em.getTransaction().commit();
		em.close();
	}
	
	public String getUsername() {
		return username;
	}
	
	public void setUsername(String username) {
		this.username = username;
	}
	
	public String getEmail() {
		return email;
	}
	
	public void setEmail(String email) {
		this.email = email;
	}
	
	public Short getAge() {
		return age;
	}
	
	public void setAge(Short age) {
		this.age = age;
	}
	
	public String getGender() {
		return gender;
	}
	
	public void setGender(String gender) {
		this.gender = gender;
	}
	
	public boolean getIsStaff() {
		return is_staff;
	}
	
	public void setIsStaff(boolean is_staff) {
		this.is_staff = is_staff;
	}
	
	public String getLocation() {
		return location;
	}
	
	public void setLocation(String location) {
		this.location = location;
	}
	
	public void addRating(Rating rating) {
		if(ratings.contains(rating))
			return;
		
		ratings.add(rating);
		
		User current = rating.getUser();
		if(current != null)
			current.getRatings().remove(rating);
		
		rating.setUser(this);
	}
	
	public List<Rating> getRatings() {
		return ratings;
	}
	
	/**
	 * Hashes a password, and sets the user's passwordHash to the output
	 * @param password
	 */
	public void setPassword(String password) {
		String salt = BCrypt.gensalt(); // The salt is embedded in the output hash string
		passwordHash = BCrypt.hashpw(password, salt);
	}
	
	/**
	 * Checks a password is correct or not
	 * @param password
	 * @return true if password is correct, false otherwise
	 */
	public boolean checkPassword(String password) {
		return BCrypt.checkpw(password, passwordHash);
	}
}
