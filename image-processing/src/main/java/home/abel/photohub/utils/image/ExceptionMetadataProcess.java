package home.abel.photohub.utils.image;

public class ExceptionMetadataProcess extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public ExceptionMetadataProcess(String message) {
        super(message);
    }

    public ExceptionMetadataProcess(String message, Exception e) {
        super(message,e);
    }

    public ExceptionMetadataProcess(Exception e) {
        super(e);
    }
}
