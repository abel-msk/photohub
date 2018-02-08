package home.abel.photohub.web.model;

import java.io.Serializable;

public class BatchResult implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final int STATUS_OK = 0;

    String id;
    int status;
    String message;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
