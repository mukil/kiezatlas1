package de.kiezatlas.deepamehta;

import de.deepamehta.BaseTopic;
import de.deepamehta.DeepaMehtaException;
import de.deepamehta.PresentableTopic;
import de.deepamehta.service.CorporateDirectives;
import de.deepamehta.service.Session;
import de.deepamehta.service.web.DeepaMehtaServlet;
import de.deepamehta.service.web.RequestParameter;
import de.deepamehta.service.web.WebSession;
import de.kiezatlas.deepamehta.topics.CityMapTopic;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;



/**
 * Kiezatlas 1.6.10<br>
 * Requires DeepaMehta 2.0b8-rev388
 * <p>
 * Last change: 09.11.2012<br>
 * Malte Rei&szlig;ig <br>
 * <mre@deepamehta.de>
 */
public class EhrenamtServlet extends DeepaMehtaServlet implements KiezAtlas {



	// --
	// --- KiezAtlas Service Settings / New berlin.de City Map Servlet
	// --



	private final String SERVICE_URL = "http://www.kiezatlas.de/rpc/";
	// private final String SERVICE_URL = "http://localhost:8080/kiezatlas/rpc/";
	private final String HTTP_KA_CONNECT_CHARSET = "ISO-8859-15";
    //
	private final String EHRENAMTS_PROJECT_MAP_ID = "t-331302";
	private final String EHRENAMTS_EVENT_MAP_ID = "t-453286";

	String topicmapId = "is-set-to-current-topicmap-id-of-request"; // used by new getLastModified impl



	protected String performAction(String action, RequestParameter params, Session session,
									CorporateDirectives directives) throws ServletException {
		if (action == null) {
			try {
				String pathInfo = params.getPathInfo();
				// error check
				if (pathInfo == null || pathInfo.length() == 1) {
					throw new DeepaMehtaException("Fehler in URL");
				}
				String[] elements = pathInfo.split("/");
				Hashtable requested = parseRequestedPath(elements);
				if (requested != null) {
					session.setAttribute("originId", requested.get("linkTo")); // linkTo here for backwards compatibility rsn
					session.setAttribute("topicId", requested.get("geo"));
					session.setAttribute("categories", requested.get("categories"));
					session.setAttribute("baseLayer", requested.get("layer"));
					session.setAttribute("critIndex", requested.get("criteria"));
					session.setAttribute("searchTerm", requested.get("search"));
				}
				// overrides pathinfo url settings just parsed.. for backwards compatibility reasons
				// application states represented in URL - just passing params to javascript
				Integer critIndex = 0;
				if (params.getParameter("critId") != null) {
					critIndex = Integer.parseInt(params.getParameter("critId"))-1;
				} // [0]
				if (params.getParameter("linkTo") != null) session.setAttribute("originId", params.getParameter("linkTo")); // linkTo here for backwards compatibility rsn
				if (params.getParameter("topicId") != null) session.setAttribute("topicId", params.getParameter("topicId"));
				if (params.getParameter("catIds") != null) session.setAttribute("categories", params.getParameter("catIds"));
				if (params.getParameter("baseLayer") != null) session.setAttribute("baseLayer", params.getParameter("baseLayer"));
				if (params.getParameter("search") != null) session.setAttribute("searchTerm", params.getParameter("search"));
				if (session.getAttribute("critIndex") == null) session.setAttribute("critIndex", critIndex);
                //
				String alias = pathInfo.substring(1);
				if (pathInfo.indexOf("&") != -1) {
					alias = pathInfo.substring(1, pathInfo.indexOf("&"));
				} else if (pathInfo.indexOf("/", 1) != -1) {
					// if / behind mapname... slice map-alias out
					alias = elements[1];
				}
				session.setAttribute("mapAlias", alias);
				BaseTopic mapTopic = CityMapTopic.lookupCityMap(alias, true, as); // throwIfNotFound=true
				setCityMap(mapTopic, session);
                // Ehrenamts Meta-Navigation Flags
                boolean eventMap = (mapTopic.getID().equals(EHRENAMTS_EVENT_MAP_ID)) ? true : false;
                boolean projectMap = (mapTopic.getID().equals(EHRENAMTS_PROJECT_MAP_ID)) ? true : false;
                session.setAttribute("onEventMap", eventMap);  // Ehrenamts Event City Map Flag
                session.setAttribute("onProjectMap", projectMap); // Ehrenamts Project City Map Flag
                // Check for Mobile Browser
                // TODO: Access HttpRequest-Object
                // String browser = request.getHeader("X-Mobile");
				if (!CityMapTopic.isProtected(mapTopic, as)) {
					//
					initCityMap(session);
					return PAGE_EHRENAMTS_MAP;
				} else {
					return PAGE_MAP_LOGIN;
				}
			} catch (DeepaMehtaException e) {
				System.out.println("*** NewAtlasServlet.performAction(): " + e);
				session.setAttribute("error", e.getMessage());
				return PAGE_ERROR;
			}
		}

		// session timeout?
		if (getCityMap(session) == null) {
			System.out.println("*** Session Expired ***");
			session.setAttribute("error", "Timeout: Kiezatlas wurde mehr als " +
				((WebSession) session).session.getMaxInactiveInterval() / 60 + " Minuten nicht benutzt");
			return PAGE_ERROR;
		}

		// login
		if (action.equals(ACTION_TRY_LOGIN)) {
			String password = params.getValue("password");
			if (CityMapTopic.passwordCorrect(getCityMap(session), as, password)) {
				initCityMap(session);
				return PAGE_EHRENAMTS_MAP;
			} else {
				return PAGE_MAP_LOGIN;
			}
		} else {
            return PAGE_ERROR;
        }
    }

    protected void preparePage(String page, RequestParameter params, Session session, CorporateDirectives directives) {
		//
	}

	protected long getLastModified(HttpServletRequest req) {
		String dateProperty = as.getTopicProperty(this.topicmapId, 1, PROPERTY_LAST_UPDATED);
		long lastModified = new Date().getTime();
		if (!dateProperty.equals("")) lastModified = new Date(Long.parseLong(dateProperty)).getTime();
		return lastModified;
	}



	// *****************
	// *** Utilities ***
	// *****************

	private String getMapTopics(String mapId, String workspaceId, Session session) {
		String result = null;
		try {
			// Send data
			URL url = new URL(SERVICE_URL);
			String query = "{\"method\": \"getMapTopics\", \"params\": [\"" + mapId + "\" , \"" + workspaceId + "\"]}";
			URLConnection connection = new URL(SERVICE_URL).openConnection();
			connection.setDoOutput(true); // Triggers POST.
			// connection.setRequestProperty("Accept-Charset", charset);
			connection.setRequestProperty("Content-Type", "application/json;charset=" + HTTP_KA_CONNECT_CHARSET);
			OutputStream output = null;
			output = connection.getOutputStream();
			output.write(query.getBytes(HTTP_KA_CONNECT_CHARSET));
			// Get the response
			BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream(), HTTP_KA_CONNECT_CHARSET)); //
			String line = "";
			if (rd.ready()) {
				line = rd.readLine();
			}
			result = line;
			rd.close();
			return result;
		} catch(UnknownHostException uke) {
			System.out.println("*** [ATLAS] couldn't load CityMapData from " + SERVICE_URL + " cause is: " + uke.getCause());
			return null;
			// done();
		} catch (Exception ex) {
			System.out.println("*** [ATLAS] Servletencountered problem: " + ex.getMessage());
			return null;
		}
	}

	private String getKiezCriterias(String mapId, Session session) {
		String result = null;
		try {
			// Send data
			URL url = new URL(SERVICE_URL);
			String query = "{\"method\": \"getWorkspaceCriterias\", \"params\": [\"" + mapId + "\"]}";
			// url.s
			URLConnection connection = new URL(SERVICE_URL).openConnection();
			connection.setDoOutput(true); // Triggers POST.
			// connection.setRequestProperty("Accept-Charset", charset);
			connection.setRequestProperty("Content-Type", "application/json;charset=" + HTTP_KA_CONNECT_CHARSET);
			OutputStream output = null;
			output = connection.getOutputStream();
			output.write(query.getBytes(HTTP_KA_CONNECT_CHARSET));
			// Get the response
			BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream(), HTTP_KA_CONNECT_CHARSET)); //
			String line = "";
			if (rd.ready()) {
				line = rd.readLine();
			}
			result = line;
			return result;
		} catch(UnknownHostException uke) {
			System.out.println("*** [ATLAS] could not load CityMap-Data from " + SERVICE_URL + " message is: " + uke.getCause());
			return null;
			// done();
		} catch (UnsupportedEncodingException ex) {
			System.out.println("*** [AtlasServlet] Servlet encountered unsupportedEncoding : " + ex.getMessage());
			return null;
		} catch (IOException ex) {
			System.out.println("*** [AtlasServlet] Servlet encountered ioException : " + ex.getMessage());
			return null;
		}
	}

	private String getWorkspaceHomepage(String workspaceId, Session session) {
		String homepageURL = "";
		//
		BaseTopic homepage = as.getRelatedTopic(workspaceId, KiezAtlas.ASSOCTYPE_HOMEPAGE_LINK,
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



	// *************************
	// *** Session Utilities ***
	// *************************



	// --- Methods to maintain data in the session

	private void initCityMap(Session session) {
		//
		BaseTopic map = getCityMap(session);
		BaseTopic workspace = as.getTopicmapOwner(map.getID());
		session.setAttribute("workspace", workspace);
		String mapTopics = getMapTopics(map.getID(), workspace.getID(), session);
		String workspaceCriterias = getKiezCriterias(map.getID(), session);
		// String workspaceInfos = getWorkspaceInfos(workspaceId, session);
		session.setAttribute("mapTopics", mapTopics);
		session.setAttribute("workspaceCriterias", workspaceCriterias);
		session.setAttribute("workspaceImprint", getWorkspaceImprint(workspace.getID(), session));
		session.setAttribute("workspaceHomepage", getWorkspaceHomepage(workspace.getID(), session));
		session.setAttribute("workspaceLogo", getWorkspaceLogo(workspace.getID(), session));
	}

	private void setCityMap(BaseTopic map, Session session) {
		String mapImage = as.getCorporateWebBaseURL() + FILESERVER_BACKGROUNDS_PATH +
			as.getTopicProperty(map, PROPERTY_BACKGROUND_IMAGE);
		session.setAttribute("map", map);
		session.setAttribute("mapImage", mapImage);
		System.out.println("  > \"map\" stored in session: " + map);
		System.out.println("  > \"mapImage\" stored in session: " + mapImage);
	}

	// ---

	/**
	 * @param	hotspots should be a vector of vector of PresentableTopics
	 * @param	cluster is first an empty vector, afterwards it should be a vector of clusters
	 */
	public void makeCluster(Vector hotspots, Vector clusters) {
		Iterator vectorOfHotspots = hotspots.iterator();
		while ( vectorOfHotspots.hasNext() ) {
			// for size of hotspot vectors in vector hotspot
			Vector currentHotspots = (Vector) vectorOfHotspots.next();
			Iterator presentableTopics = currentHotspots.iterator();
			// ### System.out.println("size of current Hotspot " + currentHotspots.toString() +" : "+ currentHotspots.size());
			// jump over the string, and make sure something is there as first item in our vector
			if (presentableTopics.hasNext()) presentableTopics.next();
			while( presentableTopics.hasNext() ) {
				// the first element is always the icon path of the following hotspots
				PresentableTopic currentPT = (PresentableTopic) presentableTopics.next();
				Cluster foundCluster = findAndCheckClusters(currentPT, clusters);
				if (foundCluster != null) {
					// addPresentable, checks for doubles
					foundCluster.addPresentable(currentPT);
				} else {
					// es gibt noch kein cluster oder es wurde kein passendes gefunden also suchen,
					// nach dem ersten auftreten von dem gleichen Point in allen hotspots
					PresentableTopic foundPT = findPT(currentPT, hotspots);
					if ( foundPT != null ) {
						// create Cluster, with two points, if they don't have the same ID add the respective dm server icon path
						// ### System.out.println("created new cluster with " + currentPT.getID() + " and " + foundPT.getID());
						clusters.add(new Cluster(currentPT, foundPT, as.getCorporateWebBaseURL() + FILESERVER_ICONS_PATH));
					}
				}
			}
		}
	}

	private Cluster findAndCheckClusters(PresentableTopic currentPT, Vector clusters){
			for (int c=0; c < clusters.size(); c++) {
				// checking each cluster for point of current pt
				Cluster currentCluster = (Cluster) clusters.get(c);
				// ### System.out.println(currentCluster.presentables.size() + " topics in current Cluster" + currentCluster.getPoint());
				// ### System.out.println("point to Check " + currentPT.getName() +", "+ currentPT.getGeometry());
				if (currentCluster.getPoint().equals(currentPT.getGeometry())) {
					// ### System.out.println("found cluster to return for adding pt");
					return currentCluster;
				}
			}
			return null;
	}

	private PresentableTopic findPT(PresentableTopic pt, Vector hotspots) {
		for(int p=0; p < hotspots.size(); p++){
			Vector scnd = (Vector)hotspots.get(p);
			for(int o=1; o < scnd.size(); o++){
				PresentableTopic toCheck = (PresentableTopic) scnd.get(o);
				if (toCheck.getGeometry().equals(pt.getGeometry()) & (!toCheck.getID().equals(pt.getID()))) {
					// if the topics don't have the same id but have the same point, they belong together
					return toCheck;
				}
			}
		}
		return null;
	}

	private BaseTopic getCityMap(Session session) {
		return (BaseTopic) session.getAttribute("map");
	}

	private SearchCriteria[] getCriterias(Session session) {
		return (SearchCriteria[]) session.getAttribute("criterias");
	}

	private SearchCriteria getCriteria(int critNr, Session session) {
		return getCriterias(session)[critNr];
	}

    // ---

	private Hashtable parseRequestedPath(String[] elements) {
		Hashtable result = new Hashtable();
		for (int i = 0; i < elements.length; i++) {
			String key = elements[i];
			try { // ### if one nomen has no attributes, the whole request fails
				if (key.equals("layer")) {
					result.put("layer", elements[i+1]);
				} else if (key.equals("p")) {
				 	result.put("geo", elements[i+1]);
				} else if (key.equals("linkTo")) {
					result.put("linkTo", elements[i+1]);
				} else if (key.equals("categories")) {
					result.put("categories", elements[i+1]);
				} else if (key.equals("criteria")) {
					result.put("criteria", Integer.parseInt(elements[i+1])-1);
				} else if (key.equals("search")) {
					result.put("search", elements[i+1]);
				} else {
					// skip this..
				}
			} catch (ArrayIndexOutOfBoundsException ex) {
				return null;
			}
		}
		return result;
	}

}
