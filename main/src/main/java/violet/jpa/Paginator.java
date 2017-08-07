package violet.jpa;

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
	
	private int PAGES_TO_SIDE = 4;
	
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
		return getPage() < (PAGES_TO_SIDE + 1);
	}
	
	public boolean isNearEnd() {
		return getPage() > (getPages() - PAGES_TO_SIDE - 1);
	}
	
	public int getStartPage() {
		return Math.max(getPage() - PAGES_TO_SIDE, 1);
	}
	
	public int getEndPage() {
		return Math.min(getPage() + PAGES_TO_SIDE, getPages());
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
