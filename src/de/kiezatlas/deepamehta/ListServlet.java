package de.kiezatlas.deepamehta;

import de.kiezatlas.deepamehta.topics.CityMapTopic;
import de.kiezatlas.deepamehta.topics.GeoObjectTopic;
//
import de.deepamehta.BaseTopic;
import de.deepamehta.DeepaMehtaException;
import de.deepamehta.AmbiguousSemanticException;
import de.deepamehta.service.CorporateDirectives;
import de.deepamehta.service.Session;
import de.deepamehta.service.TopicBean;
import de.deepamehta.service.TopicBeanField;
import de.deepamehta.service.web.DeepaMehtaServlet;
import de.deepamehta.service.web.RequestParameter;
import de.deepamehta.topics.TypeTopic;
import de.deepamehta.util.DeepaMehtaUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
//
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.TimeZone;
import java.util.Vector;

import javax.servlet.ServletException;

import org.apache.commons.fileupload.FileItem;



/**
 * Kiezatlas 1.6.2<br>
 * Requires DeepaMehta rev. 369
 * <p>
 * Last change: 06.12.2009<br>
 * J&ouml;rg Richter / Malte Rei&szlig;ig<br>
 * jri@deepamehta.de / mre@deepamehta.de
 */
public class ListServlet extends DeepaMehtaServlet implements KiezAtlas {

    private Thread worker = null;

    /**
     * With introducing a SlimList this controller gained a state which is maintain in the session
     * and that hurts to even look at. This is gonig to be nice in the near future, where no
     * tight time contstraints are a matter of fact.
     *
     * @param action
     * @param params
     * @param session
     * @param directives
     * @return
     * @throws javax.servlet.ServletException
     */
    protected String performAction(String action, RequestParameter params, Session session, CorporateDirectives directives)
            throws ServletException {
        if (action == null) {
            return PAGE_LIST_LOGIN;
        } else if (action.equals(ACTION_TRY_LOGIN)) {
            String username = params.getValue("username");
            String password = params.getValue("password");
            if (as.loginCheck(username, password)) {
                BaseTopic user = cm.getTopic(TOPICTYPE_USER, username, 1);
                setUser(user, session);
                return PAGE_LIST_HOME;
            } else {
                return PAGE_LIST_LOGIN;
            }
        } else if (action.equals(ACTION_SHOW_INSTITUTIONS)) {
            if (session.getAttribute("membership") == null) {
                session.setAttribute("membership", "Affiliated");
            }
            BaseTopic cityMap = cm.getTopic(params.getValue("cityMapID"), 1);
            BaseTopic workspace = cm.getTopic(params.getValue("workspaceID"), 1);
            setWorkspaceTopic(workspace, session);
            String instTypeID = ((CityMapTopic) as.getLiveTopic(cityMap)).getInstitutionType().getID();
            setCityMap(cityMap, session);
            setInstTypeID(instTypeID, session);
            // -- initialize filter and search attributes with "null"
            session.setAttribute("sortField", null);
            session.setAttribute("filterField", null);
            setUseCache(Boolean.FALSE, session);
            session.setAttribute("isSlim", "false");
            return PAGE_LIST;
        } else if (action.equals(ACTION_SHOW_INSTITUTIONS_SLIM)) {
            BaseTopic cityMap = cm.getTopic(params.getValue("cityMapID"), 1);
            BaseTopic workspace = cm.getTopic(params.getValue("workspaceID"), 1);
            setWorkspaceTopic(workspace, session);
            String instTypeID = ((CityMapTopic) as.getLiveTopic(cityMap)).getInstitutionType().getID();
            setCityMap(cityMap, session);
            setInstTypeID(instTypeID, session);
            // -- initialize filter and search attributes with "null"
            session.setAttribute("sortField", null);
            session.setAttribute("isSlim", "true");
            session.setAttribute("filterField", null);
            setUseCache(Boolean.FALSE, session);
            return PAGE_SLIM_LIST;
        } else if (action.equals(ACTION_SHOW_GEO_FORM)) {
            String geoObjectID = params.getValue("id");
            setGeoObject(cm.getTopic(geoObjectID, 1), session);
            return PAGE_GEO_ADMIN_FORM;
        } else if (action.equals(ACTION_UPDATE_GEO)) {
            GeoObjectTopic geo = getGeoObject(session);
            CityMapTopic cityMap = getCityMap(session);
            // --- notification ---
            // Note: the check for warnings is performed before the form input is processed
            // because the processing (updateTopic()) eat the parameters up.
            checkForWarnings(params, session, directives);
            // --- update geo object ---
            // Note: the timestamp is updated through geo object's propertiesChanged() hook
            updateTopic(geo.getType(), params, session, directives, cityMap.getID(), VIEWMODE_USE);
            // --- store image / files---
            for (int a = 0; a < params.getUploads().size(); a++) {
                FileItem f = (FileItem) params.getUploads().get(a);
                System.out.println("*** ListServlet. uploaded files are " + EditServlet.getFilename(f.getName()));
            }
            EditServlet.writeFiles(params.getUploads(), geo.getImage(), as);
            // --- update mirror topics (if remotely already known/present)
            if (geo.isPartOfMigratedWorkspaces()) {
		String mapTopicId = cityMap.getID();
		String mapAlias = as.getTopicProperty(cityMap, PROPERTY_WEB_ALIAS);
		BaseTopic workspace = getWorkspaceTopic(session);
		System.out.println("INFO: Remote Geo Object during EDIT in LIST Servlet in cityMap: " + mapAlias + ", WS: " + workspace.getID());
		geo.synchronizeGeoObject(mapAlias, workspace.getName());
	    }
            //
            setGPSCoordinates(geo, directives); // will should load coordinates if coordinate values are empty
            if (session.getAttribute("isSlim").equals("true")) {
                setUseCache(Boolean.FALSE, session);	// slim list don't use the cache
                return PAGE_SLIM_LIST;
            } else {
                // make sure that element in cache is updated
                updateTopicInCache(geo, session);
                setUseCache(Boolean.TRUE, session);	// re-filtering and -sorting is handled in preparePage with cached topics now
                return PAGE_LIST;
            }
        } else if (action.equals(ACTION_SHOW_EMPTY_GEO_FORM)) {
            return PAGE_GEO_EMPTY_FORM;
        } else if (action.equals(ACTION_CREATE_GEO)) {
            String geoObjectID = as.getNewTopicID();
            CityMapTopic cityMap = getCityMap(session);
            // --- notification ---
            // Note: the check for warnings is performed before the form input is processed
            // because the processing (createTopic()) eat the parameters up.
            checkForWarnings(params, session, directives);
            // --- place in city map ---
            // Note: the geo object is placed in city map before it is actually created.
            // This way YADE-based autopositioning can perform through geo object's propertiesChanged() hook.
            cm.createViewTopic(cityMap.getID(), 1, VIEWMODE_USE, geoObjectID, 1, 0, 0, false);	// performExistenceCheck=false
            // --- create geo object ---
            // Note: timestamp, password, and geometry-lock are initialized through geo object's evoke() hook
            createTopic(getInstTypeID(session), params, session, directives, cityMap.getID(), geoObjectID);
            // --- get geo object ---
            setGeoObject(cm.getTopic(geoObjectID, 1), session);
            GeoObjectTopic geo = getGeoObject(session);
            // --- update GPS coordinates ---
            setGPSCoordinates(geo, directives); // will load geo-coordinates if values are empty
            // --- store image ---
            EditServlet.writeFiles(params.getUploads(), geo.getImage(), as);
            // --- check if new topic is to be synced remotely too
            if (geo.isPartOfMigratedWorkspaces()) {
                // --- create mirror topic
                String mapTopicId = cityMap.getID();
                String mapAlias = as.getTopicProperty(cityMap, PROPERTY_WEB_ALIAS);
                BaseTopic workspace = getWorkspaceTopic(session);
                System.out.println("INFO: Creating new topic remotely in \"" + mapAlias + "\" (\"" + workspace.getName() + "\") LIST Servlet");
                geo.postRemoteGeoObject(mapTopicId, mapAlias, workspace.getID());
            }
            //
            if (session.getAttribute("isSlim").equals("true")) {
                setUseCache(Boolean.FALSE, session);	// slim list don't uses the cache
                return PAGE_SLIM_LIST;
            } else {
                setUseCache(Boolean.TRUE, session);	// re-filtering and -sorting is handled in preparePage with fresh topics now
                inserTopicIntoCache(geo, session);
                return PAGE_LIST;
            }
        } else if (action.equals(ACTION_GO_HOME)) {
            setFilterField(null, session);
            setFilterText(null, session);
            setSortByField(null, session);
            session.setAttribute("isSlim", "false");
            return PAGE_LIST_HOME;
        } else if (action.equals(ACTION_SORT_BY)) {
            // just sort currently rendered list of topics
            Vector topicBeans = getListedTopics(session);
            String sortBy = params.getParameter("sortField");
            sortBeans(topicBeans, sortBy);
            setListedTopics(topicBeans, session);
            setSortByField(sortBy, session);
            setUseCache(Boolean.TRUE, session);
            // not used yet
            if (session.getAttribute("isSlim").equals("true")) {
                return PAGE_SLIM_LIST;
            }
            return PAGE_LIST;
        } else if (action.equals(ACTION_FILTER)) {
            // always filter on the full list of cached topics
            Vector cachedTopics = getCachedTopicList(session);
            String filterField = params.getParameter("filterField");
            // resulting topics can be either topicbeans or basetopics
            Vector topics;
            if (filterField != null) {
                String filterText = params.getParameter("filterText");
                if (session.getAttribute("isSlim").equals("true")) {
                    topics = filterTopicsByName(cachedTopics, filterField, filterText);
                    setListedTopics(topics, session);
                    setFilterField(filterField, session);
                    setFilterText(filterText, session);
                    setUseCache(Boolean.TRUE, session);
                    return PAGE_SLIM_LIST;
                } else {
                    topics = filterBeansByField(cachedTopics, filterField, filterText);
                    setListedTopics(topics, session);
                    setFilterField(filterField, session);
                    setFilterText(filterText, session);
                    setUseCache(Boolean.TRUE, session);
                    return PAGE_LIST;
                }
            }
            setUseCache(Boolean.TRUE, session);
            return PAGE_LIST;
        } else if (action.equals(ACTION_CLEAR_FILTER)) {
            // -- reset filter and search attributes to "null"
            // session.setAttribute("filterField", null);
            Vector topics = getCachedTopicList(session);
            setListedTopics(topics, session);
            session.setAttribute("filterText", null);
            session.setAttribute("filterField", null);
            setUseCache(Boolean.TRUE, session);
            System.out.println(">>> cleared Filter");
            if (session.getAttribute("isSlim").equals("true")) {
                return PAGE_SLIM_LIST;
            }
            return PAGE_LIST;
        } else if (action.equals(ACTION_CREATE_FORM_LETTER)) {
            String letter = "";
            if (getFilterField(session) != null) {
                letter = createFormLetter(getListedTopics(session));
                // System.out.println("Take Filtered Topic List: " + letter);
            } else {
                letter = createFormLetter(getCachedTopicList(session));
                // System.out.println("Take Cached Topic List: " + letter);
            }
            if (letter != null && letter.equals("")) {
                setUseCache(Boolean.TRUE, session);
                return PAGE_LIST;
            }
            String link = as.getCorporateWebBaseURL() + FILESERVER_DOCUMENTS_PATH;
            link += writeLetter(letter, "Adressen.txt");
            session.setAttribute("formLetter", link);
            return PAGE_LINK_PAGE;
        } else if (action.equals(ACTION_FILTER_ROUNDMAILING)) {
            String cityMap = (String) params.getValue("cityMapID");
            // re-set the map
            session.setAttribute("cityMapID", cityMap);
            CityMapTopic map = (CityMapTopic) as.getLiveTopic(cityMap, 1); // live/base
            SearchCriteria[] criterias = map.getSearchCriterias();
            Vector categories = new Vector();
            for (int i = 0; i < criterias.length; i++) {
                String critId = criterias[i].criteria.getID();
                Vector cats = cm.getTopics(critId); // ics(crit.getID(), new Hashtable(), map.getID(), directives);
                categories.addAll(cats);
            }
            sortBaseTopics(categories);
            // Vector allTopics = cm.getViewTopics(map.getID(), 1);
            session.setAttribute("availableCategories", categories);
            session.setAttribute("filterField", "");
            session.setAttribute("filterFieldNames", new Vector());
            session.setAttribute("cityMapName", map.getName());
            session.setAttribute("recipients", "");
            return PAGE_LIST_MAILING;
        } else if (action.equals(ACTION_CREATE_ROUNDMAILING)) {
            String cityMap = (String) session.getAttribute("cityMapID");
            String formerRecipients = (String) session.getAttribute("recipients");
            Vector filterFieldNames = (Vector) session.getAttribute("filterFieldNames");
            // String filterText = (String) session.getAttribute("filterText");
            String filterField = (String) params.getValue("filterField");
            filterFieldNames.add(as.getLiveTopic(filterField, 1).getName());
            session.setAttribute("filterFieldNames", filterFieldNames);
            //
            CityMapTopic map = (CityMapTopic) as.getLiveTopic(cityMap, 1); // live/base
            Vector mapTopics = cm.getViewTopics(map.getID(), 1);
            Vector filteredTopics = new Vector();
            for (int i = 0; i < mapTopics.size(); i++) {
                BaseTopic topic = (BaseTopic) mapTopics.get(i);
                try {
                    // checking if a topic has the relation to the filterField assuming it's a subtype of criteria
                    Vector cats = (Vector) as.getRelatedTopics(topic.getID(), ASSOCTYPE_ASSOCIATION, 2);
                    fcats:
                    for (int j = 0; j < cats.size(); j++) {
                        BaseTopic kaTopic = (BaseTopic) cats.get(j);
                        if (kaTopic.getID().equals(filterField)) {
                            filteredTopics.add(topic);
                            break fcats;
                        }
                    }
                } catch (DeepaMehtaException ex) {
                    // System.out.println("*** ListServlet.Exc is: " + ex.getMessage() + " remvoing topic from results.. " + topic.getName());
                }
            }
            // Vector allTopics = cm.getViewTopics(map.getID(), 1);
            StringBuffer mailBoxes = new StringBuffer("");
            if (filteredTopics.size() > 0) {
                // ToDo check the Email property properly (Engagement Workspace?)
                Vector mailTo = lookUpMailAdresses(filteredTopics);
                for (int j = 0; j < mailTo.size(); j++) {
                    String mailBox = (String) mailTo.get(j);
                    if (formerRecipients.indexOf(mailBox) == -1) {
                        // checked for double
                        mailBoxes.append(mailTo.get(j) + ", ");
                    }
                }
                System.out.println("" + mailTo.size() + ". mailBoxes:" + mailBoxes.toString() + " formerRecipients: " + formerRecipients);
            }
            mailBoxes.append(formerRecipients);
            session.setAttribute("recipients", mailBoxes.toString()); // --. update the linkedcontent
            return PAGE_LIST_MAILING;
        } else if (action.equals(ACTION_FILTER_MAIL_ALL)) {
            String cityMap = (String) session.getAttribute("cityMapID");
            CityMapTopic map = (CityMapTopic) as.getLiveTopic(cityMap, 1); // live/base
            //
            Vector mapTopics = cm.getViewTopics(map.getID(), 1);
            StringBuffer mailBoxes = new StringBuffer("");
            if (mapTopics.size() > 0) {
                // ToDo check the Email property properly (Engagement Workspace?)
                Vector mailTo = lookUpMailAdresses(mapTopics);
                for (int j = 0; j < mailTo.size(); j++) {
                    String mailBox = (String) mailTo.get(j);
                    mailBoxes.append(mailTo.get(j) + ", ");
                }
            }
            session.setAttribute("recipients", mailBoxes.toString());
            return PAGE_LIST_MAILING;
        } else if (action.equals(ACTION_DELETE_ENTRY)) {
            String topicId = params.getParameter("id");
            GeoObjectTopic geo = (GeoObjectTopic) as.getLiveTopic(topicId, 1);
            // --- check if new topic is to be deleted remotely first
            if (geo.isPartOfMigratedWorkspaces()) {
                // --- delete mirror topic ---
                geo.deleteRemoteTopic();
            }
            // --- delete topic ---
            deleteTopic(topicId);
            if (session.getAttribute("isSlim").equals("true")) {
                setUseCache(Boolean.FALSE, session);
                return PAGE_SLIM_LIST;
            } else {
                removeTopicFromCache(topicId, session);
                setUseCache(Boolean.TRUE, session);
            }
            return PAGE_LIST;
        } else if (action.equals(ACTION_EXPORT_CITYMAP)) {
            String cityMap = (String) params.getValue("cityMapID");
            CityMapTopic map = (CityMapTopic) as.getLiveTopic(cityMap, 1); // live/base
            String mapAlias = map.getProperty(PROPERTY_WEB_ALIAS);
            BaseTopic instType = as.getLiveTopic(map.getInstitutionType());
            Vector allTopics = cm.getViewTopics(map.getID(), 1);
            System.out.println(">>> ListServlet got request to export \"" + mapAlias + "\" with " + instType.getName() + " (" + allTopics.size() + ")");
            // ### ToDo render approximate waiting time into the displayed result webpage
            String absoluteFileNamePath = "/home/jrichter/deepamehta/install/client/documents/" + mapAlias + ".csv"; // ### hardcoded
            File fileToWrite = new File(absoluteFileNamePath); //
            // Time
            Calendar cal = Calendar.getInstance();
            long now = cal.getTimeInMillis();
            // Date
            SimpleDateFormat sfc = new SimpleDateFormat("E HH:mm dd MMM yy");
            Date date = new Date();
            String timestamp = sfc.format(date);
            try {
                if (fileToWrite.exists()) {
                    long touched = fileToWrite.lastModified();
                    // check wether the file is pretty fresh or not
                    System.out.println("  > file already exists and\"" + absoluteFileNamePath + "\"");
                    System.out.println("  > and system knows that now it's " + now + " and the file was touched " + fileToWrite.lastModified());
                    // if (now-21600000 < touched) { // timestamp is smaller than now minus 10 000 seconds
                    //  session.setAttribute("title", "Die Daten sind aktueller als 6 Stunden und werden daher vorerst nicht wieder aktualisiert.");
                    // } else {
                    // go ahead and write the file
                    worker = new Thread(new DownloadWorker(as, cm, map, absoluteFileNamePath));
                    // start the worker to export the map to the document-repository
                    worker.start();
                    // System.out.println(">> File is going to be written, data is older than 6hrs and the user requested so");
                    session.setAttribute("title", "In wenigen Minuten stehen die aktuellsten Daten des Stadtplans zum Download bereit");
                    // }
                } else {
                    // go ahead and write the file
                    worker = new Thread(new DownloadWorker(as, cm, map, absoluteFileNamePath));
                    // start the worker to export the map to the document-repository
                    worker.start();
                    session.setAttribute("title", "In wenigen Minuten stehen die aktuellsten Daten des Stadtplans zum Download bereit");
                }
            } catch (Exception ex) {
                System.out.println("*** ListServlet.exportCityMapError " + ex.getLocalizedMessage());
            }
            session.setAttribute("link", as.getCorporateWebBaseURL() + FILESERVER_DOCUMENTS_PATH + mapAlias + ".csv");
            return PAGE_DOWNLOAD_PAGE;
        } else if (action.equals(ACTION_DOWNLOAD_CITYMAP)) {
            // get webAlias
            String cityMap = (String) params.getValue("cityMapID");
            String mapName = "";
            if (cityMap != null) {
                CityMapTopic map = (CityMapTopic) as.getLiveTopic(cityMap, 1); // live/base
                mapName = map.getName();
                String mapAlias = map.getProperty(PROPERTY_WEB_ALIAS);
                if (!mapAlias.equals("")) {
                    session.setAttribute("link", as.getCorporateWebBaseURL() + FILESERVER_DOCUMENTS_PATH + mapAlias + ".csv");
                } else {
                    session.setAttribute("link", as.getCorporateWebBaseURL() + sc.getContext("list").getServletContextName()); // point back if it's a "guest"
                }
            } else {
                session.setAttribute("link", as.getCorporateWebBaseURL() + sc.getContext("list").getServletContextName()); // point back if it's a "guest";
            }
            session.setAttribute("title", "Download der &ouml;ffentlichen Daten des Stadtplans \"" + mapName + "\"");
            return PAGE_DOWNLOAD_PAGE;
        } else if (action.equals(ACTION_SHOW_LIST_LEGEND)) {
            return PAGE_LIST_INFO;
        }
        //
        return super.performAction(action, params, session, directives);
    }

    protected void preparePage(String page, RequestParameter params, Session session, CorporateDirectives directives) {
        if (page.equals(PAGE_LIST_HOME)) {
            // next line: membership preferences are set according to workspaces
            Vector workspaces = getWorkspaces(getUserID(session), session);
            Hashtable cityMaps = getCityMaps(workspaces);
            Hashtable mapCounts = getMapCounts(cityMaps);
            Hashtable mapTimes = getMapTimes(cityMaps);
            session.setAttribute("mapTimes", mapTimes);
            session.setAttribute("mapCounts", mapCounts);
            session.setAttribute("workspaces", workspaces);
            session.setAttribute("cityMaps", cityMaps);
            session.setAttribute("emailList", null);
        } else if (page.equals(PAGE_LIST)) {
            String sortBy = getSortByField(session);
            // refresh geo objects in list from cm, if caching is not used
            if (!isCacheUsed(session).booleanValue()) {
                String cityMapID = getCityMap(session).getID();
                String instTypeID = getInstTypeID(session);
                Vector insts = cm.getTopicIDs(instTypeID, cityMapID, true);		// sortByTopicName=true
                Vector topicBeans = new Vector();
                for (int i = 0; i < insts.size(); i++) {
                    // Creates TopicBean
                    TopicBean topic = as.createTopicBean(insts.get(i).toString(), 1);
                    topicBeans.add(topic);
                }
                //
                setCachedTopicList(topicBeans, session);
                // fresh topic data & re sorted
                if (sortBy != null) {
                    sortBeans(topicBeans, sortBy);
                }
                // fresh topic data & re filtered (just used after create geo)
                if (getFilterField(session) != null) {
                    String filterText = (String) session.getAttribute("filterText");
                    topicBeans = filterBeansByField(topicBeans, getFilterField(session), filterText);
                    // System.out.println(">>>> re-filtered fresh data in topicList");
                }
                // set topics to render in list, differ from cached if sorted or filtered
                setListedTopics(topicBeans, session);
                // notifications
                session.setAttribute("notifications", directives.getNotifications());
            } else {
                // System.out.println(">>> used cached or filtered topic list");
                session.setAttribute("notifications", directives.getNotifications());
            }
            System.out.println(">>>> Runtime has: " + Runtime.getRuntime().maxMemory() / 1024 / 1024 + "mb "
                    + " of max, " + Runtime.getRuntime().totalMemory() / 1024 / 1024 + " of total, "
                    + "" + Runtime.getRuntime().freeMemory() / 1024 / 1024 + " in memory");
            // prepare the correct mailto link
            if (getFilterField(session) != null) {
                Vector beans = getListedTopics(session);
                Vector mailAdresses = getMailAddresses(beans);
                session.setAttribute("emailList", mailAdresses);
                // System.out.println(">>>> filtered emailList created with : " + mailAdresses.size() + " Einträge");
            } else {
                Vector beans = getCachedTopicList(session);
                Vector mailAdresses = getMailAddresses(beans);
                session.setAttribute("emailList", mailAdresses);
                // System.out.println(">>>> emailList created with : " + mailAdresses.size() + " Einträge");
            }
        } else if (page.equals(PAGE_SLIM_LIST)) {
            String sortBy = getSortByField(session);
            // SLIM list is not using the cache check to be dropped
            if (!isCacheUsed(session).booleanValue()) {
                String cityMapID = getCityMap(session).getID();
                String instTypeID = getInstTypeID(session);
                Vector insts = cm.getTopicIDs(instTypeID, cityMapID, true);		// sortByTopicName=true
                Vector topics = new Vector();
                for (int i = 0; i < insts.size(); i++) {
                    // Loads BaseTopic
                    BaseTopic topic = as.getLiveTopic(insts.get(i).toString(), 1);
                    topics.add(topic);
                }
                //
                setCachedTopicList(topics, session);
                // fresh topic data & re sorted
                if (sortBy != null) {
                    sortBeans(topics, sortBy);
                    // System.out.println(">>>> topics are fresh from server and sorted by: "
                    //+ session.getAttribute("sortField") );
                }
                // fresh topic data & re filtered (just used after create geo)
                if (getFilterField(session) != null) {
                    String filterText = (String) session.getAttribute("filterText");
                    topics = filterTopicsByName(topics, getFilterField(session), filterText);
                    // System.out.println(">>>> re-filtered fresh data in topicList");
                }
                // set topics to render in list, differ from cached if sorted or filtered
                setListedTopics(topics, session);
                // notifications
                session.setAttribute("notifications", directives.getNotifications());
            } else {
                // System.out.println(">>> used cached or filtered topic list");
                session.setAttribute("notifications", directives.getNotifications());
            }
        }
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


    // **********************
    // *** Custom Methods ***
    // **********************



    private void sortBaseTopics(Vector baseTopics) {
        // ### System.out.println(">>> sorting for german strings supported");
        Collections.sort(baseTopics, new MyTopicSort());
    }

    private void sortBeans(Vector topicBeans, String sortBy) {
        // ### System.out.println(">>> sorting for german strings supported");
        Collections.sort(topicBeans, new MyStringComparator(sortBy));
    }

    private Vector filterBeansByField(Vector topicBeans, String filterField, String filterText) {
        Vector filteredBeans = new Vector();
        //
        String prop;
        BaseTopic topicProp;
        TopicBean topicBean;
        TopicBeanField beanField;
        for (int i = 0; i < topicBeans.size(); i++) {
            topicBean = (TopicBean) topicBeans.get(i);
            beanField = (TopicBeanField) topicBean.getField(filterField);
            // TopicBeanFields of TYPE_MULTI
            if (beanField.type == TopicBeanField.TYPE_MULTI) {
                multiLoop:
                for (int j = 0; j < beanField.values.size(); j++) {
                    topicProp = (BaseTopic) beanField.values.get(j);
                    prop = topicProp.getName();
                    if (prop.toLowerCase().indexOf(filterText.toLowerCase()) != -1) {
                        filteredBeans.add(topicBean);
                        break multiLoop;
                    }
                }
                // TopicBeanFields of TYPE_SINGLE
            } else {
                prop = (String) beanField.value;
                if (prop.toLowerCase().indexOf(filterText.toLowerCase()) != -1) {
                    filteredBeans.add(topicBean);
                }
            }
        }
        //
        return filteredBeans;
    }

    private Vector filterTopicsByName(Vector topics, String filterField, String filterText) {
        Vector filteredTopics = new Vector();
        //
        String prop;
        // BaseTopic topicProp;
        BaseTopic topic;
        String beanField;
        for (int i = 0; i < topics.size(); i++) {
            topic = (BaseTopic) topics.get(i);
            beanField = topic.getName();
            // TopicBeanFields of TYPE_MULTI
            if (beanField.toLowerCase().indexOf(filterText.toLowerCase()) != -1) {
                filteredTopics.add(topic);
            }
        }
        //
        return filteredTopics;
    }

    /**
	 * Collects Email Addresses for all given beans by searching for TopicBeanFields {@link TopicBeanField}
	 * which are inherited by each bean as PROPERTY_EMAIL_ADDRESS as Email Topic (has to be named PROPERTY_EMAIL_ADDRESS)
	 * and a TopicBeanField with the name "Person / Email Address" (Email of Person which is assigned to an Institution)
     * @param topics
     * @return a list of Strings which are all email adresses
     */
    private Vector getMailAddresses(Vector topics) {
        Vector mailAdresses = new Vector();
        //
        Enumeration e = topics.elements();
        while (e.hasMoreElements()) {
            TopicBean bean = (TopicBean) e.nextElement();
            String mailbox = getMailbox(bean);
            if (!mailbox.equals("")) {
                mailAdresses.add(mailbox);
            }
        }
        //
        return mailAdresses;
    }

    /**
     * Collects Email Addresses for all given basetopics by searching for PROPERTY_EMAIL_ADDRESS as Email Topic (has to
     * be named PROPERTY_EMAIL_ADDRESS)
     *
     * @param topics
     * @return a list of Strings which are all email adresses
     */
    private Vector lookUpMailAdresses(Vector topics) {
        Vector mailAdresses = new Vector();
        //
        Enumeration e = topics.elements();
        while (e.hasMoreElements()) {
            BaseTopic geoObject = (BaseTopic) e.nextElement();
            String mailbox = as.getTopicProperty(geoObject.getID(), 1, PROPERTY_EMAIL_ADDRESS);
            // direct property
            if (!mailbox.equals("") && checkForMailBox(mailbox)) {
                mailbox = splitMailBoxes(mailbox);
                mailbox = encodeMailAmpersandTo(mailbox);
                mailAdresses.add(mailbox);
                // System.out.println("> >>  added EmailProperty of " + geoObject.getName() + " to recipients");
            } else {
                try {
                    mailbox = as.getRelatedTopic(geoObject.getID(), ASSOCTYPE_ASSOCIATION, TOPICTYPE_EMAIL_ADDRESS, 2, false).getName();
                    if (checkForMailBox(mailbox)) {
                        mailbox = splitMailBoxes(mailbox);
                        mailbox = encodeMailAmpersandTo(mailbox);
                        mailAdresses.add(mailbox);
                    }
                } catch (DeepaMehtaException ex) {
                    // --- to ignore .. System.out.println("*** ListServlet.MailBoxExc. : " +  ex.getMessage());
                } catch (AmbiguousSemanticException aex) {
                    mailbox = aex.getDefaultTopic().getName();
                    if (checkForMailBox(mailbox)) {
                        mailbox = splitMailBoxes(mailbox);
                        mailAdresses.add(mailbox);
                        // System.out.println("> >> added related EmailAddress of " + geoObject.getName() + " to recipients");
                    }
                }
            }
        }
        //
        return mailAdresses;
    }

    /**
     * mailAddress is handled as a correct mailbox if it contains an at-sign and no whitespace
     */
    private boolean checkForMailBox(String mailBox) {
        if (mailBox.indexOf("@") == -1 || mailBox.indexOf(" ") != -1) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * some people do have an ampersand in their mailadress, it's allowed and there are other cases ### ToDo
     */
    private String encodeMailAmpersandTo(String mailBox) {
        return mailBox.replaceAll("&", "%26").toString();
    }

    /**
     * this method assumes that people divide different mailboxes through using _either_ / or :
     */
    private String splitMailBoxes(String mailBox) {
        int splitIndex = mailBox.indexOf("/");
        StringBuffer result = new StringBuffer();
        if (splitIndex != -1) {
            String[] addresses = mailBox.split("/");
            for (int i = 0; i < addresses.length; i++) {
                String box = addresses[i];
                result.append(box);
                if (i < addresses.length) {
                    result.append(", ");
                }
            }
            System.out.println("> >>> modified MailBoxSplit /  " + mailBox.toString() + " to : " + result.toString());
            return result.toString();
        }
        splitIndex = mailBox.indexOf(":");
        if (splitIndex != -1) {
            String[] addresses = mailBox.split(":");
            for (int i = 0; i <= addresses.length; i++) {
                String box = addresses[i];
                result.append(box);
                if (i < addresses.length) {
                    result.append(", ");
                }
            }
            System.out.println("> >>> modified MailBoxSplit : " + mailBox.toString() + " to : " + result.toString());
            return result.toString();
        }
        // do nothing
        return mailBox;
    }

    /**
     * See getMailAddresses
     *
     * @param bean
     * @return
     */
    private String getMailbox(TopicBean bean) {
        TopicBeanField mailProp = bean.getField(PROPERTY_EMAIL_ADDRESS);
        if (mailProp != null) {
            // direct related Email Topic
            // ### Value can be not null and just empty "" () have to verify this in the form processor
            if (mailProp.value != null && !mailProp.value.equals("")) {
                // Type Single
                // System.out.println("type single mail property is: " + mailProp.value);
                return mailProp.value;
            } else if (mailProp.values != null && mailProp.values.size() > 0) {
                // Type Multi
                BaseTopic mailTopic = (BaseTopic) mailProp.values.get(0);
                if (!mailTopic.getName().equals("")) {
                    // System.out.println("type multi direct mail topic is: " + mailTopic.getName());
                    return mailTopic.getName();
                }
            }
        } else {
            // indirect related Email Topic via Person
            TopicBeanField mailField = bean.getField("Person / Email Address");
            if (mailField != null && mailField.type == TopicBeanField.TYPE_MULTI) {
                // ### System.out.println("indirect mailProp Field is: " + mailField.name);
                if (mailField.values.size() > 0) {
                    BaseTopic propTopic = (BaseTopic) mailField.values.get(0);
                    String mail = as.getTopicProperty(propTopic, PROPERTY_EMAIL_ADDRESS);
                    if (mail != null && mail.indexOf("@") != -1) {
                        //System.out.println("**** found indirect related email adress, added to \"mailUrl\": " +
                        //mail + ", fieldName is: " + mailField.name);
                        return mail;
                    }

                }
            }
        }
        return "";
    }

    /**
     *
     * @param topics
     * @return can be empty an empty string if the given topics were null
     */
    private String createFormLetter(Vector topics) {
        String letter = "Name" + createTab() + "Email" + createTab() + "Ansprechpartner/in" + createTab() + "Straße / Hnr."
                + "" + createTab() + "PLZ" + createTab() + "Stadt\n";
        String personName = "";
        String entry = "";
        //
        if (topics == null) {
            return "";
        }
        Enumeration e = topics.elements();
        while (e.hasMoreElements()) {
            TopicBean bean = (TopicBean) e.nextElement();
            // get related Address
            String address = getTabbedAddress(bean);
            String mailbox = getMailbox(bean);
            if (mailbox.equals("") && address == null) {
                entry = "";
                System.out.println("[Info]: Neither mailbox nor address provided, skip the entry: " + bean.name);
            } else {
                // at least one is available, make an entry
                entry += bean.name;
                entry += createTab();
                entry += getMailbox(bean);
                entry += createTab();
                if (address != null) {
                    // Create an Entry, starting with Name Tab
                    personName = getRelatedPersonName(bean);
                    //filling the Ansprechpartner Tab
                    entry += personName;
                    entry += createTab();
                    // filling the Street, Code, and City, method insert Tabs for you
                    entry += address;
                }
                // prepare for a new entry
                entry += "\n";
                // append it to the letter and clear
                letter += entry;
                entry = "";
            }
        }
        return letter;
    }

    private String createTab() {
        return "\t";
    }

    /**
     *
	 * Related Topic Name, if no relatedPerson looks for Related Info Properties on Person
	 * If no Lastname is set an empty String is returned, normally Firstname Lastname without Gender
     *
     * @param bean
	 * @return <Code>""<Code> if no lastname is assigned to the person
     */
    private String getRelatedPersonName(TopicBean bean) {
        String relatedPerson = new String();
        String firstName = bean.getValue("Person / First Name");
        String lastName = bean.getValue("Person / Name");
        TopicBeanField personField = (TopicBeanField) bean.getField("Person");
        if (personField != null && personField.type == TopicBeanField.TYPE_MULTI) {
            Vector persons = (Vector) personField.values;
            if (persons.size() > 0) {
                BaseTopic person = (BaseTopic) persons.get(0);
                // System.out.println(">>> createFormLetter:getRelatedPerson(): Related Topic Name is: " + person.getName());
                relatedPerson = person.getName();
                return relatedPerson;
            } else {
                // System.out.println(">>> createFormLetter:getRelatedPerson(): Related Topic Name is empty, found props: "
                // + lastName + ", "  +firstName);
            }
        } else if (lastName != null && !lastName.equals("")) {
            if (firstName != null && !firstName.equals("")) {
                relatedPerson += firstName + " " + lastName;
            } else {
                relatedPerson += lastName;
            }
            // System.out.println(">>> createFormLetter:getRelatedPerson(): There is no related Person but at least a " +
            // "lastName, so: take " + relatedPerson);
            return relatedPerson;
        }
        return relatedPerson;
    }

    /**
	 * Creates an address string with street, code, city divided by tabulator
	 * Addresses are not allowed to be empty, Cities are. PostalCode is enough for us to send a letter.
     *
     * @param bean
	 * @return <Code>null<Code> if no street and zip code could be found
     */
    private String getTabbedAddress(TopicBean bean) {
        String address = "";
        // get Street & Postal Code
        String street = bean.getValue("Address / Street");
        String postalCode = bean.getValue("Address / Postal Code");
        // check for web_info deeply related or related info address topic data
        if (street != null && postalCode != null) {
            // if WEB_INFO_ is not deeply, beans do not have "Address / Street" as TopicBeanField. Street is named "Address" then
            if (street.equals("") || postalCode.equals("")) {
                // System.out.println("*** createFormLetter:skipAddr, neither street or postalCode was a
                // provided as deep topicdata");
                return null;
            } else {
                address = street + createTab() + postalCode + createTab();
            }
            // check for web_info related topic name
        } else {
            Vector addresses = bean.getValues("Address");
            BaseTopic addressTopic = (BaseTopic) addresses.get(0);
            postalCode = as.getTopicProperty(addressTopic, PROPERTY_POSTAL_CODE);
            address = addressTopic.getName() + createTab() + postalCode + createTab();
            //System.out.println("*** createFormLetter: not deeply or info related address topic, " +
            // "topic name delivers: " + address);
        }
        // get City
        String oldCityProp = bean.getValue("Stadt");
        Vector citys = bean.getValues("Address / City");
        // - via "Stadt" Property
        if (oldCityProp != null) {// != null && oldCityProp.type == TopicBeanField.TYPE_SINGLE) {
            address += oldCityProp;
            // System.out.println("*** found old \"Stadt\" Property. so City is: " + address.toString());
            // - via deeply related MultiType City
        } else if (citys != null && citys.size() > 0) { // != null && cityField.type == TopicBeanField.TYPE_MULTI){
            BaseTopic city = (BaseTopic) citys.get(0);
            address += city.getName();
            // System.out.println("**** found related city: " + address.toString());
            // - no city data, but give berlin a plz try
        } else {
            if (postalCode != null) {
                postalCode.replaceAll(" ", ""); // clean up for an evtl. valueOf NumberFormatException
                int value = Integer.valueOf(postalCode).intValue();
                if (value > 10001 && value <= 14199) {
                    // is within 10001 and 14199
                    System.out.println("*** createFormLetter: no city assigned to Address: " + address + ", but internal postal "
                            + "code check delivered \"Berlin\" as the city");
                    address += "Berlin";
                }
            }
        }
        return address;
    }

    private Vector getWorkspaces(String userID, Session session) {
        Vector workspaces = new Vector();
        //
        session.setAttribute("membership", "");
        Vector ws = as.getRelatedTopics(userID, SEMANTIC_MEMBERSHIP, TOPICTYPE_WORKSPACE, 2);
        Enumeration e = ws.elements();
        if (!e.hasMoreElements()) {
            Vector aws = as.getRelatedTopics(userID, SEMANTIC_AFFILIATED_MEMBERSHIP, TOPICTYPE_WORKSPACE, 2);
            session.setAttribute("membership", "Affiliated");
            e = aws.elements();
        }
        while (e.hasMoreElements()) {
            BaseTopic w = (BaseTopic) e.nextElement();
            if (isKiezatlasWorkspace(w.getID())) {
                workspaces.addElement(w);
            }
        }
        //
        return workspaces;
    }

    private boolean isKiezatlasWorkspace(String workspaceID) {
        if (workspaceID.equals(WORKSPACE_KIEZATLAS)) {
            return true;
        }
        //
        Vector assocTypes = new Vector();
        assocTypes.addElement(SEMANTIC_SUB_WORKSPACE);
        return cm.associationExists(WORKSPACE_KIEZATLAS, workspaceID, assocTypes);
    }

    // ---

    private Hashtable getCityMaps(Vector workspaces) {
        Hashtable cityMaps = new Hashtable();
        //
        Enumeration e = workspaces.elements();
        while (e.hasMoreElements()) {
            String workspaceID = ((BaseTopic) e.nextElement()).getID();
            BaseTopic topicmap = as.getWorkspaceTopicmap(workspaceID);
            Vector maps = cm.getTopics(TOPICTYPE_CITYMAP, new Hashtable(), topicmap.getID());
            // Vector maps = cm.getTopics(TOPICTYPE_CITYMAP, null, new Hashtable(), topicmap.getID(), null, true); // sorted
            cityMaps.put(workspaceID, maps);
        }
        //
        return cityMaps;
    }

    private Hashtable getMapCounts(Hashtable cityMaps) {
        Hashtable counts = new Hashtable();
        // System.out.println(">> TIMER: mapCount was started at : " + DeepaMehtaUtils.getTime(true));
        Enumeration wsIds = cityMaps.keys();
        while (wsIds.hasMoreElements()) {
            String workspaceId = (String) wsIds.nextElement();
            Vector maps = (Vector) cityMaps.get(workspaceId);
            BaseTopic typeTopic = getWorkspaceSubType(workspaceId, KiezAtlas.TOPICTYPE_KIEZ_GEO);
            for (int i = 0; i < maps.size(); i++) {
                BaseTopic map = (BaseTopic) maps.get(i);
                Vector allTopics = cm.getViewTopics(map.getID(), 1, typeTopic.getID());
                counts.put(map.getID(), allTopics.size()); // hashtable does like int's, ### unsafe
            }
        }
        // System.out.println(">> TIMER: mapCount has finished at : " + DeepaMehtaUtils.getTime(true));
        return counts;
    }

    /**
     * If returned result vector contains a mapId as key where the value is equals("") - export was never done before
     *
     * @param cityMaps
     * @return
     */
    private Hashtable getMapTimes(Hashtable cityMaps) {
        Hashtable counts = new Hashtable();
        // System.out.println(">> TIMER: mapCount was started at : " + DeepaMehtaUtils.getTime(true));
        Enumeration wsIds = cityMaps.keys();
        // System.out.println(">> TIME INFO: Starting Fetchint the MapTimes : " + DeepaMehtaUtils.getTime(true));
        while (wsIds.hasMoreElements()) {
            String workspaceId = (String) wsIds.nextElement();
            Vector maps = (Vector) cityMaps.get(workspaceId);
            for (int i = 0; i < maps.size(); i++) {
                BaseTopic map = (BaseTopic) maps.get(i);
                String mapAlias = as.getTopicProperty(map, PROPERTY_WEB_ALIAS);
                String absoluteFileNamePath = "/home/jrichter/deepamehta/install/client/documents/" + mapAlias + ".csv"; // ### hardcoded
                // String absoluteFileNamePath = "/home/monty/source/deepaMehta/install/client/documents/"+mapAlias+".csv"; //
                File fileToWrite = new File(absoluteFileNamePath);
                if (fileToWrite.exists()) {
                    Date d = new Date(fileToWrite.lastModified());
                    Calendar cal = Calendar.getInstance(TimeZone.getDefault());
                    long now = cal.getTimeInMillis();
                    if (now - 21600000 > fileToWrite.lastModified()) { // timestamp is smaller than now minus 10 000 seconds
                        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM, E HH:mm");
                        counts.put(map.getID(), sdf.format(d));
                    } else {
                        counts.put(map.getID(), "isUpToDate");
                    }
                } else {
                    counts.put(map.getID(), "");
                }
                //
            }
        }
        // System.out.println(">> TIME INFO: mapCount has finished at : " + DeepaMehtaUtils.getTime(true));
        return counts;
    }

    /**
     * returns null if no topictype whihc is assigned to the given workspace,
     * is a subtype of "GeoObjectTopic"
     *
     * @param workspaceId
     * @return
     */
    private BaseTopic getWorkspaceSubType(String workspaceId, String superTypeId) {
        //
        TypeTopic geotype = as.type(superTypeId, 1);
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

    private void updateTopicInCache(GeoObjectTopic geo, Session session) {
        Vector topics = getCachedTopicList(session);
        for (int i = 0; i < topics.size(); i++) {
            String topicId = "";
            try {
                TopicBean t = ((TopicBean) topics.get(i));
                topicId = t.id;
            } catch (ClassCastException cex) {
                System.out.println(">> CatchedClassCastException: of object " + cex.getClass() + "GeoObject: " + ((GeoObjectTopic) topics.get(i)).getID());
                topicId = ((GeoObjectTopic) topics.get(i)).getID();
            }
            // if (t instanceof TopicBean) { topicId = t.id } else { t }
            if (topicId.equals(geo.getID())) {
                topics.set(i, as.createTopicBean(geo.getID(), 1));
                System.out.println(">>>> replaced topic (" + geo.getID() + ") in cache, inserted a fresh bean");
            }
        }
        setCachedTopicList(topics, session);
    }

    private void inserTopicIntoCache(GeoObjectTopic geo, Session session) {
        Vector topics = getCachedTopicList(session);
        if (topics != null) {
            TopicBean geoBean = as.createTopicBean(geo.getID(), 1);
            topics.add(1, geoBean); // put geoobject as bean in first place
            System.out.println(">>>> updated cache, inserted a fresh bean (" + geoBean.id + ") ");
            setCachedTopicList(topics, session);
        } else {
            System.out.println(">>>> List cache empty, do not cache anything");
        }

    }

    private void removeTopicFromCache(String topicId, Session session) {
        Vector topics = getCachedTopicList(session);
        for (int i = 0; i < topics.size(); i++) {
            TopicBean t = (TopicBean) topics.get(i);
            if (t.id.equals(topicId)) {
                topics.remove(i);
                System.out.println(">>>> removed topic (" + topicId + ") from cache");
            }
        }
    }

    /**
     * Writes txt files with incremental number into the documents repository
     *
     * @param letter
     * @param fileName
     */
    private String writeLetter(String letter, String fileName) {
        String path = "/home/jrichter/deepamehta/install/client/documents/"; // ### hardcoded ka-server
        // String path = "/home/monty/source/deepaMehta/install/client/documents/"; // ### hardcoded mre's
        File toFile = new File(path + fileName);
        try {
            int copyCount = 0;
            String newFilename = null;
            int pos = fileName.lastIndexOf('.');
            while (toFile.exists()) {
                copyCount++;
                newFilename = fileName.substring(0, pos) + "-" + copyCount + fileName.substring(pos);
                toFile = new File(path + newFilename);
                System.out.println("  > file already exists, try \"" + newFilename + "\"");
                //fileName = newFilename;
            }
            FileOutputStream fout = new FileOutputStream(toFile, true);
            OutputStreamWriter out = new OutputStreamWriter(fout, "ISO-8859-1");
            out.write(letter);
            out.close();
            System.out.println(">>> writeLetter(): written file successfully from: " + toFile.getAbsolutePath());
        } catch (IOException ex) {
            System.out.println("***: Error with writing File:" + ex.getMessage());
        }
        return toFile.getName();
    }

    private void checkForWarnings(RequestParameter params, Session session, CorporateDirectives directives) {
        CityMapTopic cityMap = getCityMap(session);
        String geoName = params.getValue(PROPERTY_NAME);
        // warning if YADE is "off"
        if (!cityMap.isYADEOn()) {
            directives.add(DIRECTIVE_SHOW_MESSAGE, "\"" + geoName + "\" wurde hilfsweise in die Ecke oben/links positioniert "
                    + "(Stadtplan \"" + cityMap.getName() + "\" hat keine YADE-Referenzpunkte)", new Integer(NOTIFICATION_WARNING));
        } else if (params.getValue(PROPERTY_YADE_X).equals("") && params.getValue(PROPERTY_YADE_Y).equals("")) {
            directives.add(DIRECTIVE_SHOW_MESSAGE, "\"" + geoName + "\" wurde hilfsweise in die Ecke oben/links positioniert "
                    + "-- Bitte YADE-Koordinaten angeben", new Integer(NOTIFICATION_WARNING));
        }
    }


    // *************************
    // *** Session Utilities ***
    // *************************



    // --- Methods to maintain data in the session

    /**
     * Stores the full List of Topics into the Session, independent of a filter. If ACTION_FILTER is applied to this servlet and this session
     * the calculations takes all elements in this list as abase: meaning that the filter operates alway on all topics wether or not
     * it there was an ACTION_FILTER in session before
     *
     * @param beans
     * @param session
     */
    private void setCachedTopicList(Vector beans, Session session) {
        System.out.println(">>> stored " + beans.size() + " \"cachedTopics\" in session");
        session.setAttribute("cachedTopics", beans);
    }

    private Vector getCachedTopicList(Session session) {
        return (Vector) session.getAttribute("cachedTopics");
    }

    private void setUser(BaseTopic user, Session session) {
        session.setAttribute("user", user);
        System.out.println("> \"user\" stored in session: " + user);
    }

    private void setCityMap(BaseTopic cityMap, Session session) {
        session.setAttribute("cityMap", cityMap);
        System.out.println("> \"cityMap\" stored in session: " + cityMap);
    }

    private void setWorkspaceTopic(BaseTopic workspace, Session session) {
        session.setAttribute("workspace", workspace);
        System.out.println("> \"workspace\" stored in session: " + workspace);
    }

    private void setInstTypeID(String instTypeID, Session session) {
        session.setAttribute("instTypeID", instTypeID);
        System.out.println("> \"instTypeID\" stored in session: " + instTypeID);
    }

    private void setGeoObject(BaseTopic geo, Session session) {
        session.setAttribute("geo", geo);
        System.out.println("> \"geo\" stored in session: " + geo);
    }

    private void setSortByField(String field, Session session) {
        session.setAttribute("sortField", field);
        System.out.println("> \"sortField\" stored in session: " + field);
    }

    /**
     * Stores the List of Topics which is to be rendered of the List or SlimList Page
     *
     * @param beans
     * @param session
     */
    private void setListedTopics(Vector beans, Session session) {
        session.setAttribute("topics", beans);
        System.out.println("> \"topics\" stored in session: " + beans.size());
    }

    private void setFilterText(String value, Session session) {
        session.setAttribute("filterText", value);
        System.out.println("> \"filterText\" stored in session: " + value);
    }

    private void setFilterField(String fieldName, Session session) {
        session.setAttribute("filterField", fieldName);
        System.out.println("> \"filterField\" stored in session: " + fieldName);
    }

    private void setUseCache(Boolean flag, Session session) {
        session.setAttribute("useCache", flag.toString());
        System.out.println(">> \"useCache\" stored in session: " + flag.toString());
    }

    // ---

    private CityMapTopic getCityMap(Session session) {
        return (CityMapTopic) as.getLiveTopic((BaseTopic) session.getAttribute("cityMap"));
    }

    private BaseTopic getWorkspaceTopic(Session session) {
        return as.getLiveTopic((BaseTopic) session.getAttribute("workspace"));
    }

    private String getInstTypeID(Session session) {
        return (String) session.getAttribute("instTypeID");
    }

    private GeoObjectTopic getGeoObject(Session session) {
        return (GeoObjectTopic) as.getLiveTopic((BaseTopic) session.getAttribute("geo"));
    }

	/** Works just if sorting was once activated, uses session
     *
     * @param session
     * @return
     */
    private String getSortByField(Session session) {
        return (String) session.getAttribute("sortField");
    }

    private Vector getListedTopics(Session session) {
        return (Vector) session.getAttribute("topics");
    }

    private Boolean isCacheUsed(Session session) {
        if (session.getAttribute("useCache").equals("true")) {
            return Boolean.TRUE;
        } else {
            return Boolean.FALSE;
        }
    }

    private String getFilterField(Session session) {
        return (String) session.getAttribute("filterField");
    }


    // ********************************
    // *** Inner Comparison Classes ***
    // ********************************



    private class MyStringComparator implements Comparator {

        private String sortBy;

        public MyStringComparator(String sortBy) {
            this.sortBy = sortBy;
        }

		public int compare( Object o1, Object o2 ) {
            TopicBean beanOne = (TopicBean) o1;
            TopicBean beanTwo = (TopicBean) o2;
            //
            String valOne = beanOne.getValue(sortBy);
            String valTwo = beanTwo.getValue(sortBy);
            //
            // int i = prepairForCompare( valOne ).compareTo( prepairForCompare( valTwo ) );
            int k = ((String) valOne).compareTo((String) valTwo);
            /*
			if ( i != 0) {
				System.out.println(">>>>i "+ i +" o1: " + o1.toString() +", o2: "+ o2.toString());
			}
			if ( i == 0 ) {
				System.out.println(">>>>k "+ k +" o1: " + o1.toString() +", o2: "+ o2.toString());
			}
             */
            return k; // ( 0 != i ) ? i : k;
        }

        /**
         * Maybe not useful
         *
         * @param o
         * @return
         */
		private String prepairForCompare( Object o ) {
			return ((String)o).toLowerCase().replace( 'ä', 'a' )
										.replace( 'ö', 'o' )
										.replace( 'ü', 'u' )
										.replace( 'ß', 's' );
        }

    }


    private class MyTopicSort implements Comparator {


		public int compare( Object o1, Object o2 ) {
            BaseTopic beanOne = (BaseTopic) o1;
            BaseTopic beanTwo = (BaseTopic) o2;
            //
            String valOne = beanOne.getName();
            String valTwo = beanTwo.getName();
            //
            // int i = prepairForCompare( valOne ).compareTo( prepairForCompare( valTwo ) );
            int k = ((String) valOne).compareTo((String) valTwo);
            /*
			if ( i != 0) {
				System.out.println(">>>>i "+ i +" o1: " + o1.toString() +", o2: "+ o2.toString());
			}
			if ( i == 0 ) {
				System.out.println(">>>>k "+ k +" o1: " + o1.toString() +", o2: "+ o2.toString());
			}
             */
            return k; // ( 0 != i ) ? i : k;
        }

        /**
         * Maybe not useful
         *
         * @param o
         * @return
         */
		private String prepairForCompare( Object o ) {
			return ((String)o).toLowerCase().replace( 'ä', 'a' )
										.replace( 'ö', 'o' )
										.replace( 'ü', 'u' )
										.replace( 'ß', 's' );
        }

    }

}
