package home.abel.photohub.utils.image;

public class ExceptionIncorrectImgFormat extends Exception {
	private static final long serialVersionUID = 1L;
	
	public ExceptionIncorrectImgFormat(String message) {
		super(message);
	}
	
	public ExceptionIncorrectImgFormat(String message, Exception e) {
		super(message,e);
	}
	
	public ExceptionIncorrectImgFormat(Exception e) {
		super(e);
	}	
}
