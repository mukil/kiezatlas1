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
import java.util.HashMap;
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
 * Ehrenamt Schnittstelle 1.0
 * is a thread based worker which imports single projects and all occuring criterias resp. categories
 * from an xml interface into one kiezatlas workspace - it reuses topics (e.g. addresstopics) known to
 * the cm (by name), triggers a geolocation on each project with nice address
 *
 * @author Malte Reißig (mre@deepamehta.de)
 */
public class TimedEngagementImporter implements Job, DeepaMehtaConstants, KiezAtlas {

    private ApplicationService as = null;
    private CorporateMemory cm = null;
    private CorporateDirectives directives = null;

    private String workspaceId = "";
    private String cityMapId = "";
    private String serviceUrl = "";
    // settings
    private String contentReportRecipient = "";
    private String serviceReportRecipient = "";
    private String iconCatMapString = "";
    private HashMap iconCatMap = null; // holds an array of String[catId,icon.gif]-Arrays
    // 
    // Imported Engagement Types
    static final String TOPICTYPE_ENG_PROJECT = "t-331314";
    static final String TOPICTYPE_ENG_ZIELGRUPPE = "t-331319";
    static final String TOPICTYPE_ENG_TAETIGKEIT = "t-331323";
    static final String TOPICTYPE_ENG_EINSATZBEREICH = "t-331321";
    static final String TOPICTYPE_ENG_MERKMAL = "t-331325";
    static final String TOPICTYPE_ENG_BEZIRK = "t-331327";

    public TimedEngagementImporter () {
      as = (ApplicationService) TimerJobParameterHolder.getInstance().getParameters().get("as");
      cm = (CorporateMemory) TimerJobParameterHolder.getInstance().getParameters().get("cm");
      directives = (CorporateDirectives) TimerJobParameterHolder.getInstance().getParameters().get("directives");
      workspaceId = (String) TimerJobParameterHolder.getInstance().getParameters().get("engagementWorkspaceId");
      cityMapId = (String) TimerJobParameterHolder.getInstance().getParameters().get("engagementCityMapId");
      serviceUrl = (String) TimerJobParameterHolder.getInstance().getParameters().get("engagementServiceUrl");
    }

    public void execute(JobExecutionContext context) throws JobExecutionException {
      //
      BaseTopic settings = getImporterSettingsTopic();
      if (settings != null) {
        contentReportRecipient = as.getTopicProperty(settings, PROPERTY_IMPORT_CONTENT_REPORT);
        serviceReportRecipient = as.getTopicProperty(settings, PROPERTY_IMPORT_SERVICE_REPORT);
        iconCatMapString = as.getTopicProperty(settings, PROPERTY_ICONS_MAP);
        System.out.println("[EngagementJob] set to run for \"" + contentReportRecipient + "\" " +
                "errors are reported to " + serviceReportRecipient + " iconsMap available ? "
                + (iconCatMapString.length() > 0));
      } else { System.out.println("[EngagementJob] ERROR while loading settings for workspace " + workspaceId); }
      //
      if (iconCatMapString.length() > 0) {
        String[] lines = iconCatMapString.split("\n");
        iconCatMap = new HashMap(lines.length);
        for (int i = 0; i < lines.length; i++) {
          String[] pair = lines[i].split("\t");
          if (pair.length > 1) {
            if (!pair[1].equals("")) {
              iconCatMap.put(pair[0], pair[1]);
              // System.out.println("validIconConfig => " + iconCatMap.get(pair[0]));
              // ### TODO: Save Icons For Categories
            }
          }
        }
        // 
        System.out.println("[EngagementJob] number of items configured in iconCatMap: " + iconCatMap.size());
      }
      // work here
      String ehrenamtXml = sendGetRequest(serviceUrl, "");
      if (ehrenamtXml != null) {
        System.out.println("[EngagementJob] loaded data.. but is doing nothing for now...." + cityMapId);
        // delete former import
        clearCriterias();
        // clears workspace if new topics are available
        clearImport();
        // store and publish new topics
        Vector topicIds = parseAndStoreData(ehrenamtXml);
        if (settings != null && as.getTopicProperty(settings, PROPERTY_ICONS_MAP).length() == 0) {
          // just store a new configuration template for an iconMap if there's no config available
          as.setTopicProperty(settings, PROPERTY_ICONS_MAP, iconCatMapString);
        }
        publishData(topicIds);
        //
        as = null;
        cm = null;
        directives = null;
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
        System.out.println("[EngagementJob] is starting to gather coordinates and publish \""+topicIds.size()+"\" topics into : " + cm.getTopic(cityMapId, 1).getName());// + " " +
        //
        Vector unusable = new Vector(); // collection of GeoObjects without GPS Data
        for (int i = 0; i < topicIds.size(); i++) {
            GeoObjectTopic baseTopic = (GeoObjectTopic) as.getLiveTopic(((String)topicIds.get(i)), 1);
            BaseTopic addressTopic = baseTopic.getAddress();
            if (as.getTopicProperty(baseTopic.getID(), 1, PROPERTY_GPS_LAT).equals("")) {
                System.out.println("[EngagementJob] WARNING *** \" " + baseTopic.getName() + "\" / \""
                        + addressTopic.getName() + "\" is missing LAT... dropping placement in CityMap");
                unusable.add(baseTopic); //
            } else if (as.getTopicProperty(addressTopic, PROPERTY_STREET).equals("über Gute-Tat.de")) {
                unusable.add(baseTopic);
            } else {
                // System.out.println(">>>> creating ViewTopic for " + baseTopic.getName() + " (" + baseTopic.getID() + ")" );
                as.createViewTopic(cityMapId, 1, null, baseTopic.getID(), 1, 0, 0, false);
            }
            // System.out.println(">>> ready to publish geoObject " +baseTopic.getName()+" ("+baseTopic.getID()+")");
        }
        int validEntries = topicIds.size() - unusable.size();
        //
        System.out.println("[EngagementJob] stored " + validEntries + " in public cityMap \"" +as.getTopicProperty(cityMapId, 1, PROPERTY_WEB_ALIAS)+ "\"");
        System.out.println("[EngagementJob] skipped " + unusable.size() + " unlocatable \""+getWorkspaceGeoType(workspaceId).getName()+"\" ");
        as.setTopicProperty(cityMapId, 1, PROPERTY_LAST_UPDATED, "" + new Date().getTime() + "");
        sendNotificationEmail(unusable, false);
    }

    /** completely remove all topics created by the last import
     *  - delete from topicmap
     *  - email topic, person topic, webpage topic, and adress topic just, if not used by any other topic ...
     */
    private void clearImport() {
        /** DANGEROUS GROUNDS */
        // collect all topics which will possibly be removed..
        Vector allGeoObjects = cm.getTopics(getWorkspaceGeoType(workspaceId).getID());
        Vector allRelatedTopics = new Vector();
        System.out.println(" --- ");
        System.out.println("[EngagementJob] deleting " +allGeoObjects.size()+ " topics of type \"" +
                getWorkspaceGeoType(workspaceId).getID()+"\"");
        System.out.println(" ---");
        for (int i = 0; i < allGeoObjects.size(); i++) {
            BaseTopic baseTopic = (BaseTopic) allGeoObjects.get(i);
            Vector relatedTopics = as.getRelatedTopics(baseTopic.getID(), ASSOCTYPE_ASSOCIATION, 2);
            for (int j = 0; j < relatedTopics.size(); j++) {
                BaseTopic relatedTopic = (BaseTopic) relatedTopics.get(j);
                // to get all related topics except, the categories subtypes of tt-topictpy-crit.
                if (relatedTopic.getType().equals(TOPICTYPE_ENG_BEZIRK) ||
                        relatedTopic.getType().equals(TOPICTYPE_ENG_ZIELGRUPPE) ||
                        relatedTopic.getType().equals(TOPICTYPE_ENG_EINSATZBEREICH) ||
                        relatedTopic.getType().equals(TOPICTYPE_ENG_TAETIGKEIT) ||
                        relatedTopic.getType().equals(TOPICTYPE_ENG_MERKMAL)) {
                    //
                } else {
                    // store topicID in Vector for later removal
                    allRelatedTopics.add(relatedTopic);
                }
            }
            directiveDeletion(baseTopic.getID());
            //
        }
        /** Starting to delete topics which were directly associated to the GeoObjectTopic a.k.a.  base-topic */

        //
        System.out.println("[EngagementJob] is starting to delete "+allRelatedTopics.size()+" relatedTopics (just if one entry has no associations)---");
        for (int k = 0; k < allRelatedTopics.size(); k++) {
            BaseTopic relTopic = (BaseTopic) allRelatedTopics.get(k);
            //
            if (cm.getAssociationIDs(relTopic.getID(), 1).size() > 0) {
              // if there's any asscoiation left for this topic, forget about it's deletion
              // Note: the former topic (and with it all associations) will be already gone at this point,
              // see upper loop and line: 198
            } else {
               directiveDeletion(relTopic.getID());
            }
        }
    }

    /** remove criteria system from configured workspace */
    private void clearCriterias() {
        Vector taetigkeiten = cm.getTopics(TOPICTYPE_ENG_TAETIGKEIT);
        Vector einsatzbereiche = cm.getTopics(TOPICTYPE_ENG_EINSATZBEREICH);
        Vector merkmale = cm.getTopics(TOPICTYPE_ENG_MERKMAL);
        Vector zielgruppen = cm.getTopics(TOPICTYPE_ENG_ZIELGRUPPE);
        Vector bezirke = cm.getTopics(TOPICTYPE_ENG_BEZIRK);
        bezirke.addAll(taetigkeiten);
        bezirke.addAll(einsatzbereiche);
        bezirke.addAll(merkmale);
        bezirke.addAll(zielgruppen);
        //
        for (int i = 0; i < bezirke.size(); i++) {
            BaseTopic category = (BaseTopic) bezirke.get(i);
            CorporateDirectives newDirectives = as.deleteTopic(category.getID(), 1);
            newDirectives.updateCorporateMemory(as, null, null, null);
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
            // ok, it`s not known to CM..
        }
    }

    /**
     * just reads in THE xml String from ehrenamt.de and saves every single project into the kiezatlas cm
     *
     * @param xmlData
     * @return
     */
    private Vector parseAndStoreData(String xmlData) {
    Vector topicIds = new Vector();
    // Vector originCatIds = new Vector(); // filled and used for iconizing an imported categorical system
    //
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
        System.out.println("[EngagementJob] Import started at " + DeepaMehtaUtils.getTime() +" for a total no of " + amountOfProjects + " " + getWorkspaceGeoType(workspaceId).getName());
        // for(int s = 0; s < listOfProjects.getLength(); s++){
        Vector misspelledObjects = new Vector();
        // iterate over projects
        for(int p = 0; p < amountOfProjects; p++) {
            // fields of each project
            String projectName = "", originId = "", contactPerson = "", projectUrl = "", postcode = "", streetNr = "", bezirk = "", orgaName = "",
                    orgaWebsite = "", orgaContact = "", timeStamp = "";
            Vector merkmale = new Vector();
            Vector taetigkeiten = new Vector();
            Vector zielgruppen = new Vector();
            Vector einsatzbereiche = new Vector();
            NodeList projectDetail = listOfProjects.item(p).getChildNodes();
            // System.out.println(">> projectDetail has childs in number : " + projectDetail.getLength());
            for (int i = 0; i < projectDetail.getLength(); i++) {
                Node node = projectDetail.item(i);
                // System.out.println(">>> node is " + node.getNodeName() + " : " + node.getNodeValue() + " (" + node.getNodeType()+")");
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
                                        } else if (contentNode.getNodeName().equals("einsatzbereiche")) {
                                            // these elements describe the fields of work
                                            // System.out.println("> einsatzbereiche: "); // + anotherNode.getNodeValue());
                                            einsatzbereiche = readInCategories(anotherNode.getNodeValue());
                                        } else if (contentNode.getNodeName().equals("taetigkeit")) {
                                            // these elements describe the type of work
                                            // System.out.println("> taetigkeiten: "); //  + anotherNode.getNodeValue());
                                            taetigkeiten = readInCategories(anotherNode.getNodeValue());
                                        } else if (contentNode.getNodeName().equals("merkmale")) {
                                            // these elements describe the attributes of work
                                            // System.out.println("> merkmale: "); // + anotherNode.getNodeValue());
                                            merkmale = readInCategories(anotherNode.getNodeValue());
                                        } else {
                                            System.out.println("*** TimedEngagementImporter found unknown category for a POI while importing from ehrenamt.");
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
            // store information per project directly
            String topicID = saveProjectData(originId, projectName, contactPerson, projectUrl, postcode, streetNr, bezirk,
                        orgaName, orgaWebsite, orgaContact, timeStamp,
                    merkmale, taetigkeiten, zielgruppen, einsatzbereiche);
            //if (topicID == null) {
                //ignore topic, failed to safe data
              //  System.out.println("Missspelled Address Item: " + projectName);
                //misspelledObjects.add(projectName);
            //} else {
                // add topicID
                topicIds.add(topicID);
            //}
        }//end of for loop with p for projects
        System.out.println("[EngagementJob] stored data at " + DeepaMehtaUtils.getTime() +" for a total no of " + amountOfProjects + " " + getWorkspaceGeoType(workspaceId).getName());
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
     *  save one item "Project" from ehrenamt.de into the corporate memory with
     *  reusing existing addresses, webpages and persons
     *  building up the categorySystem by each item which is in some categories
     */
    private String saveProjectData(String originId, String projectName, String contactPerson, String projectUrl, String postcode, String streetNr, String bezirk, String orgaName,
            String orgaWebsite, String orgaContact, String timeStamp, Vector merkmale, Vector taetigkeiten, Vector zielgruppen, Vector einsatzbereiche) {
        String topicId = "";
        String address = streetNr + ", " + postcode + " " + bezirk;
        // storing data in corporate memory
        String geoTypeId = getWorkspaceGeoType(workspaceId).getID();
        LiveTopic geoObjectTopic = as.createLiveTopic(as.getNewTopicID(), geoTypeId, projectName, null);
        //
        as.setTopicProperty(geoObjectTopic, PROPERTY_NAME, projectName);
        as.setTopicProperty(geoObjectTopic, PROPERTY_PROJECT_ORGANISATION, orgaName);
        as.setTopicProperty(geoObjectTopic, PROPERTY_PROJECT_ORIGIN_ID, originId);
        as.setTopicProperty(geoObjectTopic, PROPERTY_PROJECT_LAST_MODIFIED, timeStamp);
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
            System.out.println("[WARNING] EngagementJob is using Property \"Stadt\" at GeoObjectTopic instead of City Topic");
            as.setTopicProperty(geoObjectTopic, PROPERTY_CITY, "Berlin");
        }
        // BaseTopic knownMailbox = cm.getTopic(TOPICTYPE_EMAIL_ADDRESS, orgaContact, 1);
        // check for address in cm just by streetname and housenumber ### !not yet by Postal Code
        BaseTopic knownAddress = cm.getTopic(TOPICTYPE_ADDRESS, streetNr, 1);
        // check for street in Adress !!
        if (knownAddress != null) {
            addressTopic = as.getLiveTopic(knownAddress);
        } else {
            addressTopic = as.createLiveTopic(as.getNewTopicID(), TOPICTYPE_ADDRESS, streetNr, null);
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
        geoObject.setGPSCoordinates(directives);
        // bezirk-label special move
        if (bezirk.startsWith("Berlin-") || bezirk.startsWith("berlin-")) {
            // slice a bit redundant information
            bezirk = bezirk.substring(7);
        }
        // create availalbe categories if not yet known to the cm and associate the item to all of its categories
        // one to one
        BaseTopic bezirkAlreadyKnown = cm.getTopic(TOPICTYPE_ENG_BEZIRK, bezirk, 1);
        if (bezirkAlreadyKnown != null) {
            // connect to known Bezirk
            // System.out.println(">> reusing BezirkTopic \""+bezirkAlreadyKnown.getName()+"\"");
            as.createLiveAssociation(as.getNewAssociationID(), ASSOCTYPE_ASSOCIATION, geoObjectTopic.getID(), bezirkAlreadyKnown.getID(), null, null);
        } else {
            // new Bezirk and connect to
            System.out.println(">> creating "+as.getLiveTopic(TOPICTYPE_ENG_BEZIRK, 1).getName()+"Topic \""+bezirk+"\"");
            LiveTopic bezirkTopic = as.createLiveTopic(as.getNewTopicID(), TOPICTYPE_ENG_BEZIRK, bezirk, null);
            as.createLiveAssociation(as.getNewAssociationID(), ASSOCTYPE_ASSOCIATION, geoObjectTopic.getID(), bezirkTopic.getID(), null, null);
            // create template for iconMap too
            if (iconCatMap == null) iconCatMapString += bezirkTopic.getName() + "\t\n";
            if (iconCatMap != null && iconCatMap.get(bezirkTopic.getName()) != null) {
                as.setTopicProperty(bezirkTopic, PROPERTY_ICON, iconCatMap.get(bezirkTopic.getName()).toString());
                System.out.println(" >>> found and set icon for .. " + bezirkTopic.getName() + " => " + iconCatMap.get(bezirkTopic.getName()));
            }
        }
        // one to many
        for (int i = 0; i < zielgruppen.size(); i++) {
            String zielgruppenName = (String) zielgruppen.get(i);
            BaseTopic knownZielgruppe = cm.getTopic(TOPICTYPE_ENG_ZIELGRUPPE, zielgruppenName, 1);
            if (knownZielgruppe != null) {
                // connect to known cat
                // System.out.println(">> reusing ZielgruppenTopic \""+catAlreadyKnown.getName()+"\"");
                as.createLiveAssociation(as.getNewAssociationID(), ASSOCTYPE_ASSOCIATION, geoObjectTopic.getID(), knownZielgruppe.getID(), null, null);
            } else {
                System.out.println(">> creating "+as.getLiveTopic(TOPICTYPE_ENG_ZIELGRUPPE, 1).getName()+"Topic \""+zielgruppenName+"\"");
                LiveTopic newZielgruppe = as.createLiveTopic(as.getNewTopicID(), TOPICTYPE_ENG_ZIELGRUPPE, zielgruppenName, null);
                as.createLiveAssociation(as.getNewAssociationID(), ASSOCTYPE_ASSOCIATION, geoObjectTopic.getID(), newZielgruppe.getID(), null, null);
                if (iconCatMap == null) iconCatMapString += newZielgruppe.getName() + "\t\n";
                if (iconCatMap != null && iconCatMap.get(newZielgruppe.getName()) != null) {
                    as.setTopicProperty(newZielgruppe, PROPERTY_ICON, iconCatMap.get(newZielgruppe.getName()).toString());
                    System.out.println(" >>> found and set icon for .. " + newZielgruppe.getName() + " => " + iconCatMap.get(newZielgruppe.getName()));
                }
            }
        }
        // one to many
        for (int i = 0; i < taetigkeiten.size(); i++) {
            String taetigkeitName = (String) taetigkeiten.get(i);
            BaseTopic knownTaetigkeit = cm.getTopic(TOPICTYPE_ENG_TAETIGKEIT, taetigkeitName, 1);
            if (knownTaetigkeit != null) {
                // connect to known cat
                // System.out.println(">> reusing ZielgruppenTopic \""+catAlreadyKnown.getName()+"\"");
                as.createLiveAssociation(as.getNewAssociationID(), ASSOCTYPE_ASSOCIATION, geoObjectTopic.getID(), knownTaetigkeit.getID(), null, null);
            } else {
                System.out.println(">> creating "+as.getLiveTopic(TOPICTYPE_ENG_TAETIGKEIT, 1).getName()+"Topic \""+taetigkeitName+"\"");
                LiveTopic newTaetigkeiten = as.createLiveTopic(as.getNewTopicID(), TOPICTYPE_ENG_TAETIGKEIT, taetigkeitName, null);
                as.createLiveAssociation(as.getNewAssociationID(), ASSOCTYPE_ASSOCIATION, geoObjectTopic.getID(), newTaetigkeiten.getID(), null, null);
                if (iconCatMap == null) iconCatMapString += newTaetigkeiten.getName() + "\t\n";
                if (iconCatMap != null && iconCatMap.get(newTaetigkeiten.getName()) != null) {
                    as.setTopicProperty(newTaetigkeiten, PROPERTY_ICON, iconCatMap.get(newTaetigkeiten.getName()).toString());
                    System.out.println(" >>> found and set icon for .. " + newTaetigkeiten.getName() + " => " + iconCatMap.get(newTaetigkeiten.getName()));
                }
            }
        }
        // one to many
        for (int i = 0; i < merkmale.size(); i++) {
            String merkmalsName = (String) merkmale.get(i);
            BaseTopic merkmalKnown = cm.getTopic(TOPICTYPE_ENG_MERKMAL, merkmalsName, 1);
            if (merkmalKnown != null) {
                // connect to known cat
                // System.out.println(">> reusing ZielgruppenTopic \""+catAlreadyKnown.getName()+"\"");
                as.createLiveAssociation(as.getNewAssociationID(), ASSOCTYPE_ASSOCIATION, geoObjectTopic.getID(), merkmalKnown.getID(), null, null);
            } else {
                System.out.println(">> creating "+as.getLiveTopic(TOPICTYPE_ENG_MERKMAL, 1).getName()+"Topic \""+merkmalsName+"\"");
                LiveTopic newMerkmal = as.createLiveTopic(as.getNewTopicID(), TOPICTYPE_ENG_MERKMAL, merkmalsName, null);
                as.createLiveAssociation(as.getNewAssociationID(), ASSOCTYPE_ASSOCIATION, geoObjectTopic.getID(), newMerkmal.getID(), null, null);
                if (iconCatMap == null) iconCatMapString += newMerkmal.getName() + "\t\n";
                if (iconCatMap != null && iconCatMap.get(newMerkmal.getName()) != null) {
                    as.setTopicProperty(newMerkmal, PROPERTY_ICON, iconCatMap.get(newMerkmal.getName()).toString());
                    System.out.println(" >>> found and set icon for .. " + newMerkmal.getName() + " => " + iconCatMap.get(newMerkmal.getName()));
                }
            }
        }
        // one to many
        for (int i = 0; i < einsatzbereiche.size(); i++) {
            String einsatzbereichsName = (String) einsatzbereiche.get(i);
            BaseTopic knownEinsatzbereich = cm.getTopic(TOPICTYPE_ENG_EINSATZBEREICH, einsatzbereichsName, 1);
            if (knownEinsatzbereich != null) {
                // connect to known cat
                // System.out.println(">> reusing ZielgruppenTopic \""+catAlreadyKnown.getName()+"\"");
                as.createLiveAssociation(as.getNewAssociationID(), ASSOCTYPE_ASSOCIATION, geoObjectTopic.getID(), knownEinsatzbereich.getID(), null, null);
            } else {
                System.out.println(">> creating "+as.getLiveTopic(TOPICTYPE_ENG_EINSATZBEREICH, 1).getName()+"Topic \""+einsatzbereichsName+"\"");
                LiveTopic newEinsatzbereich = as.createLiveTopic(as.getNewTopicID(), TOPICTYPE_ENG_EINSATZBEREICH, einsatzbereichsName, null);
                as.createLiveAssociation(as.getNewAssociationID(), ASSOCTYPE_ASSOCIATION, geoObjectTopic.getID(), newEinsatzbereich.getID(), null, null);
                if (iconCatMap == null) iconCatMapString += newEinsatzbereich.getName() + "\t\n";
                if (iconCatMap != null && iconCatMap.get(newEinsatzbereich.getName()) != null) {
                    as.setTopicProperty(newEinsatzbereich, PROPERTY_ICON, iconCatMap.get(newEinsatzbereich.getName()).toString());
                    System.out.println(" >>> found and set icon for .. " + newEinsatzbereich.getName() + " => " + iconCatMap.get(newEinsatzbereich.getName()));
                }
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
                System.out.println("[EngagementJob] sending request to: " + url.toURI().toURL().toString());
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
                DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
                Document doc = docBuilder.parse(new InputSource(new StringReader(result)));
                System.out.println("[EngagementJob] finished loading and parsing data from " + url);
            } catch(UnknownHostException uke) {
                System.out.println("*** EngagementJob Thread could not load the xml data to import from " + endpoint + " message is: " + uke.getMessage());
                return null;
                // done();
            } catch (SAXParseException saxp) {
                System.out.println ("** Parsing error" + ", line "
                    + saxp.getLineNumber () + ", column " + saxp.getColumnNumber() + ", message: " + saxp.getMessage());
                System.out.println("** dataValue: " +result.substring(saxp.getColumnNumber()-100, saxp.getColumnNumber()+100));
                System.out.println("*** EngagementJob is skipping the import for today!");
                return null;
            } catch (Exception ex) {
                System.out.println("*** EngagementJob has encountered the following problem: " + ex.getMessage());
                return null;
            }
        }
        return result;
    }

    private Vector getGeoObjectInformation(String workspaceId) {
        BaseTopic geoType = getWorkspaceGeoType(workspaceId);
        if (geoType == null) {
            System.out.println(">> Workspace ("+workspaceId+") is not configured properly");
            return new Vector();
        }
        return cm.getTopics(geoType.getID());
    }

    // --- copy of BrowseServlet

    private void sendNotificationEmail(Vector unusableProjects, boolean severeFlag) {
      try {
        // "to"
        String to = contentReportRecipient;
        if (severeFlag) {
          to = serviceReportRecipient;
        }
        // GeoObjectTopic inst = (GeoObjectTopic) as.getLiveTopic(instID, 1);
        // "from"
        String from = as.getEmailAddress("t-rootuser");		// ###
        if (from == null || from.equals("")) {
          throw new DeepaMehtaException("email address of root user is unknown");
        }
        // "subject"
        String subject = "Ehrenamtsatlas: folgende Projekte haben einen fehlerhaften Addresseintrag";
        StringBuffer entries = new StringBuffer();
        entries.append("------------------------------\r");
        for (int i = 0; i < unusableProjects.size(); i++) {
            GeoObjectTopic entry = (GeoObjectTopic) unusableProjects.get(i);
            if (!entry.getAddress().getName().equals("über Gute-Tat.de")) {
              entries.append("Projekt: ");
              entries.append(entry.getName());
              entries.append(" mit Adresseintrag: ");
              entries.append(entry.getAddress().getName());
              entries.append("\r");
            }
        }
        entries.append("------------------------------\r");
        // "body"
        String body = "Dies ist eine automatische Benachrichtigung erstellt von www.kiezatlas.de\r\r" +
          "Folgende Ehrenamtsprojekte konnten aufgrund eines fehlerhaften Adresseintrags nicht korrekt verortet werden:\r\r" +
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
