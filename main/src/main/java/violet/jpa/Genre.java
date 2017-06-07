package violet.jpa;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.persistence.*;

@Entity
public class Genre {
	@Id
	private String identifier;
	
	private String name;
	
	@ManyToMany
	private List<Game> games;
	
	@ManyToMany(mappedBy="genres")
	private List<Characteristic> characteristics;
	
	public Genre() {
		games = new ArrayList<Game>();
		characteristics = new ArrayList<Characteristic>();
	}
	
	public Genre(String name) {
		this();
		this.identifier = name.toLowerCase();;
		this.name = name;
	}
	
	public String getIndentifier() {
		return identifier;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
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
	
	public static Genre getGenre(String name, boolean create, EntityManager em) {
		try {
			TypedQuery<Genre> tq = em.createQuery("SELECT g FROM Genre g WHERE LOWER(g.identifier)=:identifier", Genre.class);
			return tq.setParameter("identifier", name.toLowerCase()).getSingleResult();
		} catch(NoResultException e) {
			if(create)
				return new Genre(name);
			return null;
		}
	}
	
	public static Collection<Genre> getGenres(EntityManager em) {
		try {
			TypedQuery<Genre> tq = em.createQuery("SELECT g FROM Genre g", Genre.class);
			return tq.getResultList();
		} catch(NoResultException e) {
			return Collections.emptyList();
		}
	}
}
