package main.java.jpa;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.*;

@Entity
public class Genre {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	private String name;
	
	private final List<Game> games = new ArrayList<Game>();
	private final List<Characteristic> characteristics = new ArrayList<Characteristic>();
	
	public Genre() {
		
	}
	
	public String getName() {
		return name;
	}
	
	public boolean addGame(Game game) {
		return games.add(game);
	}
	
	public List<Game> getGames() {
		return games;
	}
	
	public boolean addCharacteristic(Characteristic characteristic) {
		return characteristics.add(characteristic);
	}
	
	public List<Characteristic> getCharacteristics() {
		return characteristics;
	}
}
