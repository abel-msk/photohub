package home.abel.photohub.connector.prototype;

public class ImageProcessingException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public ImageProcessingException(String message) {
		super(message);
	}
	
	public ImageProcessingException(String message, Exception e) {
		super(message, e);
	}
	
	public ImageProcessingException(Exception e) {
		super(e.getMessage(), e);
	}
}
