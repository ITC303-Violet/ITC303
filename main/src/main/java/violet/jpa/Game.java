package violet.jpa;

import java.util.ArrayList;
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
	private final List<Genre> genres = new ArrayList<Genre>();
	
	@OneToMany
	private final List<Rating> ratings = new ArrayList<Rating>();
	
	@Embedded
	private Image heroImage;
	
	private int steam_id;
	
	public Game() {
		
	}
	
	public Game(String name) {
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
	
	public boolean addGenre(Genre genre) {
		return genres.add(genre);
	}
	
	public List<Genre> getGenres() {
		return genres;
	}
	
	public boolean addRating(Rating rating) {
		return ratings.add(rating);
	}
	
	public List<Rating> getRatings() {
		return ratings;
	}
}
