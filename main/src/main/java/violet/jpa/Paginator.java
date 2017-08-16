package violet.jpa;

import java.util.ArrayList;
import java.util.List;

/**
 * Keeps track of what page we're on, the page size, etc of a paged dataset
 * @author somer
 *
 * @param <T> The class we're paginating
 */
public class Paginator<T> {
	private int page;
	private int pageSize;
	private int pages;
	private List<T> items;
	
	private static final int PAGES_TO_SIDE = 4;
	
	public Paginator(int page, int pageSize, int pages, List<T> items) {
		this.page = page;
		this.pageSize = pageSize;
		this.pages = pages;
		this.items = items;
	}
	
	public int getPage() {
		return page;
	}
	
	public boolean isNearStart() {
		return page < PAGES_TO_SIDE + 2;
	}
	
	public boolean isNearEnd() {
		return page > pages - PAGES_TO_SIDE - 1;
	}
	
	public int getLeftPage() {
		return Math.max(page - PAGES_TO_SIDE, 1);
	}
	
	public int getRightPage() {
		return Math.min(page + PAGES_TO_SIDE, pages);
	}
	
	public List<Integer> getNearbyPages() {
		List<Integer> out = new ArrayList<>();
		for(int i=getLeftPage(); i<=getRightPage(); i++)
			out.add(i);
		
		return out;
	}
	
	public int getPageSize() {
		return pageSize;
	}
	
	public int getPages() {
		return pages;
	}
	
	public List<T> getItems() {
		return items;
	}
}
