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

	public void setId(Long id) {
		this.id = id;
		game = getJpaBean().getGame(id);
		
		User user = userBean.getUser();
		
		EntityManager em = FactoryManager.getEM();
		if(user != null) {
			Rating rating = user.getRating(game, null, em);
			if(rating != null)
				overallRating = rating.getRating().intValue();
		} else if(game.getAverageRating(em) != null)
			overallRating = game.getAverageRating(em).getRating().intValue();
			
		characteristicRatings = new HashMap<Characteristic, Integer>();
		for(Characteristic characteristic : game.getCharacteristics()) {
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
	
	public String rateGame() {
		if(!userBean.isAuthenticated())
			return null;
			
		
		User user = userBean.getUser(); 
		user.rateGame(game, null, overallRating.doubleValue());
		
		for(Map.Entry<Characteristic, Integer> rating : characteristicRatings.entrySet()) {
			Object ratingValue = rating.getValue();
			if(ratingValue instanceof String) {
				if(((String)ratingValue).isEmpty())
					continue;
				
				ratingValue = Integer.parseInt((String)ratingValue);
			}
			user.rateGame(game, rating.getKey(), ((Integer)ratingValue).doubleValue());
		}
		
		return null;
	}
}
