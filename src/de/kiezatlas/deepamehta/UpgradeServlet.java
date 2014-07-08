package de.kiezatlas.deepamehta;

import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;

import de.deepamehta.BaseTopic;
import de.deepamehta.DeepaMehtaException;
import de.deepamehta.service.CorporateDirectives;
import de.deepamehta.service.Session;
import de.deepamehta.service.web.DeepaMehtaServlet;
import de.deepamehta.service.web.RequestParameter;
import de.deepamehta.topics.TypeTopic;

import de.kiezatlas.deepamehta.topics.CityMapTopic;
import de.kiezatlas.deepamehta.topics.GeoObjectTopic;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

import javax.servlet.ServletException;



/**
 * Kiezatlas 1.6.10<br/>
 * Requires DeepaMehta 2.0b8-rev388
 * <p>
 * Last change: 22.04.2014<br/>
 * Malte Rei&szlig;ig <br/>
 * <mre@deepamehta.de>
 * </p>
 */
public class UpgradeServlet extends DeepaMehtaServlet implements KiezAtlas {



    // --
    // --- KiezAtlas 1 Upgrade Servlet
    // --

    private final static String KA2_SERVICE_URL = "http://localhost:8182";
    //
    private final static String KA2_DEFAULT_WS_TOPIC_ID = "988";

    private static final String KA2_ADMIN_PASSWORD = "";


    // --- Instance Variables

    private String validSessionId = null;

    public static Map<String, String> PROPERTY_FACET_MAP = new HashMap();


        protected String performAction(String action, RequestParameter params, Session session,
                                        CorporateDirectives directives) throws ServletException {
        if (action == null) {
            try {
                String pathInfo = params.getPathInfo();
				// error check
				if (pathInfo == null || pathInfo.length() == 1) {
					throw new DeepaMehtaException("Fehler in URL");
				}
				// WorkspaceServlet Method
                // String alias = pathInfo.substring(1);
                // Hashtable aliasProperty = new Hashtable();
                // aliasProperty.put(PROPERTY_WORKSPACE_ALIAS, alias);
                // BaseTopic workspace = as.getTopic(TOPICTYPE_WORKSPACE, aliasProperty, null, directives);
                System.out.println("> UpgradeServlet pathInfo \"" + pathInfo + "\"");
                String[] elements = pathInfo.split("/");
                Hashtable requested = parseRequestedPath(elements);
                String workspaceId = "";
                if (requested != null) {
                    workspaceId = (String) requested.get("workspace");
                }
                BaseTopic workspace = as.getLiveTopic(workspaceId, 1);
                if (workspace != null) {
                    session.setAttribute("workspace", workspace);
                } else {
                    session.setAttribute("html", "A Workspace with this ID could not be found.");
                    return PAGE_ERROR;
                }
                return PAGE_UPGRADE_LOGIN;

                /* String alias = pathInfo.substring(1);
                if (pathInfo.indexOf("&") != -1) {
                    alias = pathInfo.substring(1, pathInfo.indexOf("&"));
                } else if (pathInfo.indexOf("/", 1) != -1) {
                    // if / behind mapname... slice map-alias out
                    alias = elements[1];
                }
                if (session != null) {
                    BaseTopic mapTopic = CityMapTopic.lookupCityMap(alias, true, as); // throwIfNotFound=true
                    if (!workspaceId.isEmpty()) {
                        postCityMap(workspaceId, alias, mapTopic.getID(), mapTopic.getName());
                    }
                    session.setAttribute("mapAlias", mapTopic.getName());
                    System.out.println("> UpgradeServlet setting city-map to \"" + mapTopic.getName() + "\"");
                } else {
                    System.out.println("> UpgradeServlet Session is NULL.. ");
                } **/
                // return PAGE_UPGRADE_CITYMAP;
            } catch (DeepaMehtaException e) {
                System.out.println("*** UpgradeServlet error during setting city-map " + e);
                session.setAttribute("error", e.getMessage());
                return PAGE_ERROR;
            }
        }

        // login
        if (action.equals(ACTION_TRY_LOGIN)) {
            System.out.println("Trying to login \"root\" to workspace-migration servlet.");
            String password = params.getValue(PROPERTY_PASSWORD);
            // ### check if user credentials are allowed..
            Hashtable userProperty = new Hashtable();
            userProperty.put(PROPERTY_USERNAME, "root");
            userProperty.put(PROPERTY_PASSWORD, password);
            BaseTopic user = null;
            try {
                user = as.getTopic(TOPICTYPE_USER, userProperty, null, directives);
                if (!as.getTopicProperty(user, PROPERTY_PASSWORD).equals(password)) {
                    System.out.println("*** ERROR: passwords do not match/ no passsword given for .." + user.getName());
                    session.setAttribute("error", "Login incorrect");
                    return PAGE_ERROR;
                }
            } catch(DeepaMehtaException ex) {
                System.out.println("User is NOT available or credentials are not correct.. ");
                session.setAttribute("error", "Login incorrect");
                return PAGE_ERROR;
            }
            // ### and is user related to this workpsace in the role of submitter
            if (user != null) {
                session.setAttribute("user", user);
                // ### generate form for the GeoObject-Topictype of this workspace..
                // GeoObjectTopic topic = as.createTopic(, "");
                // load current workspace..
                BaseTopic ws = (BaseTopic) session.getAttribute("workspace");
                // load all citymaps published in this workspace...BaseTopic geoType = getWorkspaceGeoType(ws.getID())
                BaseTopic wsMap = as.getRelatedTopic(ws.getID(), ASSOCTYPE_AGGREGATION, TOPICTYPE_TOPICMAP, 2, true);
                Vector publishedMaps = cm.getViewTopics(wsMap.getID(), 1, TOPICTYPE_CITYMAP);
                if (publishedMaps.size() >= 1) {
                    session.setAttribute("cityMaps", publishedMaps);
                }
                //
                return PAGE_UPGRADE_LIST;
            } else {
                System.out.println("User is NOT available or credentials are not correct.. ");
            }
            return PAGE_UPGRADE_LOGIN;
        } else if (action.equals(ACTION_MIGRATE_CITYMAP)) {

            BaseTopic ws = (BaseTopic) session.getAttribute("workspace");
            System.out.println("--- Woooohoow! Somebody chose to upgrade citymaps of workspace \""+ws.getName()+"\"");
            String[] cityMaps = params.getParameterValues("cityMap");
            for (int i=0; i<cityMaps.length; i++) {
                String cityMapId = cityMaps[i];
                BaseTopic cityMapTopic = as.getLiveTopic(cityMapId, 1);
                String cityMapAlias = as.getTopicProperty(cityMapTopic, PROPERTY_WEB_ALIAS);
                System.out.println("    CityMap to be upgraded is \"" + cityMapTopic.getName()
                        + "\" with web-alias \"" + cityMapAlias + "\"");
                postCityMap(ws.getID(), cityMapAlias, cityMapId, cityMapTopic.getName());
            }
            return PAGE_UPGRADE_LIST;

        } else {
            return PAGE_ERROR;
        }
    }

    protected void preparePage(String page, RequestParameter params, Session session, CorporateDirectives directives) {
        System.out.println("UpgradeSerlvet preparePage ... \"" + page + "\"");
    }



    // --
    // --- Upgrade Service Utilities
    // --



    private void postCityMap (String workspaceId, String mapAlias, String mapTopicId, String mapName) {
        BaseTopic geoType = (BaseTopic) getWorkspaceGeoType(workspaceId);
        SearchCriteria[] criterias = getWorkspaceCriterias(mapTopicId);
        System.out.println(">>> Upgrading City Map " + mapName + " with Geo Type: " + geoType.getName()
                + " ("+geoType.getType()+")");
        Vector allTopics = cm.getTopics(geoType.getID(), new Hashtable(), mapTopicId);
        System.out.println(">>> " + allTopics.size() +" within current map");
        if (getNewHTTPSession(KA2_ADMIN_PASSWORD)) {
            System.out.println("    Established valid HTTPSession \"" + validSessionId
                + "\" starting data-migration to " + KA2_SERVICE_URL);
            for (int i = 0; i< allTopics.size(); i++) {
                BaseTopic topic = null;
                try {
                    topic = (BaseTopic) allTopics.get(i);
                    GeoObjectTopic geoObject = (GeoObjectTopic) as.getLiveTopic(topic);
                    //
                    geoObject.postRemoteGeoObject(mapTopicId, mapAlias, workspaceId, criterias, validSessionId);
                } catch (DeepaMehtaException ex) {
                    System.out.println(" --------");
                    System.out.println(" WARNING: Skipped processing topic \""+topic.getName()+"\" ("+topic.getID()+") "
                            + "because of " + ex.getLocalizedMessage());
                    System.out.println(" --------");
                }
            }
        }
    }

    private boolean getNewHTTPSession(String secret) {
        String sessionId = null;
        try {
            // 1) POST-Authentication Request
            HttpURLConnection connection = (HttpURLConnection) new URL(KA2_SERVICE_URL
                    + "/accesscontrol/login").openConnection();
            connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            connection.setRequestMethod("POST");
            String auth_values = "admin:" + secret;
            String authentication = "Basic " + Base64.encode(auth_values.getBytes());
            connection.setRequestProperty("Authorization", authentication);
            try {
                if (connection.getResponseCode() == HttpURLConnection.HTTP_NO_CONTENT) {
                    String cookie = connection.getHeaderField("Set-Cookie");
                    String[] cookieValues = cookie.split(";");
                    for (int i=0; i < cookieValues.length; i++) {
                        String[] pair = cookieValues[i].split("=");
                        if (pair[0].equals("JSESSIONID")) sessionId = pair[1];
                    }
                    System.out.println("      Authentication successfull: JSESSIONID is " + sessionId);
                    validSessionId = sessionId;
                    return (sessionId != null || !sessionId.isEmpty()) ? true : false;
                } else {
                    System.out.println("      Authentication NOT successfull: " + connection.getResponseCode() + " " +
                            connection.getResponseMessage() + " Cookie: " + connection.getHeaderField("Set-Cookie"));
                    return false;
                }
            } catch (Exception je) {
                //
                je.printStackTrace();
                System.out.println("*** [KA 1 Upgrade] error getting HTTPSession from KA 2");
                return false;
            }
        } catch (UnknownHostException uke) {
            System.out.println("*** [KA 1 Upgrade] connecting to " + KA2_SERVICE_URL + " cause is: " + uke.getCause());
            return false;
        } catch (Exception ex) {
            System.out.println("*** [KA 1 Upgrade] getHTTPSession encountered problem: " + ex.getMessage());
            return false;
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
                String derivedOne = (String) subtypes.get(a);
                if (derivedOne.equals(topic.getID())) {
                    return topic;
                }
            }
        }
        return null;
    }

    private Vector getPublishedCityMaps (String workspaceId) {
        BaseTopic wsMap = as.getRelatedTopic(workspaceId, ASSOCTYPE_AGGREGATION, TOPICTYPE_TOPICMAP, 2, true);
        Vector cityMaps = cm.getViewTopics(wsMap.getID(), 1, TOPICTYPE_CITYMAP);
        System.out.println("   > found : " + cityMaps.size() + " citymaps");
        return cityMaps;
    }

    private SearchCriteria[] getWorkspaceCriterias (String cityMapId) {
        CityMapTopic mapTopic = (CityMapTopic) as.getLiveTopic(cityMapId, 1);
        SearchCriteria[] crits = mapTopic.getSearchCriterias();
        return crits;
    }

    private String getWorkspaceHomepage(String workspaceId, Session session) {
        String homepageURL = "";
        //
        BaseTopic homepage = as.getRelatedTopic(workspaceId, ASSOCTYPE_HOMEPAGE_LINK,
                TOPICTYPE_WEBPAGE, 2, true);
        if (homepage != null) homepageURL = as.getTopicProperty(homepage, PROPERTY_URL);
        return homepageURL;
    }

    private String getWorkspaceImprint(String workspaceId, Session session) {
        String impressumURL = "";
        BaseTopic impressum = as.getRelatedTopic(workspaceId, KiezAtlas.ASSOCTYPE_IMPRESSUM_LINK,
                TOPICTYPE_WEBPAGE, 2, true);
        if (impressum != null) impressumURL = as.getTopicProperty(impressum, PROPERTY_URL);
        return impressumURL;
    }

    private String getWorkspaceLogo(String workspaceId, Session session) {
        String logoURL = "";
        BaseTopic logo = as.getRelatedTopic(workspaceId, ASSOCTYPE_ASSOCIATION, TOPICTYPE_IMAGE, 2, true);
        if (logo != null) logoURL = as.getTopicProperty(logo, PROPERTY_FILE);
        return logoURL;
    }

    private Hashtable parseRequestedPath(String[] elements) {
        Hashtable result = new Hashtable();
        for (int i = 0; i < elements.length; i++) {
            String key = elements[i];
            try { // ### if one nomen has no attributes, the whole request fails
                if (key.equals("workspace")) {
                    result.put("workspace", elements[i+1]);
                } else {
                    // skip this..
                }
                /*  else if (key.equals("p")) {
                    result.put("geo", elements[i+1]);
                } else if (key.equals("linkTo")) {
                    result.put("linkTo", elements[i+1]);
                } else if (key.equals("categories")) {
                    result.put("categories", elements[i+1]);
                } else if (key.equals("criteria")) {
                    result.put("criteria", Integer.parseInt(elements[i+1])-1);
                } else if (key.equals("search")) {
                    result.put("search", elements[i+1]);
                }  */
            } catch (ArrayIndexOutOfBoundsException ex) {
                return null;
            }
        }
        return result;
    }

}
