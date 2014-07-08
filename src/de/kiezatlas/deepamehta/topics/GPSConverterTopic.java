package de.kiezatlas.deepamehta.topics;

import de.deepamehta.AmbiguousSemanticException;
import de.kiezatlas.deepamehta.KiezAtlas;
//
import de.deepamehta.BaseTopic;
import de.deepamehta.service.ApplicationService;
import de.deepamehta.service.CorporateDirectives;
import de.deepamehta.service.CorporateCommands;
import de.deepamehta.service.Session;
import de.deepamehta.topics.LiveTopic;
//
import de.deepamehta.topics.TypeTopic;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.*;



/**
 * Kiezatlas 1.6.7<br>
 * Requires DeepaMehta 2.0b8
 * <p>
 * Last functional change: 29.11.2010<br>
 * Malte Reißig<br>
 * mre@deepamehta.de
 */
public class GPSConverterTopic extends LiveTopic implements KiezAtlas {



	// *******************
	// *** Constructor ***
	// *******************



	public GPSConverterTopic(BaseTopic topic, ApplicationService as) {
		super(topic, as);
	}



	// **********************
	// *** Defining Hooks ***
	// **********************



	// ------------------
	// --- Life Cycle ---
	// ------------------



	public CorporateDirectives evoke(Session session, String topicmapID, String viewmode) {
		return super.evoke(session, topicmapID, viewmode);
	}



	// --------------------------
	// --- Providing Commands ---
	// --------------------------



	public CorporateCommands contextCommands(String topicmapID, String viewmode,
								Session session, CorporateDirectives directives) {
		CorporateCommands commands = new CorporateCommands(as);
		int editorContext = as.editorContext(topicmapID);
		//
		commands.addNavigationCommands(this, editorContext, session);
		commands.addSeparator();
		// --- "Load GPS Coordinates" ---
        commands.addCommand(ITEM_LOAD_COORDINATES, CMD_START_GEOCODING);
        commands.addCommand(ITEM_LOAD_EMPTY_COORDINATES, CMD_START_EMPTY_GEOCODING);
		// --- standard topic commands ---
		commands.addStandardCommands(this, editorContext, viewmode, session, directives);
		//
		return commands;
	}



	// --------------------------
	// --- Executing Commands ---
	// --------------------------



	public CorporateDirectives executeCommand(String command, Session session, String topicmapID, String viewmode) {
		CorporateDirectives directives = new CorporateDirectives();
		StringTokenizer st = new StringTokenizer(command, COMMAND_SEPARATOR);
		String cmd = st.nextToken();
		if (cmd.equals(CMD_START_GEOCODING)) {
        // sets NEW GPS DATA on EVERY GeoObject (or pone of its subtype) of the related workspace
        Vector workspaces = getAssignedWorkspaces();
        StringBuffer htmlReport = new StringBuffer("<html><head><title>GPS Transform Report</title></head><body>");
        htmlReport.append("<h3>GPS Transform Report for "+workspaces.size()+" workspace/s</h3>");
        for (int i = 0; i<workspaces.size(); i++) {
            // for each workspace assigned to the GPSConverterTopic
            BaseTopic workspace = (BaseTopic) workspaces.get(i);
            htmlReport.append("<hr>");
            htmlReport.append("Im Workspace: <b>" + workspace.getName() + "</b><br/>");
            System.out.println("    searching on workspace ("+i+"/"+workspaces.size()+"): " +
                    "" + workspace.getName() + ":" + workspace.getID());
            BaseTopic geotype = getWorkspaceGeoType(workspace.getID());
            if (geotype == null) {
                directives.add(DIRECTIVE_SHOW_MESSAGE, "Please assign the GPSConverter Topic to a " +
                        "'KiezAtlas' Workspace. No 'GeoObject' was found.", new Integer(NOTIFICATION_ERROR));
                return directives;
            } else {
                Vector geos = getAllGeoObjects(geotype.getID());
                // start report
                htmlReport.append("sind insgesamt " + geos.size() + " "+geotype.getName()+" zu verarbeiten.<br/><p/>");
                htmlReport.append("");
                geos = updateAllGeoCoordinates(geos);
                // re-try as long as geoObjects are returned, max. 10 times
                int runs = 10;
                scenarioloop:
                for (int r = 0; r<=runs; r++) {
                    System.out.println("\n*** updateAllCoordinates of " + geos.size() + " faulty objects / "+r+". try again");
                    geos = updateAllGeoCoordinates(geos);
                    if (r == 10) {
                        // after 10 retries report
                        htmlReport.append("<br>Adressfehler sind bei insgesamt <b>"+geos.size()+"</b> Objekten aufgetreten:");
                        // checkout the corresponding cityMap
                        htmlReport.append("<ul>");
                        for(int a = 0; a < geos.size(); a++) {
                            BaseTopic geo = (BaseTopic) geos.get(a);
                            GeoObjectTopic geoTopic = (GeoObjectTopic) as.getLiveTopic(geo);
                            if (geoTopic.getAddress() != null) {
                                htmlReport.append("<li><a href=\"http://"+ACTION_REVEAL_TOPIC+"/"+geoTopic.getAddress().getID()+"\">" +
                                        ""+geoTopic.getAddress().getName()+", " +
                                        ""+ geoTopic.getName() + "</li>");
                            } else {
                                htmlReport.append("<li><a href=\"http://"+ACTION_REVEAL_TOPIC+"/"+geoTopic.getID()+"\">" +
                                        ""+ geoTopic.getName() +"</a></li>");
                            }
                            //http://revealTopic|t-195554"
                            //http://revealTopic/t-256022
                            System.out.println("*** Fail with AdressURL : \""+ getAddressURL(geoTopic.getID()) + "\"");
                        }
                        htmlReport.append("</ul>");
                        htmlReport.append("<br/>Auch nach mehrmaligen Anfragen sind folgende Adressen " +
                                "nicht automatisch in GPS Koordinaten aufzulösen. " +
                                "Bitte überprüfen Sie die Schreibweise bzw. Korrektheit der Adressangaben im einzelnen.");
                        break scenarioloop;
                    }
                }
            }

        }
        htmlReport.append("</body></html>");
        Hashtable props = new Hashtable();
        props.put(PROPERTY_DESCRIPTION, htmlReport.toString());
        directives.add(DIRECTIVE_SHOW_TOPIC_PROPERTIES, this.getID(), props, new Integer(1));
        as.setTopicProperty(this, PROPERTY_DESCRIPTION, htmlReport.toString());
		} else if (cmd.equals(CMD_START_EMPTY_GEOCODING)){
        // sets NEW GPS DATA just on GeoObject LACKING GPS Coordinates yet (subtypes of geoobjects ) of the related workspace
        // cheap copy of the methode above
        Vector workspaces = getAssignedWorkspaces();
        StringBuffer htmlReport = new StringBuffer("<html><head><title>GPS Update Report</title></head><body>");
        htmlReport.append("<h3>GPS Update Report for "+workspaces.size()+" workspace/s</h3>");
        for (int i = 0; i<workspaces.size(); i++) {
            // for each workspace assigned to the GPSConverterTopic
            BaseTopic workspace = (BaseTopic) workspaces.get(i);
            htmlReport.append("<hr>");
            htmlReport.append("Im Workspace: <b>" + workspace.getName() + "</b><br/>");
            System.out.println("    searching on workspace ("+i+"/"+workspaces.size()+"): " +
                    "" + workspace.getName() + ":" + workspace.getID());
            BaseTopic geotype = getWorkspaceGeoType(workspace.getID());
            if (geotype == null) {
                directives.add(DIRECTIVE_SHOW_MESSAGE, "Please assign the GPSConverter Topic to a " +
                        "'KiezAtlas' Workspace. No 'GeoObject' was found.", new Integer(NOTIFICATION_ERROR));
                return directives;
            } else {
                Vector geos = getAllGeoObjects(geotype.getID());
                // start report
                htmlReport.append("sind insgesamt " + geos.size() + " "+geotype.getName()+" zu verarbeiten.<br/><p/>");
                htmlReport.append("");
                geos = updateAllEmptyGeoCoordinates(geos);
                // re-try as long as geoObjects are returned, max. 10 times
                int runs = 10;
                scenarioloop:
                for (int r = 0; r<=runs; r++) {
                    System.out.println("\n*** updateAllCoordinates of " + geos.size() + " faulty objects / "+r+". try again");
                    geos = updateAllEmptyGeoCoordinates(geos);
                    if (r == 10) {
                        // after 10 retries report
                        htmlReport.append("<br>Adressfehler sind bei insgesamt <b>"+geos.size()+"</b> Objekten aufgetreten:");
                        // checkout the corresponding cityMap
                        htmlReport.append("<ul>");
                        for(int a = 0; a < geos.size(); a++) {
                            BaseTopic geo = (BaseTopic) geos.get(a);
                            GeoObjectTopic geoTopic = (GeoObjectTopic) as.getLiveTopic(geo);
                            if (geoTopic.getAddress() != null) {
                                htmlReport.append("<li><a href=\"http://"+ACTION_REVEAL_TOPIC+"/"+geoTopic.getAddress().getID()+"\">" +
                                        ""+geoTopic.getAddress().getName()+", " +
                                        ""+ geoTopic.getName() + "</li>");
                            } else {
                                htmlReport.append("<li><a href=\"http://"+ACTION_REVEAL_TOPIC+"/"+geoTopic.getID()+"\">" +
                                        ""+ geoTopic.getName() +"</a></li>");
                            }
                            //http://revealTopic|t-195554"
                            //http://revealTopic/t-256022
                            System.out.println("*** Fail with AdressURL : \""+ getAddressURL(geoTopic.getID()) + "\"");
                        }
                        htmlReport.append("</ul>");
                        htmlReport.append("<br/>Auch nach mehrmaligen Anfragen sind folgende Adressen " +
                                "nicht automatisch in GPS Koordinaten aufzulösen. " +
                                "Bitte überprüfen Sie die Schreibweise bzw. Korrektheit der Adressangaben im einzelnen.");
                        break scenarioloop;
                    }
                }
            }

        }
        htmlReport.append("</body></html>");
        Hashtable props = new Hashtable();
        props.put(PROPERTY_DESCRIPTION, htmlReport.toString());
        directives.add(DIRECTIVE_SHOW_TOPIC_PROPERTIES, this.getID(), props, new Integer(1));
        as.setTopicProperty(this, PROPERTY_DESCRIPTION, htmlReport.toString());
		} else if (cmd.equals(CMD_FOLLOW_HYPERLINK)){
            //directives.add(DIRECTIVE_, this.getID(), props, new Integer(1));
			// delegate to super class to handle ACTION_REVEAL_TOPIC
            return super.executeCommand(command, session, topicmapID, viewmode);
		} else {
            return super.executeCommand(command, session, topicmapID, viewmode);
        }
		return directives;
	}



	// --------------------------
	// --- Utility Methods ---
	// --------------------------



  private Vector getAssignedWorkspaces() {
      Vector workspaces = as.getRelatedTopics(this.getID(), ASSOCTYPE_ASSOCIATION, TOPICTYPE_WORKSPACE, 2);
      // System.out.println("    workspaces found count: " + workspaces.size());
      return workspaces;
  }

  public Vector disabledProperties(Session session) {
      Vector disabledProps = new Vector();
      //
          disabledProps.addElement(PROPERTY_DESCRIPTION);
      //
      return disabledProps;
	}

    /**
     * returns null if no topictype whihc is assigned to the given workspace, is a subtype of "GeoObjectTopic"
     * @param workspaceId
     * @return
     */
    private BaseTopic getWorkspaceGeoType(String workspaceId) {
        //
        TypeTopic geotype = as.type(TOPICTYPE_KIEZ_GEO, 1);
        Vector subtypes = geotype.getSubtypeIDs();
        Vector workspacetypes = as.getRelatedTopics(workspaceId, ASSOCTYPE_USES, 2);
        int i;
        // ### matching the id with object in vector
        for (i = 0; i < workspacetypes.size(); i++) {
            BaseTopic topic = (BaseTopic) workspacetypes.get(i);
            int a;
            for (a = 0; a < subtypes.size(); a++) {
                // System.out.println(" counter: " + a);
                String derivedOne = (String) subtypes.get(a);
                // System.out.println("    " + derivedOne.getID() + ":" + derivedOne.getName());
                if (derivedOne.equals(topic.getID())) {
                    System.out.println(" GPSConverterTopic runs now on all instances of type " + topic.getID() + ":" + topic.getName());
                    return topic;
                }
                // Vector derivedTopics = cm.getRelatedTopics(TOPICTYPE_KIEZ_GEO, ASSOCTYPE_DERIVATION, 2) ;
            }
            // System.out.println("It's just a related TopicType named " + topic.getID() + ":" + topic.getName());
        }
        return null;
    }

    private Vector getAllGeoObjects(String typeID) {
        // Hashtable props;
        Vector geoObjects = cm.getTopics(typeID);
        System.out.println("    found " + geoObjects.size() + " topics of type (" + typeID + "): " );
        return geoObjects;
    }

    // ---

    public String removeSpaces(String s) {
        StringTokenizer st = new StringTokenizer(s," ",false);
        String t="";
        while (st.hasMoreElements()) {
            t += st.nextElement();
        }
        return t;
    }

  /**  generates an Address like: "Streetname Housenumber ZIPCODE CITY" and encodes it into an UTF-8 URL **/
	private String getAddressURL(String topicID) {
    String result;
    StringBuffer address = new StringBuffer();
    try {
        // Related Address Topic
        BaseTopic add = as.getRelatedTopic(topicID, ASSOCTYPE_ASSOCIATION, TOPICTYPE_ADDRESS, 2, true);     // emptyAllowed=true
        if (add != null) {
            // Streetname and Housenumber (can contain whitespaces)
            address.append(add.getName());
        } else {
            return "";
        }
        // Postal Code of Address
        String plz = as.getTopicProperty(add, PROPERTY_POSTAL_CODE);
        plz = removeSpaces(plz);
        address.append(" " + plz);
        // city
        GeoObjectTopic geo = (GeoObjectTopic) as.getLiveTopic(topicID, 1);
        String city = geo.getCity();
        city = removeSpaces(city);
        address.append(" " + city);
        try {
            result = URLEncoder.encode(address.toString(), "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            System.out.println(GPSConverterTopic.class.getName()+": "+ex);
            result = address.toString();
        }
        return result;
		} catch (AmbiguousSemanticException e) {
        // Related Address Topic
        BaseTopic add = e.getDefaultTopic();
        if (add != null) {
            // Streetname and Housenumber
            address.append(add.getName());
        } else {
            return "";
        }
        // Postal Code of Address
        String plz = as.getTopicProperty(add, PROPERTY_POSTAL_CODE);
        plz = removeSpaces(plz);
        address.append(" " + plz);
        // city
        GeoObjectTopic geo = (GeoObjectTopic) as.getLiveTopic(topicID, 1);
        String city = geo.getCity();
        city = removeSpaces(city);
        address.append(" " + city);
        System.out.println("*** GPSConverterTopic.getAddress(): AmbigiousSemanticExc. took " + address.toString());
        try {
            result = URLEncoder.encode(address.toString(), "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            System.out.println(GPSConverterTopic.class.getName()+": "+ex);
            result = address.toString();
        }
        return result;
		}
	}

    /** updates All Coordinates */
    private Vector updateAllGeoCoordinates(Vector geoObjects) {
        Vector faultyObjects = new Vector();
        // ### CorporateWebSettings
        // String key = as.getGoogleKey();
        Hashtable requested = new Hashtable(geoObjects.size());
        int i;
        for (i = 0; i < geoObjects.size(); i++) {
            StringBuffer requestUrl = new StringBuffer("http://maps.google.com/maps/geo?");
            BaseTopic geoObject = (BaseTopic) geoObjects.get(i);
            String address = getAddressURL(geoObject.getID());
            if (address.equals("")) {
                System.out.println("*** missing address for GeoObject: " + geoObject.getName());
                if (!faultyObjects.contains(geoObject)) {
                    faultyObjects.add(geoObject);
                }
            } else {
                requestUrl.append("q=");
                // requested. put and remove address
                requestUrl.append(address);
                requestUrl.append("&output=csv" +
                        "&oe=utf8&" +
                        "sensor=false" +
                        "&key=ABQIAAAAyg-5-YjVJ1InfpWX9gsTuxRa7xhKv6UmZ1sBua05bF3F2fwOehRUiEzUjBmCh76NaeOoCu841j1qnQ" +
                        "&gl=de");
                try {
                    String cachedCoords = (String) requested.get(address.toString());
                    // System.out.println("cachedCoords: " + cachedCoords);
                    if(cachedCoords != null && !cachedCoords.equals("")) {
                        // skipping requests to already known address
                        String[] points = cachedCoords.split(":");
                        System.out.println("*** " + points[0] +","+points[1]+" known for: " + address +", skipping request but SAVING DATA");
                        as.setTopicProperty(geoObject, PROPERTY_GPS_LAT, points[0]);
                        as.setTopicProperty(geoObject, PROPERTY_GPS_LONG, points[1]);
                    } else {
                        URL url = new URL(requestUrl.toString());
                        URLConnection con = url.openConnection();
                        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                        String inputLine;
                        while ((inputLine = in.readLine()) != null) {
                            // System.out.println("[DEBUG] GeoCodeResponse: "inputLine);
                            // Note: The Google Maps Geocoding API Version 2 has been officially deprecated as of March 8, 2010.
                            // and as of Google's Deprecation Policy will be shut down after 3 years from this date on.
                            String[] points = inputLine.split(",");
                            if (points[2].equals("0") && points[3].equals("0")) {
                                if (!faultyObjects.contains(geoObject)) {
                                    faultyObjects.add(geoObject);
                                }
                                // System.out.println("*** Tryed but failed with loading with "+address.toString()+" for "+geoObject.getName());
                            } else {
                                if (points.length < 4) {
                                  System.out.println("[GPSConverter] something went wrong.. - for ("+geoObject.getID()+"): ");
                                  if (!faultyObjects.contains(geoObject)) {
                                    faultyObjects.add(geoObject);
                                  }
                                } else {
                                  requested.put(address.toString(), points[2] + ":" + points[3]);
                                  as.setTopicProperty(geoObject, PROPERTY_GPS_LAT, points[2]);
                                  as.setTopicProperty(geoObject, PROPERTY_GPS_LONG, points[3]);
                                  System.out.println("[GPSConverter] saving: " + requested.get(address.toString()) +
                                          " for: q=\""+ address +"\"");
                                }
                            }
                        }
                        in.close();
                    }
                } catch (IOException e) {
                    // e.printStackTrace();
                    System.out.println("*** service could not connect, " + e.getMessage());
                    if (!faultyObjects.contains(geoObject)) {
                        faultyObjects.add(geoObject);
                    }
                }
            }
        }
        return faultyObjects;
    }


    private boolean hasEmptyCoordinates(BaseTopic geo) {
      if (as.getTopicProperty(geo, PROPERTY_GPS_LAT).equals("")
              | as.getTopicProperty(geo, PROPERTY_GPS_LONG).equals("")
              | as.getTopicProperty(geo, PROPERTY_GPS_LAT).equals("0")
              | as.getTopicProperty(geo, PROPERTY_GPS_LONG).equals("0")) {
        return true;
      }
      return false;
    }

    /** updates All GeoObjects without any coordinates */
    private Vector updateAllEmptyGeoCoordinates(Vector geoObjects) {
        Vector faultyObjects = new Vector();
        // ### CorporateWebSettings
        // String key = as.getGoogleKey();
        Hashtable requested = new Hashtable(geoObjects.size());
        int i;
        for (i = 0; i < geoObjects.size(); i++) {
            StringBuffer requestUrl = new StringBuffer("http://maps.google.com/maps/geo?");
            BaseTopic geoObject = (BaseTopic) geoObjects.get(i);
            String address = getAddressURL(geoObject.getID());
            if (address.equals("")) {
                System.out.println("*** still missing address for GeoObject: " + geoObject.getName());
                if (!faultyObjects.contains(geoObject)) {
                    faultyObjects.add(geoObject);
                }
            } else if (hasEmptyCoordinates(geoObject)) {
                requestUrl.append("q=");
                // requested. put and remove address
                requestUrl.append(address);
                requestUrl.append("&output=csv" +
                        "&oe=utf8&" +
                        "sensor=false" +
                        "&key=ABQIAAAAyg-5-YjVJ1InfpWX9gsTuxRa7xhKv6UmZ1sBua05bF3F2fwOehRUiEzUjBmCh76NaeOoCu841j1qnQ" +
                        "&gl=de");
                try {
                    String cachedCoords = (String) requested.get(address.toString());
                    // System.out.println("cachedCoords: " + cachedCoords);
                    if(cachedCoords != null && !cachedCoords.equals("")) {
                        // skipping requests to already known address
                        String[] points = cachedCoords.split(":");
                        System.out.println("*** " + points[0] +","+points[1]+" known for: " + address +", skipping request but SAVING DATA");
                        as.setTopicProperty(geoObject, PROPERTY_GPS_LAT, points[0]);
                        as.setTopicProperty(geoObject, PROPERTY_GPS_LONG, points[1]);
                    } else {
                        URL url = new URL(requestUrl.toString());
                        URLConnection con = url.openConnection();
                        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                        String inputLine;
                        while ((inputLine = in.readLine()) != null) {
                            //System.out.println(inputLine);
                            String[] points = inputLine.split(",");
                            if (points[2].equals("0") && points[3].equals("0")) {
                                if (!faultyObjects.contains(geoObject)) {
                                    faultyObjects.add(geoObject);
                                }
                                // System.out.println("*** Tryed but failed with loading with "+address.toString()+" for "+geoObject.getName());
                            } else {
                                if (points.length < 4) {
                                  System.out.println("[GPSConverter] something went wrong.. - for ("+geoObject.getID()+"): ");
                                  if (!faultyObjects.contains(geoObject)) {
                                    faultyObjects.add(geoObject);
                                  }
                                } else {
                                  requested.put(address.toString(), points[2] + ":" + points[3]);
                                  as.setTopicProperty(geoObject, PROPERTY_GPS_LAT, points[2]);
                                  as.setTopicProperty(geoObject, PROPERTY_GPS_LONG, points[3]);
                                  System.out.println("[GPSConverter] saving: " + requested.get(address.toString()) +
                                          " for: q=\""+ address +"\"");
                                }
                            }
                        }
                        in.close();
                    }
                } catch (IOException e) {
                    // e.printStackTrace();
                    System.out.println("*** service could not connect, " + e.getMessage());
                    if (!faultyObjects.contains(geoObject)) {
                        faultyObjects.add(geoObject);
                    }
                }
            }
        }
        return faultyObjects;
    }

}
