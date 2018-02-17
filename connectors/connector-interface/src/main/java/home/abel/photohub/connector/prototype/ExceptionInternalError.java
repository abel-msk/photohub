package home.abel.photohub.connector.prototype;

public class ExceptionInternalError extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public ExceptionInternalError(String message) {
        super(message);
    }
    public ExceptionInternalError(String message,Throwable old) {
        super(message,old);
    }

}
