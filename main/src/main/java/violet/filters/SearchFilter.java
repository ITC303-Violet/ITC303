package violet.filters;



public class SearchFilter {
	String start;
	String filter;
	String query;
	String search;
	String queryOrder;
	String queryOn;
	String sortBy;
	boolean releaseOnly;
	
	
	public SearchFilter(boolean r, String s){
		this.releaseOnly = r;
		this.search = s;
	}
	
	
	public String queryS (){
		
		start = "SELECT g FROM Game g";
		return start;
		}
	
	//for rating
	public String queryON(){
		//queryOn = " LEFT JOIN Rating r ON r.game=g AND r.user IS NULL AND r.characteristic IS NULL";
		queryOn = "";
		return queryOn;
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
	
	public String queryO (String sortBy){
		this.sortBy = sortBy;
		
		if (sortBy == "1"){
			queryOrder = " ORDER BY g.release DESC NULLS LAST, g.id ASC";
		}
		else if (sortBy == "2"){
			queryOrder = " ORDER BY g.name ASC NULLS LAST, g.id ASC";	
		}
		else if (sortBy == "3"){
				queryOrder = " ORDER BY g.release DESC NULLS LAST, g.id ASC";	
			}
		else {
			queryOrder = " ORDER BY g.name ASC NULLS LAST, g.id ASC";
		}
		System.out.println("|||||||||||||||||||||||||||||||||||||||||||||||||||||||||||");
		System.out.println(sortBy);
		System.out.println("||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||");
		//queryOrder = " ORDER BY g.release DESC NULLS LAST, g.id ASC";
		//queryOrder = " ORDER BY g.name ASC NULLS LAST, g.id ASC";
		//queryOrder = " ORDER BY g.name DESC NULLS LAST, g.id ASC";
		//queryOrder = " ORDER BY g.genre ASC NULLS LAST, g.id ASC";
		//queryOrder = " ORDER BY r.rating DESC NULLS LAST, g.id ASC";
		
		//LEFT JOIN Rating r ON r.game=g AND r.user IS NULL AND r.characteristic IS NULL ORDER BY r.rating
		return queryOrder;
	}
	
	
}
