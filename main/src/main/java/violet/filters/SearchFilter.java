package violet.filters;


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


public class SearchFilter{
	private String start;
	private String filter;
	private String query;
	private String search;
	private String queryOrder;
	private String queryOn;
	private String sortBy;
	boolean releaseOnly;
	private String[] genreFilter;
	
	
	
	public SearchFilter(boolean r){
		this.releaseOnly = r;
	}
	
	
	public String queryS (String s, String on){
		this.search = s;
		start = "SELECT DISTINCT g FROM Game g";
		if (on.equals("rating")){
			start += " LEFT JOIN Rating r ON r.game=g AND r.user IS NULL AND r.characteristic IS NULL";
		}
		return start;
	}
	
	
	public String queryF (boolean t){
		if(t == true){
			filter += " INNER JOIN Genre gg WHERE gg.action IN :genres AND g.blacklisted=FALSE";
		}
		else{
			filter = " WHERE g.blacklisted=FALSE";
		}
		if(this.releaseOnly)
			filter += " AND g.release < CURRENT_TIMESTAMP";
		if(!search.isEmpty())
			filter += " AND LOWER(g.name) LIKE :searchQuery";
		return filter;
	}
	
	public String queryO (String sortBy){
		this.sortBy = sortBy;
		System.out.println("sort = " + sortBy);
		if (sortBy.equals("release")){
			queryOrder = " ORDER BY g.release DESC NULLS LAST, g.id ASC";
		}
		else if (sortBy.equals("rating")){
			queryOrder = " ORDER BY r.rating DESC NULLS LAST, g.id ASC";	
		}
		else if (sortBy.equals("az")){
				queryOrder = " ORDER BY g.name ASC NULLS LAST, g.id ASC";	
			}
		else if (sortBy.equals("za")){
			queryOrder = " ORDER BY g.name DESC NULLS LAST, g.id ASC";	
		}
		else{
			queryOrder = " ORDER BY g.name ASC NULLS LAST, g.id ASC";
		}
		
		return queryOrder;
	}
	
	
}
