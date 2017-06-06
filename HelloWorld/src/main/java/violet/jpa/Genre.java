package violet.jpa;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.*;

@Entity
public class Genre {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	private String name;
	
	@ManyToMany
	private List<Game> games;
	
	@ManyToMany(mappedBy="genres")
	private List<Characteristic> characteristics;
	
	public Genre() {
		games = new ArrayList<Game>();
		characteristics = new ArrayList<Characteristic>();
	}
	
	public String getName() {
		return name;
	}
	
	public void addGame(Game game) {
		if(games.contains(game))
			return;
		
		games.add(game);
		game.addGenre(this);
	}
	
	public List<Game> getGames() {
		return games;
	}
	
	public void addCharacteristic(Characteristic characteristic) {
		if(characteristics.contains(characteristic))
			return;
		
		characteristics.add(characteristic);
		characteristic.addGenre(this);
	}
	
	public List<Characteristic> getCharacteristics() {
		return characteristics;
	}
}
