package home.abel.photohub.web.model;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

public class ResponseObjectsListPage<T> extends PageImpl<T> {
	private static final long serialVersionUID = 1L;
	
	private int size;
	private long totalElements;
	private int totalPages;
	private int number;
	
	//public ResponsePage(List<T> content, Page<List>originalPage Pageable pageable, long total) {	
	public ResponseObjectsListPage(List<T> content) {
		super(content);
	}
	public <P> ResponseObjectsListPage(List<T> content, Page<P> origPage, Pageable pageable) {
		super(content, pageable, origPage.getTotalElements());
		setTotalElements(origPage.getTotalElements());
		setTotalPages(origPage.getTotalPages());
		setSize(origPage.getSize());
		setNumber(origPage.getNumber() + 1);
	}
	
	//==== 
	@Override
	public int getSize() {
		return size;
	}
	public void setSize(int size) {
		this.size = size;
	}
	@Override
	public long getTotalElements() {
		return totalElements;
	}
	public void setTotalElements(long totalElements) {
		this.totalElements = totalElements;
	}
	@Override
	public int getTotalPages() {
		return totalPages;
	}
	public void setTotalPages(int totalPages) {
		this.totalPages = totalPages;
	}
	@Override
	public int getNumber() {
		return number;
	}
	public void setNumber(int number) {
		this.number = number;
	}

}
