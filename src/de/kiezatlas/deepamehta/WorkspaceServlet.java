package de.kiezatlas.deepamehta;

import de.deepamehta.AmbiguousSemanticException;
import de.kiezatlas.deepamehta.topics.GeoObjectTopic;
//
import de.deepamehta.BaseTopic;
import de.deepamehta.DeepaMehtaException;
import de.deepamehta.OrderedItem;
import de.deepamehta.PropertyDefinition;
import de.deepamehta.service.ApplicationService;
import de.deepamehta.service.CorporateDirectives;
import de.deepamehta.service.Session;
import de.deepamehta.service.web.DeepaMehtaServlet;
import de.deepamehta.service.web.RequestParameter;
import de.deepamehta.service.web.WebSession;
//
import de.deepamehta.topics.EmailTopic;
import de.deepamehta.topics.TypeTopic;
import javax.servlet.ServletException;
//
import java.io.File;
import java.util.*;
//
import org.apache.commons.fileupload.FileItem;



/**
 * Kiezatlas 1.6.8.3<br>
 * Requires DeepaMehta 2.0b8.
 * <p>
 * Last change: 02.01.2012<br>
 * Malte Rei&szlig;ig<br>
 * mre@deepamehta.de
 */
public class WorkspaceServlet extends DeepaMehtaServlet implements KiezAtlas {

    static final String ASSOCTYPE_SUBMITTER = "t-822236";

    protected String performAction(String action, RequestParameter params, Session session, CorporateDirectives directives)
            throws ServletException {
        if (action == null) {
            try {
                String pathInfo = params.getPathInfo();
                // error check
                if (pathInfo == null || pathInfo.length() == 1) {
                    throw new DeepaMehtaException("Fehler in URL");
                }
                //
                String alias = pathInfo.substring(1);
                Hashtable aliasProperty = new Hashtable();
                aliasProperty.put(PROPERTY_WORKSPACE_ALIAS, alias);
                BaseTopic workspace = as.getTopic(TOPICTYPE_WORKSPACE, aliasProperty, null, directives);
                if (workspace != null) {
                    session.setAttribute("workspace", workspace);
                } else {
                    session.setAttribute("html", "A Workspace with this web-alias could not be found.");
                    return PAGE_WORKSPACE_ERROR;
                }
                //
                return PAGE_WORKSPACE_LOGIN;
            } catch (DeepaMehtaException e) {
                System.out.println("*** WorkspaceServlet.performAction(): " + e);
                session.setAttribute("error", e.getMessage());
                return PAGE_WORKSPACE_ERROR;
            }
        }
        // session timeout?
        if (getWorkspaceObject(session) == null) {	// ### doesn't return null but throws exception!
            System.out.println("*** Session Expired ***");
			session.setAttribute("error", "Timeout: Kiezatlas wurde mehr als " +
				((WebSession) session).session.getMaxInactiveInterval() / 60 + " Minuten nicht benutzt");
            return PAGE_WORKSPACE_ERROR;
        }
        //
        if (action.equals(ACTION_TRY_LOGIN)) {
            // GeoObjectTopic geo = getGeoObject(session);
            // BaseTopic workspace = getWorkspaceTopicByAlias();
            String password = params.getValue(PROPERTY_PASSWORD);
            String username = params.getValue(PROPERTY_USERNAME);
            // ### check if user credentials are allowed..
            Hashtable userProperty = new Hashtable();
            userProperty.put(PROPERTY_USERNAME, username);
            userProperty.put(PROPERTY_PASSWORD, password);
            BaseTopic user = null;
            try {
                user = as.getTopic(TOPICTYPE_USER, userProperty, null, directives);
                if (!user.getName().equals(username)) {
                    System.out.println("*** ERROR: usernames do not match/ no username given for .." + user.getName());
                    session.setAttribute("error", "Login incorrect");
                    return PAGE_WORKSPACE_ERROR;
                } else if (!as.getTopicProperty(user, PROPERTY_PASSWORD).equals(password)) {
                    System.out.println("*** ERROR: passwords do not match/ no passsword given for .." + user.getName());
                    session.setAttribute("error", "Login incorrect");
                    return PAGE_WORKSPACE_ERROR;
                }
            } catch (DeepaMehtaException ex) {
                System.out.println("User is NOT available or credentials are not correct.. ");
                session.setAttribute("error", "Login incorrect");
                return PAGE_WORKSPACE_ERROR;
            }
            // ### and is user related to this workpsace in the role of submitter
            if (user != null) {
                session.setAttribute("user", user);
                // ### generate form for the GeoObject-Topictype of this workspace..
                // GeoObjectTopic topic = as.createTopic(, "");
                // load current workspace..
                BaseTopic ws = (BaseTopic) session.getAttribute("workspace");
                // load current workspace geo-topictype
                BaseTopic geoType = getWorkspaceGeoType(ws.getID());
                // load all citymaps published in this workspace...BaseTopic geoType = getWorkspaceGeoType(ws.getID())
                Vector cityMaps = as.getRelatedTopics(ws.getID(), ASSOCTYPE_PUBLISHING, TOPICTYPE_CITYMAP, 1, true);
                BaseTopic wsMap = as.getRelatedTopic(ws.getID(), ASSOCTYPE_AGGREGATION, TOPICTYPE_TOPICMAP, 2, true);
                Vector publishedMaps = cm.getViewTopics(wsMap.getID(), 1, TOPICTYPE_CITYMAP);
                if (publishedMaps.size() >= 1) {
                    session.setAttribute("cityMaps", publishedMaps);
                    System.out.println("DEBUG: published maps are: " + publishedMaps);
                }
                //
                System.out.println("DEBUG: formType: " + geoType.getID() + " / " + geoType.getType() + " ws: " + ws.getName());
                session.setAttribute("instTypeId", geoType.getID());
                return PAGE_WORKSPACE_FORM;
            } else {
                System.out.println("User is NOT available or credentials are not correct.. ");
            }
            // String geoPw = as.getTopicProperty(geo, PROPERTY_PASSWORD);
            // return password.equals(geoPw) ? PAGE_WORKSPACE_FORM : PAGE_WORKSPACE_LOGIN;
            return PAGE_WORKSPACE_LOGIN;
            //
        } else if (action.equals(ACTION_SHOW_WORKSPACE_FORM)) {
            // create properly a new item
            BaseTopic ws = (BaseTopic) session.getAttribute("workspace");
            // load current workspace geo-topictype
            BaseTopic geoType = getWorkspaceGeoType(ws.getID());
            session.setAttribute("instTypeId", geoType.getID());
            return PAGE_WORKSPACE_FORM;
            //
        } else if (action.equals(ACTION_CREATE_GEO)) {
            // load all selected citymap-ids..
            String[] cityMaps = params.getParameterValues("cityMap");
            // --- notification ---
            // Note: the check for warnings is performed before the form input is processed
            // because the processing (createTopic()) eat the parameters up.
            // ### checkForWarnings(params, session, directives);
            // --- place in city map ---
            // Note: the geo object is placed in all city maps before it is actually created.
            // This way YADE-based autopositioning can perform through geo object's propertiesChanged() hook.
            // ### cm.createViewTopic(cityMap.getID(), 1, VIEWMODE_USE, geoObjectID, 1, 0, 0, false);	// performExistenceCheck=false
            // --- create geo object ---
            // Note: timestamp, password, and geometry-lock are initialized through geo object's evoke() hook
            BaseTopic ws = (BaseTopic) session.getAttribute("workspace");
            BaseTopic geoType = getWorkspaceGeoType(ws.getID());
            String geoObjectId = "";
            try {
                geoObjectId = createTopic(geoType.getID(), params, session, directives); // skip citymap
                // equip our new object with a random webalias...
                as.setTopicProperty(geoObjectId, 1, PROPERTY_WEB_ALIAS, UUID.randomUUID().toString());
            } catch (DeepaMehtaException ex) {
                System.out.println("*** Error catched along... should redirect to form with UDPATE_GEO..");
            }
            //
            for (int i = 0; i < cityMaps.length; i++) {
                String cityMapId = cityMaps[i];
                System.out.println("DEBUG: cityMaps to publish this object to.. " + cityMapId);
                // --- place in city map ---
                // Note: the geo object is placed in city map before it is actually created.
                // This way YADE-based autopositioning can perform through geo object's propertiesChanged() hook.
                cm.createViewTopic(cityMapId, 1, VIEWMODE_USE, geoObjectId, 1, 0, 0, false);	// performExistenceCheck=false
            }
            // --- get geo object ---
            setGeoObject(cm.getTopic(geoObjectId, 1), session);
            GeoObjectTopic geo = getGeoObject(session);
            setGPSCoordinates(geo, directives); //{ // loads gps coordinates
            // --- store image ---
            // EditServlet.writeFiles(params.getUploads(), geo.getImage(), as);
            // --- possibly create notification to logged in user if entry was created
            BaseTopic user = (BaseTopic) session.getAttribute("user");
            String userMailbox = "";
            if (user != null && as.getMailboxURL(user.getID()) != null) {
                userMailbox = as.getMailboxURL(user.getID());
            }
            // double check..
            if (userMailbox.equals("")) { // cannot be null anymore..
                BaseTopic emailTopic = null;
                try {
                    emailTopic = (BaseTopic) as.getRelatedTopic(user.getID(), ASSOCTYPE_ASSOCIATION, 2, true);
                } catch (AmbiguousSemanticException aex) {
                    //
                    emailTopic = aex.getDefaultTopic();
                }
                if (emailTopic != null) {
                    userMailbox = emailTopic.getName();
                }
            }
            sendNotificationEmail(userMailbox, geoObjectId, ws);
            return PAGE_WORKSPACE_OBJECT_ADDED;
        } else if (action.equals(ACTION_UPDATE_GEO)) {
            GeoObjectTopic geo = getGeoObject(session);
            // --- update geo object ---
            updateTopic(geo.getType(), params, session, directives);
            // --- store image ---
            writeFiles(params.getUploads(), geo.getImage(), as);
            //
            return PAGE_WORKSPACE_OBJECT_ADDED;
            //
        } else {
            return super.performAction(action, params, session, directives);
        }
    }

    protected void preparePage(String page, RequestParameter params, Session session, CorporateDirectives directives) {
        if (page.equals(PAGE_GEO_HOME)) {
            // String geoID = getGeoObject(session).getID();
            // TopicBean topicBean = as.createTopicBean(geoID, 1);
            // session.setAttribute("topicBean", topicBean);
            // updateImagefile(session);
        }
    }



    // *****************
    // *** Utilities ***
    // *****************

    private void sendNotificationEmail(String mailbox, String topicId, BaseTopic workspace) {
        try {
            GeoObjectTopic inst = (GeoObjectTopic) as.getLiveTopic(topicId, 1);
            BaseTopic geoType = getWorkspaceGeoType(workspace.getID());
            TypeTopic topicType = (TypeTopic) as.getLiveTopic(geoType);
            // "from"
            String from = as.getEmailAddress("t-rootuser");		// ###
            if (from == null || from.equals("")) {
                throw new DeepaMehtaException("email address of root user is unknown");
            }
            // "to"
            BaseTopic email = inst.getEmail();
            String to = "";
            if (email == null || email.getName().equals("")) {
                // use given mailbox
                System.out.println("User MAILBOX : " + mailbox);
                to = mailbox;
                // throw new DeepaMehtaException("email address of \"" + inst.getName() + "\" is unknown");
            } else {
                // use institution mailbox..
                to = email.getName();
            }
            // "subject"
            String subject = "Kiezatlas: Neuer Datensatz";
            // "body"
            // Hashtable props = formType.getProperties();
            StringBuffer topicBody = new StringBuffer("");
            try {
                Vector hiddenProps = as.triggerHiddenProperties(topicType);		// may return null
                Enumeration items = topicType.getDefinition().elements();
                while (items.hasMoreElements()) {
                    OrderedItem item = (OrderedItem) items.nextElement();
                    if (item instanceof PropertyDefinition) {
                        PropertyDefinition propDef = (PropertyDefinition) item;
                        String propName = propDef.getPropertyName();
                        String propLabel = as.triggerPropertyLabel(propDef, topicType, topicType.getID());
                        String propValue = topicId != null ? as.getTopicProperty(topicId, 1, propName) : "";
                        // Note: the hook returns _parameter names_ and the page delivers _field labels_
                        // if (hiddenProps == null || !hiddenProps.contains(propName)) {
                        // }
                        if (propLabel.equals("LONG") || propLabel.equals("LAT") || propLabel.equals("Password") || propLabel.equals("Name")
                                || propLabel.equals("Owner ID") || propLabel.equals("Locked Geometry") || propLabel.equals("YADE x") || propLabel.equals("YADE y")
                                || propLabel.equals("Stichworte") || propLabel.equals("Description") || propLabel.equals("Icon")) {
                            // do not append
                        } else {
                            topicBody.append("" + propLabel + ":" + propValue + "\r");
                        }
                    }
                }
            } catch (NullPointerException ex) {
                System.out.println("ERROR: sendNotificationMail.. " + topicBody.toString() + " message: \r-------"
                        + "\r" + ex.getMessage());
            }
            Hashtable topic = cm.getTopicData(topicId, 1);
            String body = "Dies ist eine automatische Benachrichtigung von www.kiezatlas.de\r\r"
                    + "Im Workspace \"" + workspace.getName() + "\" wurde in ihrem Namen der folgende Datensatz neu eingetragen:\r\r"
                    + "------------------------------\r"
                    + topic.get("Trägerident") + "\r\r" + topicBody.toString()
                    + // "Autor: " + topic.get(PROPERTY_) + "\r" +
                    // "Email: " + topic.get(PROPERTY_) + "\r" +
                    // "Datum: " + topic.get(PROPERTY_) + "\r" +
                    // "Uhrzeit: " + topic.get(PROPERTY_COMMENT_TIME) + "\r" +
                    "------------------------------\r\r"
                    + "Im Falle des Mißbrauchs: Kontakieren Sie bitte den Kiez-Administrator vom "
                    + workspace.getName() + " Workspace.\r"
                    + "Die Kontakt-Email ist Thomas.Moser@ba-ts.berlin.de \r"
                    + "\r\r"
                    + "Mit freundlichen Grüßen\r"
                    + "ihr Kiezatlas-Team";
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
     * returns null if there is no topictype assigned to the given workspace, or is topictype is not a subtype of
     * "GeoObjectTopic"
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

    /**
     * @see	#performAction
     * @see	ListServlet#performAction
     */
    static Vector writeFiles(Vector fileItems, BaseTopic topic, ApplicationService as) {
        Vector fileNames = new Vector();
        for (int i = 0; i < fileItems.size(); i++) {
            try {
                System.out.println(">>> WorkspaceServlet.writeFiles(): " + fileItems.size() + " files uploaded, writing fileItem: " + i);
                FileItem item = (FileItem) fileItems.get(i);
                String propName = getFileChooserFieldName(item);
                String fileext = getFileExtension(item.getName());	// ### explorer includes entire path
                String filename = getFilename(item.getName());	// ### explorer includes entire path
                System.out.println("  > filename=\"" + filename + "\" extension=\"" + fileext + "\"");
                String path = "/home/jrichter/deepamehta/install/client/documents/";	// ### hardcoded
                if (fileext.equalsIgnoreCase("png") || fileext.equalsIgnoreCase("jpg") || fileext.equalsIgnoreCase("gif")) {
                    path = "/home/jrichter/deepamehta/install/client/images/";	// ### hardcoded
                }
                File fileToWrite = new File(path + filename);
                // find new filename if already exists
                int copyCount = 0;
                String newFilename = null;
                int pos = filename.lastIndexOf('.');
                while (fileToWrite.exists()) {
                    copyCount++;
                    newFilename = filename.substring(0, pos) + "-" + copyCount + filename.substring(pos);
                    fileToWrite = new File(path + newFilename);
                    System.out.println("  > file already exists, try \"" + newFilename + "\"");
                }
                //
                item.write(fileToWrite);
                // ### item.write(new File(as.getCorporateWebBaseURL().substring(5) + "images/" + filename));
                System.out.println("  > file \"" + fileToWrite + "\" written successfully");
                if (copyCount > 0) {
                    if (newFilename != null) {
                        as.setTopicProperty(topic, propName, newFilename);
                    }
                    fileNames.add(newFilename);
                } else {
                    fileNames.add(filename);
                }
            } catch (Exception e) {
                System.out.println("*** WorkspaceServlet.writeFiles(): " + e);
            }
        }
        return fileNames;
    }

    static String getFileChooserFieldName(FileItem item) {
        String fieldName = item.getFieldName();
        int pos = fieldName.lastIndexOf(":");
        return pos != -1 ? fieldName.substring(pos + 1) : fieldName;
    }

    // ###
    static String getFilename(String path) {
        int pos = path.lastIndexOf('\\');
        return pos != -1 ? path.substring(pos + 1) : path;
    }

    static String getFileExtension(String path) {
        int pos = path.lastIndexOf('.');
        return pos != -1 ? path.substring(pos + 1) : "";
    }



    // *************************
    // *** Session Utilities ***
    // *************************

    private void setWorkspaceObject(BaseTopic workspace, Session session) {
        session.setAttribute("workspace", workspace);
        System.out.println("> \"workspace\" stored in session: " + workspace);
    }

    private BaseTopic getWorkspaceObject(Session session) {
        return ((BaseTopic) session.getAttribute("workspace"));
    }

    private GeoObjectTopic getGeoObject(Session session) {
        return (GeoObjectTopic) as.getLiveTopic((BaseTopic) session.getAttribute("geo"));
    }

    private void setGeoObject(BaseTopic geo, Session session) {
        session.setAttribute("geo", geo);
        System.out.println("> \"geo\" stored in session: " + geo);
    }

    /**
     * Connects to Google's GeoCoder and gets for the Ojbects resp. Address the GPS Coordinates and saves them into the
     * Corporate Memory
     *
     * @param geo
     * @param directives
     */
    private void setGPSCoordinates(GeoObjectTopic geo, CorporateDirectives directives) {
        geo.setGPSCoordinates(directives);
    }

    // ---

    // ### compare to class Institution
    private void updateImagefile(Session session) {
        String imageURL = null;
        BaseTopic image = getGeoObject(session).getImage();
        if (image != null) {
            String imagefile = as.getTopicProperty(image, PROPERTY_FILE);
            if (imagefile.length() > 0) {
                // yes, users store pdf or docs as object's logo image into the system,
                // we have to adjust the path so that they can see what they're doing
                if (imagefile.indexOf("png") != -1 || imagefile.indexOf("jpg") != -1 || imagefile.indexOf("gif") != -1
                        || imagefile.indexOf("PNG") != -1 || imagefile.indexOf("JPG") != -1 || imagefile.indexOf("GIF") != -1) {
                    imageURL = as.getCorporateWebBaseURL() + FILESERVER_IMAGES_PATH + imagefile;
                } else {
                    imageURL = as.getCorporateWebBaseURL() + FILESERVER_DOCUMENTS_PATH + imagefile;
                }
            }
        }
        session.setAttribute("imagefile", imageURL);
        System.out.println("> \"imagefile\" stored in session: " + imageURL);
    }
}
