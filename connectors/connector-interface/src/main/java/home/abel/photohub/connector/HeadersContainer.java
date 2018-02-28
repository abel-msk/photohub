package home.abel.photohub.connector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class HeadersContainer extends HashMap<String,List<String>> {


    public void addHeader(String hdrName, String hdrValue) {
        if (hdrValue != null) {
            List<String> hdrList = this.computeIfAbsent(hdrName, k -> new ArrayList<>());
            hdrList.add(hdrValue);
        }
    }

    public void addHeadersList ( String key, List<String> valuesList) {
        if (valuesList != null) {
            this.replace(key, valuesList);
        }
    }

    public Set<String> getHdrKeys() {
        return this.keySet();
    }
    public List<String> getHdrValues(String key) {
        return this.get(key);
    }

    public String getFirstValue(String key) {
        List<String> valuesList = this.get(key);
        if (valuesList != null) return valuesList.get(0);
        return null;
    }

    public String toString() {
        StringBuilder resp = new StringBuilder();
        for (String key : getHdrKeys()) {
            resp.append(key).append(": [");
            for (String value :getHdrValues(key)) {
                resp.append("'").append(value).append("',");
            }
            resp.append("]\n");
        }
        return resp.toString();
    }


}
