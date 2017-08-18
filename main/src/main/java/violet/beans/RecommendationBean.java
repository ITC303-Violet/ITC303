package violet.beans;

import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import violet.jpa.FactoryManager;
import violet.jpa.Game;
import violet.jpa.Paginator;

/**
 * Provides the interface to the recommendation engine
 * @author somer
 */
@ManagedBean
@RequestScoped
public class RecommendationBean {
	@ManagedProperty(value="#{userBean}")
	private UserBean userBean;
	
	@ManagedProperty(value="#{gameBean}")
	private GameBean gameBean;
	
	public GameBean getGameBean() {
		return gameBean;
	}

	public void setGameBean(GameBean gameBean) {
		this.gameBean = gameBean;
	}

	public UserBean getUserBean() {
		return userBean;
	}

	public void setUserBean(UserBean userBean) {
		this.userBean = userBean;
	}
	
	public String gotoRecommendation() {
		return "/game.xhtml?faces-redirect=true&gameid=9249";
	}
}
