package violet.jpa;

import java.util.ArrayList;
import java.util.List;

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
	
	private int steam_id;
	
	public Game() {
		
	}
	
	public Game(String name) {
		this.name = name;
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
