package violet.jpa;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import javax.persistence.*;

@Entity
public class Game {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private String name;
	
	@Lob
	private String description;
	
	@Lob
	private String short_description;
	
	@ManyToMany(mappedBy="games")
	private List<Genre> genres;
	
	@OneToMany(mappedBy="game")
	private List<Rating> ratings;
	
	@OneToMany(mappedBy="game")
	private List<Screenshot> screenshots;
	
	@Embedded
	private Image heroImage;
	
	@Column(unique=true)
	private int steam_id;
	
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
	
	public Long getId() {
		return id;
	}
	
	private static final Pattern[] patterns = {
		Pattern.compile("(?:^\\W+|\\W+$)"),
		Pattern.compile("\\W+")
	};
	
	private static final String[] replacements = {
		"",
		"_"
	};
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
	
	public List<Genre> getGenres() {
		return genres;
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
	
	public boolean hasScreenshot(String remoteId) {
		EntityManager em = FactoryManager.get().createEntityManager();
		try {
			TypedQuery<Long> tq = em.createQuery("SELECT COUNT(s) FROM Game g INNER JOIN g.screenshots s WHERE g.id=:id AND s.remoteIdentifier=:rid", Long.class);
			Long result = tq.setParameter("id", id)
					.setParameter("rid", remoteId)
					.getSingleResult();
			return result > 0;
		} catch(NoResultException e) {
			return false;
		} finally {
			em.close();
		} 
	}
}
