package violet.filters;


public class SearchFilter{
	private boolean releaseOnly;
	private String searchQuery;
	private String sortBy;
	private String[] platforms;
	private boolean genreFiltered;
	
	private String start;
	private String where;
	private String order;
	
	public SearchFilter(boolean releasedOnly, String searchQuery, String sortBy, String[] platforms, boolean genreFiltered) {
		this.releaseOnly = releasedOnly;
		this.searchQuery = searchQuery;
		this.sortBy = sortBy;
		this.platforms = platforms;
		this.genreFiltered = genreFiltered;
	}
	
	public String queryStart() {
		start = "SELECT DISTINCT g FROM Game g";
		
		if(sortBy.equals("rating"))
			start += " LEFT JOIN Rating r ON r.game=g AND r.user IS NULL AND r.characteristic IS NULL";
		
		return start;
	}
	
	public String queryWhere() {
		if(genreFiltered)
			where = " INNER JOIN g.genres gg WHERE gg.identifier IN :genres AND g.blacklisted=FALSE";
		else
			where = " WHERE g.blacklisted=FALSE";
		
		if(this.releaseOnly)
			where += " AND g.release < CURRENT_TIMESTAMP";
		else if(sortBy.equals("upcoming"))
			where += " AND g.release > CURRENT_TIMESTAMP";
		
		if(!searchQuery.isEmpty())
			where += " AND LOWER(g.name) LIKE :searchQuery";
		
		if(platforms.length > 0) {
			where += " AND (";
			
			for(int i=0; i<platforms.length; i++) {
				String platform = platforms[i];
				if(platform.equals("steam"))
					where += "g.steam_id IS NOT NULL";
				else if(platform.equals("playstation"))
					where += "g.ps_store_id IS NOT NULL";
				else if(platform.equals("xbox"))
					where += "g.xbox_store_id IS NOT NULL";
				else if(platform.equals("nintendo"))
					where += "g.nintendo_id IS NOT NULL";
				
				if(i!=platforms.length-1)
					where += " OR "; 
			}
			
			where += ")";
		}
		
		return where;
	}
	
	public String queryOrder() {
		switch(sortBy) {
			case "released":
				order = " ORDER BY g.release DESC NULLS LAST, g.id ASC";
				break;
			case "upcoming":
				order = " ORDER BY g.release ASC NULLS LAST, g.name ASC";
				break;
			case "rating":
				order = " ORDER BY r.rating DESC NULLS LAST, g.release DESC NULLS LAST, g.id ASC";
				break;
			case "za":
				order = " ORDER BY g.name DESC NULLS LAST, g.id ASC";	
				break;
			case "az":
			default:
				order = " ORDER BY g.name ASC NULLS LAST, g.id ASC";
		}
		
		return order;
	}
}
