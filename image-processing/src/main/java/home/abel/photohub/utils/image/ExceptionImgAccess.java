package home.abel.photohub.utils.image;

public class ExceptionImgAccess extends Exception {
	private static final long serialVersionUID = 1L;

	
	public ExceptionImgAccess (String message){
		super(message);
	}
	
	public ExceptionImgAccess(Throwable cause) {
		super("Nested from: ",cause);
	}
	
	public ExceptionImgAccess(String message, Throwable cause) {
		super(message, cause);
	}
}
