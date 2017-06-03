package violet.beans;

import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;

import violet.jpa.Game;
import violet.jpa.Paginator;

@ManagedBean(name="gameListBean")
@RequestScoped
public class GameListBean {
	private static final int FRONT_PAGE_SIZE = 24;
	private static final int PAGE_SIZE = 20;
	
	@ManagedProperty(value="#{jpaBean}")
	private JPABean jpaBean;

	@ManagedProperty(value="#{userBean}")
	private UserBean userBean;
	
	private int page = 1;
	
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
	
	public int getPage() {
		return page;
	}
	
	public void setPage(int page) {
		this.page = page;
	}
	
	public List<Game> getFrontPageGames() {
		return getJpaBean().getPaginatedGames(1, FRONT_PAGE_SIZE, true).getItems(); 
	}
	
	public Paginator<Game> getPaginatedGames() {
		return getJpaBean().getPaginatedGames(page, PAGE_SIZE, false);
	}
}
