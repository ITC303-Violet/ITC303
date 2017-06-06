package violet.beans;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;

import violet.jpa.Characteristic;
import violet.jpa.Game;
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
	
	private Integer overallRating = 0;
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
		
		characteristicRatings = new HashMap<Characteristic, Integer>();
		for(Characteristic characteristic : game.getCharacteristics()) {
			characteristicRatings.put(characteristic, 0);
		}
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
		if(!userBean.isAuthenticated()) {
			Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Not authenticated");
			return null;
		}
			
		
		User user = userBean.getUser(); 
		user.rateGame(game, null, overallRating.doubleValue());
		
		for(Map.Entry<Characteristic, Integer> rating : characteristicRatings.entrySet()) {
			user.rateGame(game, rating.getKey(), rating.getValue().doubleValue());
		}
		
		Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Rated");
		
		return null;
	}
}
