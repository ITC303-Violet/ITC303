package jpa;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.*;

@Entity
public class Characteristic {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	private String name;
	
	@ManyToOne(optional=false)
	private Genre genre;
	
	@OneToMany
	private final List<Rating> ratings = new ArrayList<Rating>();
	
	public Characteristic() {
		
	}

	public String getName() {
		return name;
	}

	public Genre getGenre() {
		return genre;
	}

	public void setGenre(Genre genre) {
		this.genre = genre;
	}
	
	public boolean addRating(Rating rating) {
		return ratings.add(rating);
	}
	
	public List<Rating> getRatings() {
		return ratings;
	}
}
