package home.abel.photohub.web.model;

public class DefaultPageResponse<T> extends DefaultObjectResponse<T> {
	private static final long serialVersionUID = 1L;
	
	public int limit=50;
	public long offset=0;
	

	public DefaultPageResponse(String msg, Integer retCode, T object) {
		super(msg, retCode, object);
	}
		
	public DefaultPageResponse(T object) {
		super("OK", 0, object);
		this.object = object;
	}
	
	public long getOffset() {
		return offset;
	}

	public void setOffset(long offset) {
		this.offset = offset;
	}

	public int getLimit() {
		return limit;
	}

	public void setLimit(int limit) {
		this.limit = limit;
	}

	
	

}
