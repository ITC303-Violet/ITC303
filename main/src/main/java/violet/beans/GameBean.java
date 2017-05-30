package violet.beans;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;

import violet.jpa.Game;

@ManagedBean
@RequestScoped
public class GameBean {
	@ManagedProperty(value = "#{jpaBean}")
	private JPABean jpaBean;
	
	private Long id;
	private Game game;
	
	public JPABean getJpaBean() {
		return jpaBean;
	}

	public void setJpaBean(JPABean jpaBean) {
		this.jpaBean = jpaBean;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
		game = getJpaBean().getGame(id);
	}
	
	public Game getGame() {
		return game;
	}
	
	public void setGame(Game game) {
		this.game = game;
	}
}
