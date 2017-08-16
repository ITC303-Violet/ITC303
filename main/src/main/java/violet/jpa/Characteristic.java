package violet.jpa;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.persistence.*;

/**
 * Used to store characteristics of a genre to be rated on a game
 * @author somer
 */
@Entity
public class Characteristic {
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;
	
	private String name;
	
	@ManyToMany
	private List<Genre> genres;
	
	@OneToMany(mappedBy="characteristic", cascade=CascadeType.PERSIST)
	private List<Rating> ratings;
	
	@ManyToMany
	private List<User> users;
	
	
	public Characteristic() {
		genres = new ArrayList<Genre>();
		ratings = new ArrayList<Rating>();
		users = new ArrayList<User>();
	}
	
	public Characteristic(String name) {
		this();
		this.name = name;
	}
	
	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}
	
	public void addGenre(Genre genre) {
		if(genres.contains(genre))
			return;
		
		genres.add(genre);
		genre.addCharacteristic(this);
	}
	
	public void removeGenre(Genre genre) {
		if(!genres.contains(genre))
			return;
		
		genres.remove(genre);
		genre.removeCharacteristic(this);
	}
	
	public List<Genre> getGenres() {
		return genres;
	}
	
	public void addRating(Rating rating) {
		if(ratings.contains(rating))
			return;
		
		ratings.add(rating);
		
		Characteristic current = rating.getCharacteristic();
		if(current != null)
			current.getRatings().remove(rating);
		
		rating.setCharacteristic(this);
	}
	
	public List<Rating> getRatings() {
		return ratings;
	}
	
	public void addUser(User user) {
		if(users.contains(user))
			return;
		
		users.add(user);
		user.addFavouredCharacteristic(this);
	}
	
	public void removeUser(User user) {
		if(!users.contains(user))
			return;
		
		users.remove(user);
		user.removeFavouredCharacteristic(this);
	}
	
	public List<User> getUsers() {
		return users;
	}
	
	
	/**
	 * @param em
	 * @return A collection of all Genres saved in the database
	 */
	public static Collection<Characteristic> getCharacteristics(EntityManager em, boolean orphaned) {
		try {
			String query = "SELECT c FROM Characteristic c";
			if(!orphaned)
				query += " WHERE c.genres IS NOT EMPTY";
			query += " ORDER BY c.name";
			return em.createQuery(query, Characteristic.class).getResultList();
		} catch(NoResultException e) {
			return Collections.emptyList();
		}
	}
		
	/**
	 * @param name
	 * @param create
	 * @param em
	 * @return A Characteristic object with the given name. If create is true and the characteristic doesn't already exist, a new Characteristic object will be created and returned.
	 */
	public static Characteristic getCharacteristic(String name, boolean create, EntityManager em) {
		try {
			TypedQuery<Characteristic> tq = em.createQuery("SELECT c FROM Characteristic c WHERE LOWER(c.name)=:name", Characteristic.class);
			return tq.setParameter("name", name.toLowerCase()).getSingleResult();
		} catch(NoResultException e) {
			if(create)
				return new Characteristic(name);
			return null;
		}
	}
	
	public static Long count() {
		EntityManager em = FactoryManager.getCommonEM();
		try {
			return em.createQuery("SELECT COUNT(c) FROM Characteristic c", Long.class)
					.getSingleResult();
		} catch(NoResultException e) {
			return 0L;
		}
	}
}
