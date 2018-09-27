package home.abel.photohub.connector.prototype;

public interface KeyStoreInt {

    public ConnectionKeyInt getKey() throws Exception;
    public void setKey (ConnectionKeyInt theKey) throws Exception;
    public void deleteKey () throws Exception;


}
