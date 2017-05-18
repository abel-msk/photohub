package home.abel.photohub.model.auth;

public class TokenAccessException extends Exception {
	private static final long serialVersionUID = 1L;
	
	public TokenAccessException(String message) {
		super(message);
	}
	
	public TokenAccessException(String message,Throwable exception) {
		super(message,exception);
	}


}
