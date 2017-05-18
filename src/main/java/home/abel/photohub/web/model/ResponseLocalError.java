package home.abel.photohub.web.model;

import java.io.Serializable;

public class ResponseLocalError implements Serializable {
	private static final long serialVersionUID = 1L;
	
	String message = "Error";
	String error   = "1";
	
	ResponseLocalError() {
		
	}
	ResponseLocalError(Throwable e) {
		message = e.getLocalizedMessage();
	}	
	ResponseLocalError(Throwable e, String err) {
		message = e.getLocalizedMessage();
		error = err;
	}		
	ResponseLocalError(String msg, String err) {
		message = msg;
		error = err;
	}		
	
	public void setError(String err) {
		error = err;
	}
	
	public void setMessage(String msg) {
		message = msg;
	}	
	
	public String getError() {
		return error;
	}
	
	public String getMessage() {
		return message;
	}	
	
	@Override
	public String toString()  {
		return message + " Status = " + error;
	}
	
}
