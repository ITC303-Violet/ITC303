package violet.jpa;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.persistence.*;

/**
 * Stores game data
 * @author somer
 */
@Entity
@NamedQueries({
	@NamedQuery(name="Game.findAverageRating", query="SELECT r FROM Rating r WHERE r.game=:game AND r.user IS NULL AND r.characteristic IS NULL"),
	@NamedQuery(name="Game.findAverageCharacteristicRating", query="SELECT r FROM Rating r WHERE r.game=:game AND r.characteristic=:characteristic AND r.user IS NULL"),
	@NamedQuery(name="Game.count", query="SELECT COUNT(g) FROM Game g")
})
public class Game {
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;
	private String name;
	private boolean blacklisted = false;
	
	@Lob
	private String description;
	
	@Lob
	private String short_description;
	
	@ManyToMany(mappedBy="games", cascade=CascadeType.PERSIST)
	private List<Genre> genres;
	
	@OneToMany(mappedBy="game", cascade=CascadeType.PERSIST)
	private List<Rating> ratings;
	
	@OneToMany(mappedBy="game", cascade=CascadeType.PERSIST)
	private List<Screenshot> screenshots;
	
	@Embedded
	private Image heroImage;
	
	@Column(unique=true)
	private int steam_id;
	
	@Column(unique=true)
	private String ps_store_id;
	
	@Column(unique=true)
	private String xbox_store_id;
	
	
	@Temporal(TemporalType.TIMESTAMP)
	private Date release;
	
	public Game() {
		genres = new ArrayList<Genre>();
		ratings = new ArrayList<Rating>();
		screenshots = new ArrayList<Screenshot>();
	}
	
	public Game(String name) {
		this();
		this.name = name;
	}
	
	/**
	 * @return A map of the average ratings given to a characteristic of the game
	 */
	public Map<Characteristic, Rating> getAverageCharacteristicRatings() {
		HashMap<Characteristic, Rating> ratings = new HashMap<>();
		
		EntityManager em = FactoryManager.pullCommonEM();
		for(Genre genre : genres)
			for(Characteristic characteristic : genre.getCharacteristics())
				if(!ratings.containsKey(characteristic))
					ratings.put(characteristic, getAverageCharacteristicRating(characteristic, em));
		FactoryManager.popCommonEM();
		
		return ratings;
	}
	
	/**
	 * @return the average overall rating of the game
	 */
	public Rating getAverageRating() {
		EntityManager em = FactoryManager.pullCommonEM();
		Rating rating = getAverageRating(em);
		FactoryManager.popCommonEM();
		
		return rating;
	}
	
	/**
	 * @return the average overall rating of the game
	 */
	public Rating getAverageRating(EntityManager em) {
		return getAverageCharacteristicRating(null, em);
	}
	
	/**
	 * @param characteristic (can be null for the overall rating)
	 * @param em
	 * @return the average rating of the given characteristic of the game
	 */
	public Rating getAverageCharacteristicRating(Characteristic characteristic, EntityManager em) {
		TypedQuery<Rating> tq;
		if(characteristic == null)
			tq = em.createNamedQuery("Game.findAverageRating", Rating.class);
		else
			tq = em.createNamedQuery("Game.findAverageCharacteristicRating", Rating.class)
				.setParameter("characteristic", characteristic);
		try {
			tq.setFlushMode(FlushModeType.COMMIT);
			Rating rating = tq.setParameter("game", this)
					.getSingleResult();
			return rating;
		} catch(NoResultException e) {
			return null;
		}
	}
	
	public Long getId() {
		return id;
	}
	
	private static final Pattern[] patterns = {
		Pattern.compile("(?:^\\W+|\\W+$)"), // remove all leading and trailing not letters & numbers
		Pattern.compile("\\W+") // replace all other non letters & numbers with an underscore (_)
	};
	
	private static final String[] replacements = {
		"",
		"_"
	};
	
	/**
	 * @return returns a URL friendly name
	 */
	public String getCleanedName() {
		String out = name;
		for(int i=0; i<patterns.length; i++)
			out = patterns[i].matcher(out).replaceAll(replacements[i]);
		return out;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public boolean getBlacklisted() {
		return blacklisted;
	}
	
	public void setBlacklisted(boolean blacklisted) {
		this.blacklisted = blacklisted;
	}
	
	public String getDescription() {
		return description;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}
	
	public String getShortDescription() {
		return description;
	}
	
	public void setShortDescription(String short_description) {
		this.short_description = short_description;
	}
	
	public Image getHeroImage() {
		return heroImage;
	}
	
	public void setHeroImage(Image heroImage) {
		this.heroImage = heroImage;
	}
	public String getPSStoreId() {
		return ps_store_id;
	}
	
	public void setPSStoreId(String ps_store_id) {
		this.ps_store_id = ps_store_id;
	}
	public String getXBoxStoreId() {
		return xbox_store_id;
	}
	
	public void setXBoxStoreId(String xbox_store_id) {
		this.xbox_store_id = xbox_store_id;
	}
	
	
	public int getSteamId() {
		return steam_id;
	}
	
	public void setSteamId(int steam_id) {
		this.steam_id = steam_id;
	}
	
	public void addGenre(Genre genre) {
		if(genres.contains(genre))
			return;
		
		genres.add(genre);
		genre.addGame(this);
	}
	
	public void removeGenre(Genre genre) {
		if(!genres.contains(genre))
			return;
		
		genres.remove(genre);
		genre.removeGame(this);
	}
	
	public List<Genre> getGenres() {
		return genres;
	}
	
	public List<Genre> getDisplayGenres() {
		List<Genre> displayGenres = new ArrayList<Genre>();
		for(Genre genre : genres)
			if(!genre.getHidden())
				displayGenres.add(genre);
		
		return displayGenres;
	}
	
	public boolean hasGenre(Genre genre) {
		return genres.contains(genre);
	}
	
	public List<Characteristic> getCharacteristics() {
		List<Characteristic> characteristics = new ArrayList<Characteristic>();
		for(Genre genre : genres)
			for(Characteristic characteristic : genre.getCharacteristics())
				if(!characteristics.contains(characteristic))
					characteristics.add(characteristic);
		
		return characteristics;
	}
	
	public void addRating(Rating rating) {
		if(ratings.contains(rating))
			return;
		
		ratings.add(rating);
		
		Game current = rating.getGame();
		if(current != null)
			current.getRatings().remove(rating);
		
		rating.setGame(this);
	}
	
	public List<Rating> getRatings() {
		return ratings;
	}
	
	public void setRelease(Date release) {
		this.release = release;
	}
	
	public Date getRelease() {
		return release;
	}
	
	public boolean getHasRelease() {
		return release != null;
	}
	
	/**
	 * @return true if the game has a release date before the current date
	 */
	public boolean getReleased() {
		if(!getHasRelease())
			return false;
		return new Date().after(release);
	}
	
	public void addScreenshot(Screenshot screenshot) {
		if(screenshots.contains(screenshot))
			return;
		
		screenshots.add(screenshot);
		
		Game current = screenshot.getGame();
		if(current != null)
			current.getScreenshots().remove(screenshot);
		
		screenshot.setGame(this);
	}
	
	public List<Screenshot> getScreenshots() {
		return screenshots;
	}
	
	/**
	 * @param remoteId
	 * @return true if the game has a screenshot with a matching remoteId to the one provided
	 */
	public boolean hasScreenshot(String remoteId) {
		EntityManager em = FactoryManager.getCommonEM();
		try {
			TypedQuery<Long> tq = em.createQuery("SELECT COUNT(s) FROM Game g INNER JOIN g.screenshots s WHERE g.id=:id AND s.remoteIdentifier=:rid", Long.class);
			Long result = tq.setParameter("id", id)
					.setParameter("rid", remoteId)
					.getSingleResult();
			return result > 0;
		} catch(NoResultException e) {
			return false;
		} 
	}
	
	public static Long count() {
		EntityManager em = FactoryManager.getCommonEM();
		try {
			return em.createNamedQuery("Game.count", Long.class)
					.getSingleResult();
		} catch(NoResultException e) {
			return 0L;
		}
	}
}
