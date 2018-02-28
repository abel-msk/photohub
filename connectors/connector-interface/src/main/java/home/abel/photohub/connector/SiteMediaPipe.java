package home.abel.photohub.connector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.*;

public class SiteMediaPipe {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    protected HeadersContainer  ResponseHdr = new HeadersContainer();
    protected String status = "200";
    protected InputStream inputStream = null;
    protected String error = null;


    public void addHeader(String hdrName, String hdrValue) {
        ResponseHdr.addHeader(hdrName ,hdrValue);
    }

    public Set<String> getHdrKeys() {
        return ResponseHdr.keySet();
    }
    public List<String> getHdrValues(String key) {
        return ResponseHdr.get(key);
    }

    public void addHeadersList(String key, List<String> values) {
        ResponseHdr.addHeadersList(key,values);
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public HeadersContainer getHeaders() { return ResponseHdr;}

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.setStatus(null);
        this.error = error;
    }
}
