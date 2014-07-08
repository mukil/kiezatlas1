/*
 * XMLAddressFileReader.java
 *
 * Created on 16. August 2006, 23:17
 *
 */

package de.swers.kiezatlas.tools;

import org.xml.sax.Parser;
import org.xml.sax.DocumentHandler;
import org.xml.sax.helpers.ParserFactory;

/**
 *
 * @author Ramazan Sarikaya
 */
public class XMLAddressFileReader {
    
    private XMLAddressHandler handler = new XMLAddressHandler();
    
    /** Creates a new instance of XMLAddressFileReader */
    public XMLAddressFileReader() {
    }
    
    public XMLAddressHandler getXMLAddressHandler() {
        return this.handler;
    }

    public XMLAddressFileReader(String filename) throws Exception {
        Parser parser = ParserFactory.makeParser("org.apache.xerces.parsers.SAXParser");
        parser.setDocumentHandler(handler);
        parser.parse(filename);
    }
}
