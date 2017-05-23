package main.java.beans;

import java.util.List;

import javax.faces.bean.ManagedProperty;

import main.java.jpa.Game;

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
		return getGames(25);
	}
	
	public List<Game> getGames(int length) {
		return getJpaBean().getGames(length);
	}
}
