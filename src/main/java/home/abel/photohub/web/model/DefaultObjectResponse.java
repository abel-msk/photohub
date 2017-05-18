package home.abel.photohub.web.model;

import java.io.Serializable;

public class DefaultObjectResponse<T> extends DefaultResponse implements Serializable {
	private static final long serialVersionUID = 1L;	
	//ResponsePhotoObject object = null;
	T object = null;
	
	public DefaultObjectResponse() {
		//super();
	}
	
	public DefaultObjectResponse(String msg, Integer retCode) {
		super(msg, retCode);
		this.object = null;
	}
	
	public DefaultObjectResponse(String msg, Integer retCode,T object) {
		super(msg, retCode);
		this.object = object;
	}
	
	public DefaultObjectResponse(T object) {
		super("OK", 0);
		this.object = object;
	}

	
	public T getObject() {
		return object;
	}

	public void setObject(T object) {
		this.object = object;
	}

}
