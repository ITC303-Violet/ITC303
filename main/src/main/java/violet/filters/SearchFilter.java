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

import violet.jpa.FactoryManager;
import violet.jpa.Game;
import violet.jpa.Paginator;



public class SearchFilter {
	String start;
	String filter;
	String query;
	String search;
	String queryOrder;
	boolean releaseOnly;
	
	public SearchFilter(boolean r, String s){
		this.releaseOnly = r;
		this.search = s;
	}
	
	
	public String queryS (){
		start = "SELECT g FROM Game g LEFT JOIN Rating r ON r.game=g AND r.user IS NULL AND r.characteristic IS NULL";
		return start;
		}
	
	public String queryF (){
		filter = " WHERE g.blacklisted=FALSE";
		//filter += " AND g.genre=Action";
		if(this.releaseOnly)
			filter += " AND g.release < CURRENT_TIMESTAMP";
		if(!search.isEmpty())
			filter += " AND LOWER(g.name) LIKE :searchQuery";
		return filter;
	}
	
	public String queryO (){
		//queryOrder = " ORDER BY g.release DESC NULLS LAST, g.id ASC";
		//queryOrder = " ORDER BY g.name ASC NULLS LAST, g.id ASC";
		//queryOrder = " ORDER BY g.name DESC NULLS LAST, g.id ASC";
		//queryOrder = " ORDER BY g.genre ASC NULLS LAST, g.id ASC";
		queryOrder = " ORDER BY r.rating DESC NULLS LAST, g.id ASC";
		
		//LEFT JOIN Rating r ON r.game=g AND r.user IS NULL AND r.characteristic IS NULL ORDER BY r.rating
		return queryOrder;
	}
	
	
}
