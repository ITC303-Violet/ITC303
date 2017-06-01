package violet.jpa;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.mindrot.jbcrypt.BCrypt;

import javax.persistence.*;

@Entity
@Table(name="VUser")
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
	}
	
	public User(String username, String email, String password) {
		this();
		this.username = username;
		this.email = email;
		setPassword(password);
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
