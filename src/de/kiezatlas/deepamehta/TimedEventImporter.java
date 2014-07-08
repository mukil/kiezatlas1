package de.kiezatlas.deepamehta;

import de.deepamehta.BaseTopic;
import de.deepamehta.DeepaMehtaConstants;
import de.deepamehta.DeepaMehtaException;
import de.deepamehta.assocs.LiveAssociation;
import de.deepamehta.service.ApplicationService;
import de.deepamehta.service.CorporateDirectives;
import de.deepamehta.service.CorporateMemory;
import de.deepamehta.topics.EmailTopic;
import de.deepamehta.topics.LiveTopic;
import de.deepamehta.topics.TypeTopic;
import de.deepamehta.util.DeepaMehtaUtils;
import de.kiezatlas.deepamehta.topics.GeoObjectTopic;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Hashtable;
import java.util.Vector;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * Event Calendar Schnittstelle 1.0
 * is a thread based worker which imports single projects and all occuring criterias resp. categories
 * from an xml interface into one kiezatlas workspace - it reuses topics (e.g. addresstopics) known to
 * the cm (by name), triggers a geolocation on each project with nice address
 *
 * @author Malte Reißig (mre@deepamehta.de)
 */
public class TimedEventImporter implements Job, DeepaMehtaConstants, KiezAtlas {

    private ApplicationService as = null;
    private CorporateMemory cm = null;
    private CorporateDirectives directives = null;

    private String workspaceId = "";
    private String cityMapId = "";
    private String serviceUrl = "";
    // settings
    private String contentReportRecipient = "";
    private String serviceReportRecipient = "";
    private String iconCatMap = ""; // remains empty for event importer
    // 
    // to be imported Event TopicTypes
    static final String TOPICTYPE_EVT_EVENT = "t-453276";
    static final String TOPICTYPE_EVT_BEZIRK = "t-453278";
    static final String TOPICTYPE_EVT_KATEGORIE = "t-453280";

    public TimedEventImporter () {
      as = (ApplicationService) TimerJobParameterHolder.getInstance().getParameters().get("as");
      cm = (CorporateMemory) TimerJobParameterHolder.getInstance().getParameters().get("cm");
      directives = (CorporateDirectives) TimerJobParameterHolder.getInstance().getParameters().get("directives");
      workspaceId = (String) TimerJobParameterHolder.getInstance().getParameters().get("eventWorkspaceId");
      cityMapId = (String) TimerJobParameterHolder.getInstance().getParameters().get("eventCityMapId");
      serviceUrl = (String) TimerJobParameterHolder.getInstance().getParameters().get("eventServiceUrl");
    }

    public void execute(JobExecutionContext context) throws JobExecutionException {
      //
      System.out.println("[EventJob] was kicked off for workspace \"" + workspaceId + "\" at "
              + DeepaMehtaUtils.getTime().toString());
      BaseTopic settings = getImporterSettingsTopic(); // 
      if (settings != null) {
          contentReportRecipient = as.getTopicProperty(settings, PROPERTY_IMPORT_CONTENT_REPORT);
          serviceReportRecipient = as.getTopicProperty(settings, PROPERTY_IMPORT_SERVICE_REPORT);
          iconCatMap = as.getTopicProperty(settings, PROPERTY_ICONS_MAP);
          System.out.println("[EventJob] set to run for \"" + contentReportRecipient + "\" " +
              "errors are reported to " + serviceReportRecipient + " iconsMap available ? "
              + (iconCatMap.length() > 0));
      } else { System.out.println("[EventJob] ERROR while loading settings for workspace " + workspaceId); }
      // work here
      String ehrenamtXml = sendGetRequest(serviceUrl, "");
      if (ehrenamtXml != null) {
          System.out.println("[EventJob] loaded data.. and is now scheduled to 06:15 AM.. for cityMapID: " + cityMapId);
          // delete former import
          clearCriterias();
          // clears workspace if new topics are available
          clearImport();
          // store and publish new topics
          Vector topicIds = parseAndStoreData(ehrenamtXml);
          //
          publishData(topicIds);
      }
    }

    // --
    // --- Utilities
    // --


    private BaseTopic getImporterSettingsTopic() {
        Vector workspaces = as.getRelatedTopics(workspaceId, ASSOCTYPE_ASSOCIATION, TOPICTYPE_IMPORTER_SETTINGS, 1);
        if (workspaces.size() >= 1) {
            BaseTopic settings = (BaseTopic) workspaces.get(0);
            return settings;
        }
        return null;
    }

    private void publishData(Vector topicIds) {
        System.out.println("[EventJob] is starting to gather coordinates and publish \"" + topicIds.size() +
            "\" topics into : " + cm.getTopic(cityMapId, 1).getName());// + " " +
        Vector unusable = new Vector(); // collection of GeoObjects with GPS Data
        for (int i = 0; i < topicIds.size(); i++) {
            GeoObjectTopic baseTopic = (GeoObjectTopic) as.getLiveTopic(((String)topicIds.get(i)), 1);
            // System.out.println("[GPSINFO] is LAT: " + as.getTopicProperty(baseTopic, PROPERTY_GPS_LAT) + " LON: "
            // + as.getTopicProperty(baseTopic, PROPERTY_GPS_LONG));
            BaseTopic addressTopic = baseTopic.getAddress();
            if (as.getTopicProperty(baseTopic.getID(), 1, PROPERTY_GPS_LAT).equals("")) {
                System.out.println("[EventJob] WARNING ***  \"" + addressTopic.getName() +
                    "\" is without GPS Data... dropping placement in CityMap");
                // ###TODO: Report Functionality
                unusable.add(baseTopic); //
            } else if (as.getTopicProperty(addressTopic, PROPERTY_STREET).equals("über Gute-Tat.de")) {
                unusable.add(baseTopic);
            } else {
                as.createViewTopic(cityMapId, 1, null, baseTopic.getID(), 1, 0, 0, false);
            }
        }
        int validEntries = topicIds.size() - unusable.size();
        //
        System.out.println("[EventJob] stored " + validEntries + " in public cityMap \"" 
            + as.getTopicProperty(cityMapId, 1, PROPERTY_WEB_ALIAS)+ "\"");
        System.out.println("[EventJob] skipped " + unusable.size() + " unlocatable \"" 
            + getWorkspaceGeoType(workspaceId).getName()+"\"");
        as.setTopicProperty(cityMapId, 1, PROPERTY_LAST_UPDATED, "" + new Date().getTime() + "");
        sendNotificationEmail(unusable);
        //
    }

    /** remove criteria system from configured workspace */
    private void clearCriterias() {
        Vector bezirke = cm.getTopics(TOPICTYPE_EVT_BEZIRK);
        Vector einsatzbereiche = cm.getTopics(TOPICTYPE_EVT_KATEGORIE);
        bezirke.addAll(einsatzbereiche);
        //
        for (int i = 0; i < bezirke.size(); i++) {
            BaseTopic category = (BaseTopic) bezirke.get(i);
            CorporateDirectives newDirectives = as.deleteTopic(category.getID(), 1);
            newDirectives.updateCorporateMemory(as, null, null, null);
            System.out.println("[INFO] deleted Event Category: " + category.getName());
        }
    }

    /** completely remove all topics created by the last import
     *  e.g. delete from topicmap, delete related address topic, delete email topic,
     *  delete person topic, delete webpage topic, delete webpage topic if not used by any other topic ...
     */
    private void clearImport() {
        /** import data is, date of last import, complete or not complete, error objects */
        Vector allGeoObjects = cm.getTopics(getWorkspaceGeoType(workspaceId).getID());
        Vector allRelatedTopics = new Vector();
        System.out.println("[EventJob] cleaning up " +allGeoObjects.size()+ " of type \""
                + getWorkspaceGeoType(workspaceId).getID()+"\" --- ");
        for (int i = 0; i < allGeoObjects.size(); i++) {
            BaseTopic baseTopic = (BaseTopic) allGeoObjects.get(i);
            Vector relatedTopics = as.getRelatedTopics(baseTopic.getID(), ASSOCTYPE_ASSOCIATION, 2);
            for (int j = 0; j < relatedTopics.size(); j++) {
                BaseTopic relatedTopic = (BaseTopic) relatedTopics.get(j);
                // to get all related topics except, the categories subtypes of tt-topictpy-crit.
                if (relatedTopic.getType().equals(TOPICTYPE_EVT_BEZIRK) ||
                        relatedTopic.getType().equals(TOPICTYPE_EVT_KATEGORIE)) {
                } else {
                    // store topicID in Vector for later removal
                    allRelatedTopics.add(relatedTopic);
                }
            }
            directiveDeletion(baseTopic.getID());
            //
        }
        //
        System.out.println("[EventJob] " + allRelatedTopics.size() + " relatedTopics to be deleted (just "
            + " if one entry has no associations)---");
        for (int k = 0; k < allRelatedTopics.size(); k++) {
            BaseTopic relTopic = (BaseTopic) allRelatedTopics.get(k);
            //
            if (cm.getAssociationIDs(relTopic.getID(), 1).size() > 0) {
                // System.out.println(">>> relTopic ("+relTopic.getID()+") \"" + relTopic.getName()
                // + "\" not to delete, has "+cm.getAssociationIDs(relTopic.getID(), 1).size()+" other associations");
            } else {
               directiveDeletion(relTopic.getID());
            }
        }
    }

    /** performs a clear deletion of a topic with all it`s associations and it`s removal from all maps*/
    private void directiveDeletion(String topicID) {
		// CorporateDirectives myDirective = as.deleteTopic(topicID, 1);	// ### version=1
        try {
            LiveTopic topic = as.getLiveTopic(topicID, 1);
            if (topic != null) {
                // topic.del
                CorporateDirectives newDirectives = as.deleteTopic(topic.getID(), 1);	// ### version=1
                newDirectives.updateCorporateMemory(as, null, null, null);
            }
        } catch (DeepaMehtaException dex) {
            // yeah, it`s not known to CM..
        }
	}

    /**
       * just reads in THE xml String from berlin.de and saves every single event into the kiezatlas cm
     *
     * @param xmlData
     * @return
     */
    private Vector parseAndStoreData(String xmlData) {
    Vector topicIds = new Vector();
    try {

        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
        Document doc = docBuilder.parse(new InputSource(new StringReader(xmlData)));

        // normalize text representation..
        doc.getDocumentElement().normalize();
        //
        NodeList listOfProjects = doc.getElementsByTagName("project");
        int amountOfProjects = listOfProjects.getLength();
        System.out.println("");
        System.out.println(" --- Import started at "+DeepaMehtaUtils.getTime() +" for a total no of "
            + amountOfProjects + " " + getWorkspaceGeoType(workspaceId).getName());
        // for(int s = 0; s < listOfProjects.getLength(); s++){
        System.out.println(" -- ");
        System.out.println("");
        Vector misspelledObjects = new Vector();
        // iterate over projects
        for(int p = 0; p < amountOfProjects; p++) {
            // fields of each project
            String projectName = "", eventDescription = "", originId = "", contactPerson = "", projectUrl = "", 
                postcode = "", streetNr = "", bezirk = "", orgaName = "", orgaWebsite = "", orgaContact = "",
                timeStamp = "";
            Vector zielgruppen = new Vector();
            NodeList projectDetail = listOfProjects.item(p).getChildNodes();
            for (int i = 0; i < projectDetail.getLength(); i++) {
                Node node = projectDetail.item(i);
                if (node.hasChildNodes()) {
                    NodeList details = node.getChildNodes();
                    for (int j = 0; j < details.getLength(); j++) {
                        Node detailNode = details.item(j);
                        if (detailNode.hasChildNodes()) {
                            // System.out.println(">>>> "+detailNode.getNodeName()+" is ");
                            for(int k = 0; k < detailNode.getChildNodes().getLength(); k++) {
                                Node contentNode = detailNode.getChildNodes().item(k);
                                if (contentNode.hasChildNodes()) {
                                    // process deep project info, such as categories
                                    for (int l = 0; l < contentNode.getChildNodes().getLength(); l++) {
                                        Node anotherNode = contentNode.getChildNodes().item(l);
                                        if (contentNode.getNodeName().equals("contact")) {
                                            // it`s THE contact person
                                            contactPerson = anotherNode.getNodeValue();
                                            // System.out.println("> ansprechpartner: " + anotherNode.getNodeValue());
                                        } else if (contentNode.getNodeName().equals("adress")) {
                                            // it`s THE point of interest
                                            streetNr = anotherNode.getNodeValue();
                                            // System.out.println("> anlaufstelle: " + anotherNode.getNodeValue());
                                        } else if (contentNode.getNodeName().equals("postcode")) {
                                            // it`s THE code of interest
                                            postcode = anotherNode.getNodeValue();
                                            // System.out.println("> plz: " + anotherNode.getNodeValue());
                                        } else if (contentNode.getNodeName().equals("location")) {
                                            // it`s THE category \"Bezirk\"
                                            bezirk = anotherNode.getNodeValue();
                                            // bezirk
                                            // System.out.println("> bezirk: " + bezirk);
                                        } else if (contentNode.getNodeName().equals("zielgruppen")) {
                                            // these are target groups for this project
                                            // System.out.println("> zielgruppen: "); // + anotherNode.getNodeValue());
                                            zielgruppen = readInCategories(anotherNode.getNodeValue());
                                        } else {
                                            System.out.println("*** [EventJob] found unknown category for a POI while "
                                                + "importing from ehrenamt.");
                                        }
                                    }
                                } else {
                                    // check for other interesting data such as
                                    //
                                    if(detailNode.getNodeName().equals("created")) {
                                        // System.out.println("> timestamp is : " + contentNode.getNodeValue());
                                        timeStamp = contentNode.getNodeValue();
                                    } else if (detailNode.getNodeName().equals("projectname")) {
                                        // System.out.println("- Name is \" " + contentNode.getNodeValue() + "\"");
                                        projectName = contentNode.getNodeValue();
                                    } else if (detailNode.getNodeName().equals("projectdescr")) {
                                        // System.out.println("- Name is \" " + contentNode.getNodeValue() + "\"");
                                        eventDescription = contentNode.getNodeValue();
                                    } else if (detailNode.getNodeName().equals("projecturl")) {
                                        // obsolete ? all needed infos should be right here..
                                        projectUrl = contentNode.getNodeValue();
                                        // System.out.println(">website at : " + contentNode.getNodeValue());
                                    } else if (detailNode.getNodeName().equals("organisationname")) {
                                        // System.out.println(">organ. is : " + contentNode.getNodeValue());
                                        orgaName = contentNode.getNodeValue();
                                    } else if (detailNode.getNodeName().equals("contact")) {
                                        // System.out.println(">contactperson is : " + contentNode.getNodeValue());
                                        orgaContact = contentNode.getNodeValue();
                                    } else if (detailNode.getNodeName().equals("url")) {
                                        // System.out.println(">projectpage at : " + contentNode.getNodeValue());
                                        orgaWebsite = contentNode.getNodeValue();
                                    } else if (detailNode.getNodeName().equals("id")) {
                                        // System.out.println(">projectpage at : " + contentNode.getNodeValue());
                                        originId = contentNode.getNodeValue();
                                    }
                                    //System.out.println("data is: " + contentNode.getNodeValue());
                                }
                            }
                        }
                    }
                }
            }
            // projectData was gathered
            // store information per project now
            String topicID = saveProjectData(originId, projectName, eventDescription, contactPerson, 
                projectUrl, postcode, streetNr, bezirk, orgaName, orgaWebsite, orgaContact, timeStamp, zielgruppen);
            topicIds.add(topicID);
        }//end of for loop with p for projects
        System.out.println("[EventJob] stored data at "+DeepaMehtaUtils.getTime() +" for a total no of "
            + amountOfProjects + " " + getWorkspaceGeoType(workspaceId).getName());
    } catch (SAXParseException err) {
           System.out.println ("** Parsing error" + ", line "
             + err.getLineNumber () + ", column " + err.getColumnNumber() + ", message: " + err.getMessage());
    } catch (SAXException e) {
        Exception x = e.getException ();
        ((x == null) ? e : x).printStackTrace ();

    } catch (Throwable t) {
        t.printStackTrace ();
    }
    //System.exit (0);
    return topicIds;
    }

    /**
     *  save one item "Event" into the corporate memory with
     *  reusing existing addresses, webpages and persons
     *  building up the categorySystem by each item which is in some categories
     */
    private String saveProjectData(String originId, String eventName, String eventDescr, String contactPerson,
            String projectUrl, String postcode, String streetNr, String bezirk, String orgaName,
            String orgaWebsite, String orgaContact, String timeStamp, Vector zielgruppen) {
        String topicId = "";
        String address = streetNr + ", " + postcode + " " + bezirk;
        // storing data in corporate memory
        String geoTypeId = getWorkspaceGeoType(workspaceId).getID();
        LiveTopic geoObjectTopic = as.createLiveTopic(as.getNewTopicID(), geoTypeId, eventName, null);
        //
        as.setTopicProperty(geoObjectTopic, PROPERTY_NAME, eventName);
        String descr = "";
        if (eventDescr.length() > 256) { descr = eventDescr.substring(0, 256) + " ..."; } else { descr = eventDescr; }
        as.setTopicProperty(geoObjectTopic, PROPERTY_EVENT_DESCRIPTION, descr);
        as.setTopicProperty(geoObjectTopic, PROPERTY_PROJECT_ORGANISATION, orgaName);
        as.setTopicProperty(geoObjectTopic, PROPERTY_PROJECT_ORIGIN_ID, originId);
        as.setTopicProperty(geoObjectTopic, PROPERTY_EVENT_TIME, timeStamp);
        as.setTopicProperty(geoObjectTopic, PROPERTY_LOCKED_GEOMETRY, "off");
        //
        LiveTopic webpageTopic;
        LiveTopic contactPersonTopic;
        LiveTopic addressTopic;
        LiveTopic cityTopic;
        // LiveTopic mailTopic;
        // check for webpage in cm
        Hashtable webProps = new Hashtable();
        webProps.put(PROPERTY_URL, projectUrl);
        Vector webpages = cm.getTopics(TOPICTYPE_WEBPAGE, webProps);
        // check for URL !!!
        if (webpages.size() > 0) {
            webpageTopic = as.getLiveTopic((BaseTopic)webpages.get(0));
            //
        } else {
            webpageTopic = as.createLiveTopic(as.getNewTopicID(), TOPICTYPE_WEBPAGE, projectUrl, null);
            as.setTopicProperty(webpageTopic.getID(), 1, PROPERTY_URL, projectUrl);
            // as.setTopicProperty(webpageTopic.getID(), 1, PROPERTY_NAME, "weitere Ehrenamt Projektinfos");
        }
        // check for person in cm
        BaseTopic knownPerson = cm.getTopic(TOPICTYPE_PERSON, contactPerson, 1);
        if (knownPerson != null) {
            contactPersonTopic = as.getLiveTopic(knownPerson);
        } else {
            contactPersonTopic = as.createLiveTopic(as.getNewTopicID(), TOPICTYPE_PERSON, contactPerson, null);
        }
        // check for City Topic Berlin in cm
        BaseTopic berlinTopic = cm.getTopic(TOPICTYPE_CITY, "Berlin", 1);
        if (berlinTopic != null) {
            cityTopic = as.getLiveTopic(berlinTopic);
        } else {
            cityTopic = null;
            System.out.println("[WARNING] EventJob is using Property \"Stadt\" at GeoObjectTopic instead of City Topic");
            as.setTopicProperty(geoObjectTopic, PROPERTY_CITY, "Berlin");
        }
        // BaseTopic knownMailbox = cm.getTopic(TOPICTYPE_EMAIL_ADDRESS, orgaContact, 1);
        // check for address in cm
        BaseTopic knownAddress = cm.getTopic(TOPICTYPE_ADDRESS, streetNr, 1);
        // check for street in Adress !!
        if (knownAddress != null) {
            addressTopic = as.getLiveTopic(knownAddress);
        } else {
            addressTopic = as.createLiveTopic(as.getNewTopicID(), TOPICTYPE_ADDRESS, streetNr, null);
            System.out.println(">>> created new Address for " + streetNr);
        }
        // add postalcode to address topic
        as.setTopicProperty(addressTopic, PROPERTY_POSTAL_CODE, postcode);
        as.setTopicProperty(addressTopic, PROPERTY_STREET, streetNr);
        //
        LiveAssociation toWebpage = as.createLiveAssociation(as.getNewAssociationID(), ASSOCTYPE_ASSOCIATION, geoObjectTopic.getID(), webpageTopic.getID(), null, null);
        LiveAssociation toPerson = as.createLiveAssociation(as.getNewAssociationID(), ASSOCTYPE_ASSOCIATION, geoObjectTopic.getID(), contactPersonTopic.getID(), null, null);
        LiveAssociation toAddress = as.createLiveAssociation(as.getNewAssociationID(), ASSOCTYPE_ASSOCIATION, geoObjectTopic.getID(), addressTopic.getID(), null, null);
        if (as.getAssociation(addressTopic.getID(), ASSOCTYPE_ASSOCIATION, 2, TOPICTYPE_CITY, true, directives) == null) {
            LiveAssociation toCity = as.createLiveAssociation(as.getNewAssociationID(), ASSOCTYPE_ASSOCIATION, addressTopic.getID(), cityTopic.getID(), null, null);
        }
        // fetch GPS Data from GeoCoder
        GeoObjectTopic geoObject = (GeoObjectTopic) geoObjectTopic;
        // ### fixme: if address is just "Berlin" which seems to be happening, then G. has a coordinate for that spot !!!
        geoObject.setGPSCoordinates(directives);
        // bezirk label special move
        if (bezirk.startsWith("Berlin-") || bezirk.startsWith("berlin-")) {
            // slice a bit redundancy
            bezirk = bezirk.substring(7);
        }
        // one to one
        BaseTopic bezirkAlreadyKnown = cm.getTopic(TOPICTYPE_EVT_BEZIRK, bezirk, 1);
        if (bezirkAlreadyKnown != null) {
            // connect to known Bezirk
            // System.out.println(">> reusing BezirkTopic \""+bezirkAlreadyKnown.getName()+"\"");
            as.createLiveAssociation(as.getNewAssociationID(), ASSOCTYPE_ASSOCIATION, geoObjectTopic.getID(), bezirkAlreadyKnown.getID(), null, null);
        } else {
            // new Bezirk and connect to
            System.out.println(">> creating "+as.getLiveTopic(TOPICTYPE_EVT_BEZIRK, 1).getName()+" Topic \""+bezirk+"\"");
            LiveTopic bezirkTopic = as.createLiveTopic(as.getNewTopicID(), TOPICTYPE_EVT_BEZIRK, bezirk, null);
            as.createLiveAssociation(as.getNewAssociationID(), ASSOCTYPE_ASSOCIATION, geoObjectTopic.getID(), bezirkTopic.getID(), null, null);
        }
        // one to many
        for (int i = 0; i < zielgruppen.size(); i++) {
            String zielgruppenName = (String) zielgruppen.get(i);
            BaseTopic knownZielgruppe = cm.getTopic(TOPICTYPE_EVT_KATEGORIE, zielgruppenName, 1);
            if (knownZielgruppe != null) {
                // connect to known cat
                // System.out.println(">> reusing ZielgruppenTopic \""+knownZielgruppe.getName()+"\"");
                as.createLiveAssociation(as.getNewAssociationID(), ASSOCTYPE_ASSOCIATION, geoObjectTopic.getID(), knownZielgruppe.getID(), null, null);
            } else {
                System.out.println(">> creating "+as.getLiveTopic(TOPICTYPE_EVT_KATEGORIE, 1).getName()+"Topic \""+zielgruppenName+"\"");
                LiveTopic newZielgruppe = as.createLiveTopic(as.getNewTopicID(), TOPICTYPE_EVT_KATEGORIE, zielgruppenName, null);
                as.createLiveAssociation(as.getNewAssociationID(), ASSOCTYPE_ASSOCIATION, geoObjectTopic.getID(), newZielgruppe.getID(), null, null);
            }
        }
        return geoObjectTopic.getID();
    }

    private Vector readInCategories(String catSeperatedValue) {
        Vector cats = new Vector();
        while (catSeperatedValue.indexOf(";") != -1) {
            int pointer = catSeperatedValue.indexOf(";");
            String catName, restName = "";
            if (pointer != -1) {
                catName = catSeperatedValue.substring(0, pointer);
                restName = catSeperatedValue.substring(pointer + 1);
                cats.add(catName);
                //System.out.println(">> cat: " + catName + " rest: " + restName);
            }
            catSeperatedValue = restName;
        }
        cats.add(catSeperatedValue);
        // System.out.println(">> lastcat: " + catSeperatedValue);
        return cats;
    }

    private String sendGetRequest(String endpoint, String requestParameters) {
        String result = null;
        if (endpoint.startsWith("http://")) {
            // Send a GET request to the servlet
            try {
                // Send data
                String urlStr = endpoint;
                if (requestParameters != null && requestParameters.length() > 0) {
                    urlStr += "?" + requestParameters;
                }
                URL url = new URL(urlStr);
                System.out.println("[EventJob] sending request to: " + url.toURI().toURL().toString());
                URLConnection conn = url.openConnection();
                // Get the response
                // BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream(), "ISO-8859-1"));
                BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                StringBuffer sb = new StringBuffer();
                String line;
                while ((line = rd.readLine()) != null) {
                    sb.append(line);
                }
                rd.close();
                result = sb.toString();
                System.out.println("[EventJob] finished loading data from " + url);
                DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
                Document doc = docBuilder.parse(new InputSource(new StringReader(result)));
            } catch(UnknownHostException uke) {
                System.out.println("*** TimedEventJob could not load the xml data to import from " + endpoint
                    + " message is: " + uke.getMessage());
                return null;
                // done();
            } catch (SAXParseException saxp) {
                System.out.println ("** Parsing error" + ", line "
                    + saxp.getLineNumber () + ", column " + saxp.getColumnNumber() + ", message: " + saxp.getMessage());
                System.out.println("dataValue: " +result.substring(saxp.getColumnNumber()-15, saxp.getColumnNumber()+50));
                System.out.println("*** TimedEventJob is skipping the import for today !");
                return null;
            } catch (Exception ex) {
                System.out.println("*** TimedEventJob encountered problem: " + ex.getMessage());
                return null;
            }
        }
        return result;
    }

    // --- copy of BrowseServlet

    private void sendNotificationEmail(Vector unusableProjects) {
      if (unusableProjects.isEmpty()) return;
      try {
        // GeoObjectTopic inst = (GeoObjectTopic) as.getLiveTopic(instID, 1);
        // "from"
        String from = as.getEmailAddress("t-rootuser");		// ###
        if (from == null || from.equals("")) {
          throw new DeepaMehtaException("email address of root user is unknown");
        }
        // "to"
        String to = "mre@newthinking.de";
        // "subject"
        String subject = "Ehrenamtsatlas: folgende Veranstaltungen haben einen fehlerhaften Addresseintrag";
        StringBuffer entries = new StringBuffer();
        entries.append("------------------------------\r");
        for (int i = 0; i < unusableProjects.size(); i++) {
            GeoObjectTopic entry = (GeoObjectTopic) unusableProjects.get(i);
            entries.append("Veranstaltung: ");
            entries.append(entry.getName());
            entries.append(" mit Adresseintrag: ");
            entries.append(entry.getAddress().getName());
            entries.append("\r");
        }
        entries.append("------------------------------\r");
        // "body"
        String body = "Dies ist eine automatische Benachrichtigung erstellt von www.kiezatlas.de\r\r" +
          "Folgende Veranstaltungen konnten aufgrund eines fehlerhaften Adresseintrags nicht korrekt verortet werden:\r\r" +
          "" + entries.toString() + "\r" +
          "www.berlin.de/buergeraktiv/atlas\r\r" +
          "Mit freundlichen Grüßen\r" +
          "ihr Kiezatlas-Team";
        //
        System.out.println(">>> send notification email");
        System.out.println("  > SMTP server: \"" + as.getSMTPServer() + "\"");	// as.getSMTPServer() throws DME
        System.out.println("  > from: \"" + from + "\"");
        System.out.println("  > to: \"" + to + "\"");
        // send email
        EmailTopic.sendMail(as.getSMTPServer(), from, to, subject, body);		// EmailTopic.sendMail() throws DME
      } catch (Exception e) {
        System.out.println("*** notification email not send (" + e + ")");
      }
    }


    /**
     * returns null if no topictype whihc is assigned to the given workspace,
     * is a subtype of "GeoObjectTopic"
     *
     * @param workspaceId
     * @return
     */
    private BaseTopic getWorkspaceGeoType(String workspaceId) {
        //
        TypeTopic geotype = as.type(TOPICTYPE_KIEZ_GEO, 1);
        Vector subtypes = geotype.getSubtypeIDs();
        Vector workspacetypes = as.getRelatedTopics(workspaceId, ASSOCTYPE_USES, 2);
        int i;
        for (i = 0; i < workspacetypes.size(); i++) {
            BaseTopic topic = (BaseTopic) workspacetypes.get(i);
            int a;
            for (a = 0; a < subtypes.size(); a++) {
                // System.out.println(" counter: " + a);
                String derivedOne = (String) subtypes.get(a);
                // System.out.println("    " + derivedOne.getID() + ":" + derivedOne.getName());
                if (derivedOne.equals(topic.getID())) {
                    // System.out.println(" found geoType " + topic.getID() + ":" + topic.getName());
                    return topic;
                }
            }
        }
        return null;
    }

}
