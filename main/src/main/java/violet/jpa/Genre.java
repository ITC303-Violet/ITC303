package violet.jpa;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.persistence.*;

/**
 * Stores the genres that games are assigned
 * @author somer
 */
@Entity
public class Genre {
	@Id
	private String identifier;
	
	private String name;
	
	private boolean blacklisted = false;
	
	@ManyToMany
	private List<Game> games;
	
	@ManyToMany(mappedBy="genres", cascade=CascadeType.PERSIST)
	private List<Characteristic> characteristics;
	
	@ManyToMany
	private List<User> users;
	
	public Genre() {
		games = new ArrayList<Game>();
		characteristics = new ArrayList<Characteristic>();
		users = new ArrayList<User>();
	}
	
	public Genre(String name) {
		this();
		this.identifier = name.toLowerCase();;
		this.name = name;
	}
	
	public String getIdentifier() {
		return identifier;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public boolean getBlacklisted() {
		return blacklisted;
	}
	
	private void updateBlacklisted(boolean blacklisted) {
		FactoryManager.pullTransaction();
		try {
			for(Game game : games) {
				if(blacklisted)
					game.setBlacklisted(blacklisted);
				else {
					for(Genre genre : game.getGenres()) // only if no other genres the game belongs to are blacklisted
						if(genre != this && genre.getBlacklisted())
							return;
					
					game.setBlacklisted(blacklisted);
				}
			}
		} finally {
			FactoryManager.popTransaction();
		}
	}
	
	public void setBlacklisted(boolean blacklisted) {
		this.blacklisted = blacklisted;
		updateBlacklisted(blacklisted);
	}
	
	public void addUser(User user) {
		if(users.contains(user))
			return;
		
		users.add(user);
		user.addFavouredGenre(this);
	}
	
	public void removeUser(User user) {
		if(!users.contains(user))
			return;
		
		users.remove(user);
		user.removeFavouredGenre(this);
	}
	
	public List<User> getUsers() {
		return users;
	}
	
	public void addGame(Game game) {
		if(games.contains(game))
			return;
		
		games.add(game);
		game.addGenre(this);
		
		updateBlacklisted(blacklisted);
	}
	
	public void removeGame(Game game) {
		if(!games.contains(game))
			return;
		
		updateBlacklisted(false);
		
		games.remove(game);
		game.removeGenre(this);	
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
	
	/**
	 * @param name
	 * @param create
	 * @param em
	 * @return A Genre object with the given name. If create is true and the genre doesn't already exist, a new Genre object will be created and returned.
	 */
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
		return getGenres(em, false);
	}
	
	/**
	 * @param em
	 * @return A collection of all Genres saved in the database
	 */
	public static Collection<Genre> getGenres(EntityManager em, boolean blacklisted) {
		try {
			String query = "SELECT g FROM Genre g";
			if(!blacklisted)
				query += " WHERE g.blacklisted=FALSE";
			query += " ORDER BY g.name";
			TypedQuery<Genre> tq = em.createQuery(query, Genre.class);
			return tq.getResultList();
		} catch(NoResultException e) {
			return Collections.emptyList();
		}
	}
}
