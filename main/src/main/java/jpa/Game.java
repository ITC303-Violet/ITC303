package main.java.jpa;
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
	
	@ManyToMany(mappedBy="games")
	private final List<Genre> genres = new ArrayList<Genre>();
	
	@OneToMany
	private final List<Rating> ratings = new ArrayList<Rating>();
	
	public Game() {
		
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
