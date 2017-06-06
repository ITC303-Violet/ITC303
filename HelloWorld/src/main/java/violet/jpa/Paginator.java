package violet.jpa;

import java.util.List;

public class Paginator<T> {
	private int page;
	private int pageSize;
	private int pages;
	private List<T> items;
	
	public Paginator(int page, int pageSize, int pages, List<T> items) {
		this.page = page;
		this.pageSize = pageSize;
		this.pages = pages;
		this.items = items;
	}
	
	public int getPage() {
		return page;
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
