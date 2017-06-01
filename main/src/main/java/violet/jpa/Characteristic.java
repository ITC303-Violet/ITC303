package violet.jpa;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.*;

@Entity
public class Characteristic {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	private String name;
	
	@ManyToMany
	private List<Genre> genres;
	
	@OneToMany(mappedBy="characteristic")
	private List<Rating> ratings;
	
	public Characteristic() {
		genres = new ArrayList<Genre>();
		ratings = new ArrayList<Rating>();
	}

	public String getName() {
		return name;
	}
	
	public void addGenres(Genre genre) {
		if(genres.contains(genre))
			return;
		
		genres.add(genre);
		genre.addCharacteristic(this);
	}
	
	public boolean addGenre(Genre genre) {
		return genres.add(genre);
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
