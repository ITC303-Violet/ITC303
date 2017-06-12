package violet.jpa;

import java.util.ArrayList;
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
	
	public Characteristic() {
		genres = new ArrayList<Genre>();
		ratings = new ArrayList<Rating>();
	}
	
	public Characteristic(String name) {
		this();
		this.name = name;
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
}
