package violet.beans;

import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;

import violet.jpa.Game;

@ManagedBean(name="gameListBean")
@SessionScoped
public class GameListBean {
	@ManagedProperty(value="#{jpaBean}")
	private JPABean jpaBean;

	@ManagedProperty(value="#{userBean}")
	private UserBean userBean;
	
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
	
	public List<Game> getGames() {
		return getLimitedGames(24);
	}
	
	public List<Game> getLimitedGames(int length) {
		return getJpaBean().getGames(length);
	}
}
