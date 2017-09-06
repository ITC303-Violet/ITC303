package violet.beans;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import violet.jpa.Genre;
import violet.jpa.Paginator;
import violet.filters.SearchFilter;

/**
 * Provides the list of games for the front page and browse page
 * @author somer
 */
@ManagedBean(name="gameListBean")
@RequestScoped
public class GameListBean {
	private static final int FRONT_PAGE_SIZE = 24;
	private static final int PAGE_SIZE = 15;
	private SearchFilter filter;
	
	private static Map<String, String> sortOptions;

	@ManagedProperty(value="#{userBean}")
	private UserBean userBean;
	
	private int page = 1;
	
	private String searchQuery = "";
	private String sortQuery = "release";
	private String[] genreFilter;
	
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
	
	public String getSortQuery() {
		return sortQuery;
	}
	
	public void setSortQuery(String sortQuery) {
		if(getSortOptions().containsValue(sortQuery))
			this.sortQuery = sortQuery;
		else
			this.sortQuery = "release";
	}
	
	public Map<String, String> getSortOptions() {
		if(sortOptions == null) {
			sortOptions = new HashMap<>();
			sortOptions.put("Release Date", "release");
			sortOptions.put("Average Rating", "rating");
		}
		
		return sortOptions;
	}
	
	public String[] getGenreFilter() {
		if(genreFilter == null)
			return new String[0];
		
		return genreFilter;
	}
	
	public void setGenreFilter(String[] genreFilter) {
		this.genreFilter = genreFilter;
	}
	
	public List<Genre> getGenreChoices() {
		FactoryManager.pullCommonEM();
		try {
			return new ArrayList<Genre>(Genre.getGenres(FactoryManager.getCommonEM(), false));
		} finally {
			FactoryManager.popCommonEM();
		}
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
	 * @param search filters for a substring in a game's name
	 * @return a {@link Paginator} object containing the list of games
	 */
	public Paginator<Game> getPaginatedGames(int page, Integer length, boolean releasedOnly, String search) {
		EntityManager em = FactoryManager.pullCommonEM();
		try {
			search = search != null ? search.toLowerCase() : "";
			search = search.isEmpty() ? "" : "%" + search.replace("%", "\\%").replace("_", "\\_") + "%";
			
			
			
			//filter
			//browse.getSortValue();
			filter = new SearchFilter(releasedOnly, search);
			String queryStart = filter.queryS();
			String queryFilter = filter.queryF();
			String queryOrder = filter.queryO(sortQuery);
			
			TypedQuery<Game> tq = em.createQuery(queryStart + queryFilter + queryOrder, Game.class);
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
			
			Long pages = count/Long.valueOf(length.longValue());
			
			return new Paginator<Game>(page, length, pages.intValue() + 1, list); // We need to grab it first, or the finally below will close the em before we get the row
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
