/*
 * StadtinfoData.java
 *
 * Created on 17. August 2006, 00:57
 *
 */

package de.swers.kiezatlas.tools;

/**
 *
 * @author Ramazan
 */
public class StadtinfoData extends java.util.Hashtable {
    /** Creates a new instance of StadtinfoData */
    public StadtinfoData() {
        super(); // 22 items expected
    }
    
    public void setData(String key, String value) {
        this.put(key, value);
    }
    
    public String getData(String key) {
        Object ret = this.get(key);
        if (ret == null) return "";
        return (String) ret;
    }
    
}
