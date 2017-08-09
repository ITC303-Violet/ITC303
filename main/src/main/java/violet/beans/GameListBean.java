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
 * Provides the list of games for the front page and browse page
 * @author somer
 */
@ManagedBean(name="gameListBean")
@RequestScoped
public class GameListBean {
	private static final int FRONT_PAGE_SIZE = 24;
	private static final int PAGE_SIZE = 15;

	@ManagedProperty(value="#{userBean}")
	private UserBean userBean;
	
	private int page = 1;
	
	//@ManagedProperty(value="#{param.q}")
	private String searchQuery = "";
	
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
	
	public String getSearchQuery() {
		return searchQuery;
	}
	
	public void setSearchQuery(String searchQuery) {
		this.searchQuery = searchQuery;
	}
	
	public String search() {
		setPage(1);
		
		return "pretty:browse-games";
	}
	
	/**
	 * Returns a {@link Paginator} object containing the list of games
	 * @param page the page number
	 * @param length length of a page (i.e. 25 games)
	 * @param releasedOnly if true, only returns games that have a release date before the current date
	 * @return a {@link Paginator} object containing the list of games
	 */
	public Paginator<Game> getPaginatedGames(int page, int length, boolean releasedOnly, String search) {
		EntityManager em = FactoryManager.pullCommonEM();
		try {
			search = search != null ? search.toLowerCase() : "";
			search = search.isEmpty() ? "" : "%" + search.replace("%", "\\%").replace("_", "\\_") + "%";
			
			String queryStart = "SELECT g FROM Game g ";
			String queryFilter = " WHERE g.blacklisted=FALSE";
			if(releasedOnly)
				queryFilter += " AND g.release < CURRENT_TIMESTAMP";
			if(!search.isEmpty())
				queryFilter += " AND LOWER(g.name) LIKE :searchQuery";
			
			TypedQuery<Game> tq = em.createQuery(queryStart + queryFilter + " ORDER BY g.release DESC NULLS LAST, g.id ASC", Game.class);
			if(!search.isEmpty())
				tq.setParameter("searchQuery", search);
			
			List<Game> list = tq
					.setFirstResult((page-1) * length)
					.setMaxResults(length)
					.getResultList();
			
			TypedQuery<Long> ctq = em.createQuery("SELECT COUNT(g) FROM Game g" + queryFilter, Long.class);
			if(!search.isEmpty())
				ctq.setParameter("searchQuery", search);
			Long count = ctq.getSingleResult();
			
			Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Count " + count);
			
			return new Paginator<Game>(page, length, (int)(count/length+1), list); // We need to grab it first, or the finally below will close the em before we get the row
		} catch(NoResultException e) {
			List<Game> list = Collections.<Game>emptyList();
			return new Paginator<Game>(page, length, 0, list);
		} finally {
			FactoryManager.popCommonEM();
		}
	}
	
	public List<Game> getFrontPageGames() {
		return getPaginatedGames(1, FRONT_PAGE_SIZE, true, "").getItems(); 
	}
	
	public Paginator<Game> getPaginatedGames() {
		return getPaginatedGames(page, PAGE_SIZE, false, searchQuery);
	}
}
