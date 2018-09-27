package home.abel.photohub.connector;

import home.abel.photohub.connector.prototype.ConnectionKeyInt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BaseConnKey implements ConnectionKeyInt {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    protected  String key;
    protected  String data;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
