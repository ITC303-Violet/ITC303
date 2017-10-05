package violet.filters;


public class SearchFilter{
	private boolean releaseOnly;
	private String searchQuery;
	private String sortBy;
	private boolean genreFiltered;
	
	private String start;
	private String where;
	private String order;
	
	public SearchFilter(boolean releasedOnly, String searchQuery, String sortBy, boolean genreFiltered) {
		this.releaseOnly = releasedOnly;
		this.searchQuery = searchQuery;
		this.sortBy = sortBy;
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
