package home.abel.photohub.connector;

import home.abel.photohub.connector.prototype.KeyStoreInt;

import javax.sql.DataSource;

public class KeyStoreFactory {

    protected DataSource dataSource;

    public KeyStoreFactory(DataSource ds) {
        dataSource = ds;
    }

    public KeyStoreInt getKeyStore(String ID) {
        return new BaseKeyStore(ID,dataSource);

    }



}
