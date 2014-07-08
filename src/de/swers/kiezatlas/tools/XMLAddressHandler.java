/*
 * XMLAddressHandler.java
 *
 * Created on 16. August 2006, 21:41
 *
 */

package de.swers.kiezatlas.tools;

import org.xml.sax.HandlerBase;
import org.xml.sax.AttributeList;

/**
 *
 * @author Ramazan
 */
public class XMLAddressHandler extends HandlerBase{
    
    private String currentTag=""; 
    private int currentIndex=0;
    private int noOfItems=0;
    public StadtinfoData stadtinfoData[] = new StadtinfoData[2000];
    
    /** Creates a new instance of XMLAddressHandler */
    public XMLAddressHandler() {
    }
    
    public void startElement(String name, AttributeList atts) {
        // System.out.println("Start : " + name);
        this.currentTag=name;
        if (name.equalsIgnoreCase("Stadtinfo")) {
            stadtinfoData[currentIndex] = new StadtinfoData();
        }
    }
    
    public void characters (char ch[], int start, int length) {
        String content = new String(ch, start, length);
        // System.out.println("     >"+content+"<");
        if (currentTag.compareToIgnoreCase("dataroot")!=0 && currentTag.compareToIgnoreCase("stadtinfo")!=0 && ch[start]!='\n') {
            if (content.length()>1) {
                if (content.charAt(0)=='\"') {content = content.substring(1);}
                if (content.charAt(content.length()-1)=='\"') {content = content.substring(0,content.length()-1);}
            }
            stadtinfoData[currentIndex].setData(currentTag, content);
        }
    }

    public void endElement(String name) {
        // System.out.println("Ende : " + name);
        if (name.equalsIgnoreCase("Stadtinfo")) {
            this.noOfItems++;
            this.currentIndex++;
        }
    }
    
    public int getNoOfItems() {
        return this.noOfItems;
    }
}
