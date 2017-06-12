package violet.beans;

import java.util.HashMap;
import java.util.Map;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import javax.persistence.EntityManager;

import violet.jpa.Characteristic;
import violet.jpa.FactoryManager;
import violet.jpa.Game;
import violet.jpa.Rating;
import violet.jpa.User;

/**
 * Handles game rating
 * @author somer
 */
@ManagedBean
@RequestScoped
public class GameBean {
	@ManagedProperty(value = "#{jpaBean}")
	private JPABean jpaBean;
	
	@ManagedProperty(value = "#{userBean}")
	private UserBean userBean;
	
	private Long id;
	private Game game;
	
	private Integer overallRating = null;
	private Map<Characteristic, Integer> characteristicRatings;
	
	public JPABean getJpaBean() {
		return jpaBean;
	}

	public void setJpaBean(JPABean jpaBean) {
		this.jpaBean = jpaBean;
	}
	
	public UserBean getUserBean() {
		return userBean;
	}

	public void setUserBean(UserBean userBean) {
		this.userBean = userBean;
	}

	public Long getId() {
		return id;
	}
	
	/**
	 * Sets the game id, and sets the various others pieces of information
	 * @param id
	 */
	public void setId(Long id) {
		this.id = id;
		game = getJpaBean().getGame(id);
		
		User user = userBean.getUser();
		
		EntityManager em = FactoryManager.getEM();
		if(user != null) { // User is logged in
			Rating rating = user.getRating(game, null, em);
			if(rating != null) // User has rated the game before
				overallRating = rating.getRating().intValue();
		} else if(game.getAverageRating(em) != null) // User is not logged in, check the game has an average rating
			overallRating = game.getAverageRating(em).getRating().intValue(); // Set the rating component value to the average rating
			
		characteristicRatings = new HashMap<Characteristic, Integer>();
		for(Characteristic characteristic : game.getCharacteristics()) { // Do the same as above for all the components associated with the genres the game is associated with
			if(user != null) {
				Rating rating = user.getRating(game, characteristic, em);
				if(rating != null)
					characteristicRatings.put(characteristic, rating.getRating().intValue());
			} else {
				Rating averageRating = game.getAverageCharacteristicRating(characteristic, em);
				characteristicRatings.put(characteristic, averageRating == null ? 0 : averageRating.getRating().intValue());
			}
		}
		em.close();
	}
	
	public Game getGame() {
		return game;
	}
	
	public void setGame(Game game) {
		this.game = game;
	}
	
	public Integer getOverallRating() {
		return overallRating;
	}
	
	public void setOverallRating(Integer overallRating) {
		this.overallRating = overallRating;
	}
	
	public Map<Characteristic, Integer> getCharacteristicRatings() {
		return characteristicRatings;
	}
	
	/**
	 * JSF action called when a user rates a game 
	 * @return
	 */
	public String rateGame() {
		if(!userBean.isAuthenticated()) // Check a user is signed in
			return null;
			
		User user = userBean.getUser(); 
		user.rateGame(game, null, overallRating.doubleValue()); // register the user's rating
		
		for(Map.Entry<Characteristic, Integer> rating : characteristicRatings.entrySet()) {
			Object ratingValue = rating.getValue();
			if(ratingValue instanceof String) { // If the value is a string (primefaces seems to offer the value as a string)
				if(((String)ratingValue).isEmpty()) // If the value is empty - ignore that characteristic
					continue;
				
				ratingValue = Integer.parseInt((String)ratingValue); 
			}
			user.rateGame(game, rating.getKey(), ((Integer)ratingValue).doubleValue()); // save that characteristic rating
		}
		
		return null;
	}
}