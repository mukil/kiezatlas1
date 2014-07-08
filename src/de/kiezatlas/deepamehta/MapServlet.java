package de.kiezatlas.deepamehta;

import java.awt.Dimension;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.swing.ImageIcon;

import de.deepamehta.BaseTopic;
import de.deepamehta.DeepaMehtaException;
import de.deepamehta.PresentableTopic;
import de.deepamehta.service.CorporateDirectives;
import de.deepamehta.service.Session;
import de.deepamehta.service.TopicBean;
import de.deepamehta.service.web.DeepaMehtaServlet;
import de.deepamehta.service.web.RequestParameter;
import de.deepamehta.service.web.WebSession;
import de.deepamehta.topics.TypeTopic;

import de.kiezatlas.deepamehta.topics.CityMapTopic;
import de.kiezatlas.deepamehta.topics.GeoObjectTopic;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.Date;



/**
 * Kiezatlas 1.6.10<br>
 * Requires DeepaMehta 2.0b8-rev388
 * <p>
 * Last change: 09.11.2012<br>
 * Malte Rei&szlig;ig <br>
 * <mre@deepamehta.de>
 */
public class MapServlet extends DeepaMehtaServlet implements KiezAtlas {


	// --
	// --- KiezAtlas Service Settings / Kiezatlas Labs City Map Servlet
	// --


    private final String SERVICE_URL = "http://www.kiezatlas.de/rpc/";
	// private final String SERVICE_URL = "http://localhost:8080/kiezatlas/rpc/";
	// private final String urlStr = "http://212.87.44.116:8080/rpc/";
	private final String charset = "ISO-8859-1";

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
					session.setAttribute("originId", requested.get("linkTo")); // linkTo here for backwards compatibil.
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
				if (params.getParameter("linkTo") != null) session.setAttribute("originId", params.getParameter("linkTo"));
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
				this.topicmapId = mapTopic.getID();
				if (!CityMapTopic.isProtected(mapTopic, as)) {
					//
					initCityMap(session);
					return PAGE_MAP_ATLAS;
				} else {
					return PAGE_MAP_LOGIN;
				}
			} catch (DeepaMehtaException e) {
				System.out.println("*** BrowseServlet.performAction(): " + e);
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
				return PAGE_MAP_ATLAS;
			} else {
				return PAGE_MAP_LOGIN;
			}
		// search
		} else if (action.equals(ACTION_SEARCH)) {
			setSearchMode(SEARCHMODE_BY_NAME, session);
			setSearchValue(params.getValue("search"), session);
			return PAGE_GEO_LIST;
		// show
		} else if (action.equals(ACTION_SHOW_CATEGORIES)) {
			String critNr = params.getValue("critNr");
			setSearchMode(critNr, session);
			session.setAttribute("defaultCriteria", critNr);
			return PAGE_CATEGORY_LIST;
		// select
		} else if (action.equals(ACTION_SELECT_CATEGORY)) {
			Vector selCats = getSelectedCats(session);
			String catID = params.getValue("id");
			toggle(selCats, catID);
			return PAGE_CATEGORY_LIST;
		// search by
		} else if (action.equals(ACTION_SEARCH_BY_CATEGORY)) {
			// needed for "cross-links" ### criterias in geo info are not rendered as links anymore
			String critNr = params.getValue("critNr");
			if (critNr != null) {
				setSearchMode(critNr, session);
			}
			//
			String catID = params.getValue("id");
			setSearchValue(as.getLiveTopic(catID, 1).getName(), session);
			// ### setSearchMode(SEARCHMODE_BY_CATEGORY, session);		// needed for "cross-links"
			Vector selCats = getSelectedCats(session);
			switchOn(selCats, catID);
			return PAGE_GEO_LIST;
		// info
		} else if (action.equals(ACTION_SHOW_GEO_INFO)) {
			// ### should also set the current HotspotMarker (e.g. after searchByCategory)
			String geoID = params.getValue("id");
			setSelectedGeo(geoID, session);
			TopicBean topicBean = as.createTopicBean(geoID, 1);
			session.setAttribute("topicBean", topicBean);
			String imagePath = as.getCorporateWebBaseURL() + FILESERVER_IMAGES_PATH;
			session.setAttribute("imagePath", imagePath);
			GeoObjectTopic geo = (GeoObjectTopic) as.getLiveTopic(geoID, 1);
			boolean isForumActivated = geo.isForumActivated();
			session.setAttribute("forumActivition", isForumActivated ? SWITCH_ON : SWITCH_OFF);
			if (isForumActivated) {
				session.setAttribute("commentCount", new Integer(geo.getComments().size()));
			}
			return PAGE_GEO_INFO;
		}
		return PAGE_ERROR;
	}

	protected void preparePage(String page, RequestParameter params, Session session, CorporateDirectives directives) {
		if (page.equals(PAGE_CATEGORY_LIST)) {
			Vector categories = cm.getTopics(getCurrentCriteria(session).criteria.getID());
			Vector selectedCats = getSelectedCats(session);
			session.setAttribute("categories", categories);
			session.setAttribute("selectedCats", selectedCats);
			// disable enumeration rendering
			session.setAttribute("enumerationFlag", false);
			// hotspots
			setCategoryHotspots(session);
			// clear marker
			setSelectedGeo(null, session);
		} else if (page.equals(PAGE_GEO_LIST)) {
			// institutions
			String mapID = getCityMap(session).getID();
			String instTypeID = getInstitutionType(session).getID();
			String searchMode = getSearchMode(session);
			Vector insts;
			// enable the rendering of enumeration
			session.setAttribute("enumerationFlag", true);
			if (searchMode.equals(SEARCHMODE_BY_NAME)) {
				insts = cm.getViewTopics(mapID, 1, instTypeID, getSearchValue(session));
				// hotspots
				setHotspots(insts, ICON_HOTSPOT, session);
				session.setAttribute("selectedCatId", "");
			} else {
				String catID = params.getValue("id");
				insts = cm.getRelatedViewTopics(mapID, 1, catID, ASSOCTYPE_ASSOCIATION, instTypeID, 1);	// ### copy in setCategoryHotspots()
				// hotspots + return the index of the current CatID in the multi-dimensional hotspot vector
				int catIndex = setSearchedCategoryHotspots(session, catID);
				session.setAttribute("selectedCatId", ""+catIndex+"");
			}
			session.setAttribute("institutions", insts);
			// categories & addresses
			Hashtable categories = new Hashtable();
			Hashtable addresses = new Hashtable();
			for (int i = 0; i < insts.size(); i++) {
				BaseTopic t = (BaseTopic) insts.elementAt(i);
				try {
					GeoObjectTopic geo = (GeoObjectTopic) as.getLiveTopic(t);
					// categories
					String critTypeID = getCriteria(0, session).criteria.getID();
					categories.put(geo.getID(), geo.getCategories(critTypeID));
					// address
					BaseTopic address = geo.getAddress();
					// if no related address put new hashtable in it for property city
					Hashtable addressProps = address != null ? as.getTopicProperties(address) : new Hashtable();
					addressProps.put(PROPERTY_CITY, geo.getCity());
					addresses.put(geo.getID(), addressProps);
				} catch (ClassCastException e) {
					System.out.println("*** BrowseServlet.preparePage(): " + t + ": " + e);
					// ### happens if geo object type is not up to date
				}
			}
			session.setAttribute("categories", categories);
			session.setAttribute("addresses", addresses);
			// clear marker
			setSelectedGeo(null, session);
		} else if (page.equals(PAGE_GEO_FORUM)) {
			session.setAttribute("geoComments", getSelectedGeo(session).getCommentBeans());
		}
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
			connection.setRequestProperty("Content-Type", "application/json;charset=" + charset);
			OutputStream output = null;
			output = connection.getOutputStream();
			output.write(query.getBytes(charset));
			// Get the response
			BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream(), charset)); //
			String line = "";
			if (rd.ready()) {
				line = rd.readLine();
			}
			result = line;
			rd.close();
			return result;
		} catch(UnknownHostException uke) {
			System.out.println("*** [MapServlet] could not load the json data to import from " + SERVICE_URL + " message is: " + uke.getMessage());
			return null;
			// done();
		} catch (Exception ex) {
			System.out.println("*** [MapServlet] Servleten countered problem: " + ex.getMessage());
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
			connection.setRequestProperty("Content-Type", "application/json;charset=" + charset);
			OutputStream output = null;
			output = connection.getOutputStream();
			output.write(query.getBytes(charset));
			// Get the response
			BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream(), charset)); //
			String line = "";
			if (rd.ready()) {
				line = rd.readLine();
			}
			result = line;
			return result;
		} catch(UnknownHostException uke) {
			System.out.println("*** [MapServlet] could not load the json data to import from " + SERVICE_URL + " message is: " + uke.getMessage());
			return null;
			// done();
		} catch (UnsupportedEncodingException ex) {
			System.out.println("*** [MapServlet] Servlet encountered unsupportedEncoding : " + ex.getMessage());
			return null;
		} catch (IOException ex) {
			System.out.println("*** [MapServlet] Servlet encountered ioException : " + ex.getMessage());
			return null;
		}
	}

	private void toggle(Vector topicIDs, String topicID) {
		if (topicIDs.contains(topicID)) {
			topicIDs.removeElement(topicID);
		} else {
			topicIDs.addElement(topicID);
		}
	}

	private void switchOn(Vector topicIDs, String topicID) {
		if (!topicIDs.contains(topicID)) {
			topicIDs.addElement(topicID);
		}
	}

	private String getWorkspaceHomepage(String workspaceId, Session session) {
		String homepageURL = "";
		//
		BaseTopic homepage = as.getRelatedTopic(workspaceId, KiezAtlas.ASSOCTYPE_HOMEPAGE_LINK, TOPICTYPE_WEBPAGE, 2, true);
		if (homepage != null) homepageURL = as.getTopicProperty(homepage, PROPERTY_URL);
		return homepageURL;
	}

	private String getWorkspaceImprint(String workspaceId, Session session) {
		String impressumURL = "";
		BaseTopic impressum = as.getRelatedTopic(workspaceId, KiezAtlas.ASSOCTYPE_IMPRESSUM_LINK, TOPICTYPE_WEBPAGE, 2, true);
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

	private void setSearchMode(String searchMode, Session session) {
		session.setAttribute("searchMode", searchMode);
		System.out.println("> \"searchMode\" stored in session: " + searchMode);
	}

	private void setSearchValue(String searchValue, Session session) {
		session.setAttribute("searchValue", searchValue);
		System.out.println("> \"searchValue\" stored in session: " + searchValue);
	}

	private void initInstitutaionType(Session session) {
		BaseTopic instType = ((CityMapTopic) as.getLiveTopic(getCityMap(session))).getInstitutionType();	// ### ugly
		session.setAttribute("instType", instType);
		System.out.println(">>> \"instType\" stored in session: " + instType);
	}

	private void initSearchCriterias(Session session) {
		SearchCriteria[] criterias = ((CityMapTopic) as.getLiveTopic(getCityMap(session))).getSearchCriterias();	// ### ugly
		session.setAttribute("criterias", criterias);
	}

	private void initShapeTypes(Session session) {
		Vector st = ((CityMapTopic) as.getLiveTopic(getCityMap(session))).getShapeTypes();	// ### ugly
		System.out.println(">>> there are " + st.size() + " shape types:");
		Vector shapeTypes = new Vector();
		for (int i = 0; i < st.size(); i++) {
			TypeTopic shapeType = (TypeTopic) as.getLiveTopic((BaseTopic) st.elementAt(i));
			shapeTypes.addElement(new ShapeType(shapeType.getID(), shapeType.getPluralNaming(),
				as.getTopicProperty(shapeType, PROPERTY_COLOR), false));	// isSelected=false
			System.out.println("  > " + shapeType.getName());
		}
		session.setAttribute("shapeTypes", shapeTypes);
	}

	private void initStylesheet(Session session) {
		BaseTopic stylesheet = ((CityMapTopic) as.getLiveTopic(getCityMap(session))).getStylesheet();		// ### ugly
		if (stylesheet != null) {
			session.setAttribute("stylesheet", as.getTopicProperty(stylesheet, PROPERTY_CSS));
			System.out.println(">>> \"stylesheet\" stored in session: \"" + stylesheet.getName() + "\"");
		} else {
			session.setAttribute("stylesheet", "");
			System.out.println("*** NO STYLESHEET FOUND (there is no stylesheet topic assigned to the Kiez-Atlas workspace)");
		}
	}

	private void initSiteLogo(Session session) {
		BaseTopic siteLogo = ((CityMapTopic) as.getLiveTopic(getCityMap(session))).getSiteLogo();		// ### ugly
		if (siteLogo != null) {
			String imagefile = as.getCorporateWebBaseURL() + FILESERVER_IMAGES_PATH + as.getTopicProperty(siteLogo, PROPERTY_FILE);
			session.setAttribute("siteLogo", imagefile);
			System.out.println(">>> \"siteLogo\" stored in session: \"" + imagefile + "\"");
		} else {
			// ### session.setAttribute("siteLogo", "");
			System.out.println("*** NO SITELOGO FOUND (there is no image topic assigned to the Kiez-Atlas workspace)");
		}
	}

	private void initSiteLinks(Session session) {
		// homepage link
		BaseTopic homepageLink = ((CityMapTopic) as.getLiveTopic(getCityMap(session))).getHomapageLink();		// ### ugly
		if (homepageLink != null) {
			String homepageURL = as.getTopicProperty(homepageLink, PROPERTY_URL);
			session.setAttribute("homepageURL", homepageURL);
			System.out.println(">>> \"homepageURL\" stored in session: \"" + homepageURL + "\"");
		} else {
			// ### session.setAttribute("homepageURL", "");
			System.out.println("*** NO HOMEPAGE LINK FOUND (there is no webpage topic assigned to the Kiez-Atlas workspace)");
		}
		// impressum link
		BaseTopic impressumLink = ((CityMapTopic) as.getLiveTopic(getCityMap(session))).getImpressumLink();		// ### ugly
		if (impressumLink != null) {
			String impressumURL = as.getTopicProperty(impressumLink, PROPERTY_URL);
			session.setAttribute("impressumURL", impressumURL);
			System.out.println(">>> \"impressumURL\" stored in session: \"" + impressumURL + "\"");
		} else {
			// ### session.setAttribute("impressumLink", "");
			System.out.println("*** NO IMPRESSUM LINK FOUND (there is no webpage topic assigned to the Kiez-Atlas workspace)");
		}
	}

	private void setSelectedGeo(String geoID, Session session) {
		GeoObject geo = geoID != null ? new GeoObject(geoID, getCriterias(session), as) : null;
		session.setAttribute("selectedGeo", geo);
		System.out.println("> \"selectedGeoObject\" stored in session: " + geo);
	}

	// ---

	private void setCategoryHotspots(Session session) {
		String mapID = getCityMap(session).getID();
		String instTypeID = getInstitutionType(session).getID();
		Vector selCats = getSelectedCats(session);
		//
		Vector hotspots = new Vector();
		Vector cluster = new Vector();
		Enumeration e = selCats.elements();
		while (e.hasMoreElements()) {
			String catID = (String) e.nextElement();
			Vector presentables = cm.getRelatedViewTopics(mapID, 1, catID, ASSOCTYPE_ASSOCIATION, instTypeID, 1);
			String icon;
			if (getSearchMode(session).equals("0")) {	// ### first search criteria uses distinct visualization
				icon = as.getLiveTopic(catID, 1).getIconfile();
				if (icon.startsWith("ka-")) {	// use only small icon if standard Kiezatlas category icons is used
					icon = icon.substring(0, icon.length() - 4) + "-small.gif";		// ### could be property
				}
			} else {
				icon = ICON_HOTSPOT;
			}
			presentables.insertElementAt(as.getCorporateWebBaseURL() + FILESERVER_ICONS_PATH + icon, 0);
			hotspots.addElement(presentables);
		}
		makeCluster(hotspots, cluster);
		session.setAttribute("cluster", cluster);
		session.setAttribute("hotspots", hotspots);
		System.out.println("> \"hotspots\" stored in session: institutions for " + selCats.size() + " categories");
	}

	/**
	 * nearly the same to setCategoryHotspots, except that it
	 * takes a catID as an argument and returns it index in the enumeration
	 */
	private int setSearchedCategoryHotspots(Session session, String catId) {
		String mapID = getCityMap(session).getID();
		String instTypeID = getInstitutionType(session).getID();
		Vector selCats = getSelectedCats(session);
		//
		Vector hotspots = new Vector();
		Vector cluster = new Vector();
		Enumeration e = selCats.elements();
		int catIndex = 0;
		int finalCatIndex = 0;
		while (e.hasMoreElements()) {
			String catID = (String) e.nextElement();
			if (catId == catID) { finalCatIndex = catIndex; };
			Vector presentables = cm.getRelatedViewTopics(mapID, 1, catID, ASSOCTYPE_ASSOCIATION, instTypeID, 1);
			String icon;
			if (getSearchMode(session).equals("0")) {	// ### first search criteria uses distinct visualization
				icon = as.getLiveTopic(catID, 1).getIconfile();
				if (icon.startsWith("ka-")) {	// use only small icon if standard Kiezatlas category icons is used
					icon = icon.substring(0, icon.length() - 4) + "-small.gif";		// ### could be property
				}
			} else {
				icon = ICON_HOTSPOT;
			}
			presentables.insertElementAt(as.getCorporateWebBaseURL() + FILESERVER_ICONS_PATH + icon, 0);
			hotspots.addElement(presentables);
			catIndex++;
		}
		makeCluster(hotspots, cluster);
		session.setAttribute("cluster", cluster);
		session.setAttribute("hotspots", hotspots);
		System.out.println("> \"hotspots\" stored in session: institutions for " + selCats.size() + " categories");
		return finalCatIndex;
	}

	// ---

	/**
	 * @param	topics	vector of PresentableTopics
	 */
	private void setHotspots(Vector topics, String icon, Session session) {
		Vector hotspots = new Vector();
		Vector cluster = new Vector();
		Vector presentables = new Vector(topics);
		presentables.insertElementAt(as.getCorporateWebBaseURL() + FILESERVER_ICONS_PATH + icon, 0);
		hotspots.addElement(presentables);
		// ### System.out.println("Have to handle clusters in here");
		makeCluster(hotspots, cluster);
		session.setAttribute("cluster", cluster);
		session.setAttribute("hotspots", hotspots);
		System.out.println("> \"hotspots\" stored in session: " + topics.size() + " institutions");
		System.out.println("> \"clusters\" "+cluster.size()+" stored in session");
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

	/** should remove a presentable from the hotspots vector when part of cluster
	 *  ### implement it the other way around
	 */
	private void removeClusteredHotspots(Vector hotspots, Vector clusters) {
		for (int i = 0; i < hotspots.size(); i++) {
			Vector topics = (Vector) hotspots.get(i);
			Vector doubled = new Vector();
			for (int j = 1; j < topics.size(); j++) {
				PresentableTopic topic = (PresentableTopic) topics.get(j);
				if (isPartOfCluster(clusters, topic.getID())) {
					// topic is visible in cluster
					doubled.add(topic);
				}
			}
			topics.removeAll(doubled);
		}
	}

	private boolean isPartOfCluster(Vector clusters, String topicId) {
		for (int i = 0; i < clusters.size(); i++) {
			Cluster object = (Cluster) clusters.get(i);
			Vector topics = object.getPresentables();
			for (int j = 0; j < topics.size(); j++) {
				PresentableTopic topic = (PresentableTopic) topics.get(j);
				if (topic.getID().equals(topicId)) {
					return true;
				}
			}
		}
		return false;
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

	// ---

	private void toggleShapeDisplay(String shapeTypeID, Session session) {
		Vector shapeTypes = getShapeTypes(session);
		Enumeration e = shapeTypes.elements();
		while (e.hasMoreElements()) {
			ShapeType shapeType = (ShapeType) e.nextElement();
			if (shapeType.typeID.equals(shapeTypeID)) {
				shapeType.isSelected = !shapeType.isSelected;
			}
		}
	}

	private void updateShapes(Session session) {
		try {
			Vector shapes = new Vector();
			Vector shapeTypes = getShapeTypes(session);
			String mapID = getCityMap(session).getID();
			// for all shape types ...
			Enumeration e = shapeTypes.elements();
			while (e.hasMoreElements()) {
				ShapeType shapeType = (ShapeType) e.nextElement();
				// if type is selected ...
				if (shapeType.isSelected) {
					// ... query all shapes and add Shape objects to the "shapes" vector
					Vector shapeTopics = cm.getViewTopics(mapID, 1, shapeType.typeID);
					Enumeration e2 = shapeTopics.elements();
					while (e2.hasMoreElements()) {
						PresentableTopic shapeTopic = (PresentableTopic) e2.nextElement();
						String icon = as.getLiveTopic(shapeTopic).getIconfile();
						// --- load shape image and calculate position ---
						String url = as.getCorporateWebBaseURL() + FILESERVER_ICONS_PATH + icon;
						// Note 1: Toolkit.getImage() is used here instead of createImage() in order to utilize
						// the Toolkits caching mechanism
						// Note 2: ImageIcon is a kluge to make sure the image is fully loaded before we proceed
						Image image = new ImageIcon(Toolkit.getDefaultToolkit().getImage(new URL(url))).getImage();
						int width = image.getWidth(null);
						int height = image.getHeight(null);
						// System.out.println(">>> shape size: " + width + "x" + height);
						Dimension size = new Dimension(width, height);
						Point point = shapeTopic.getGeometry();
						point.translate(-width / 2, -height / 2);
						String targetWebalias = as.getTopicProperty(shapeTopic, PROPERTY_TARGET_WEBALIAS);
						//
						shapes.addElement(new Shape(url, point, size, targetWebalias));
					}
				}
			}
			session.setAttribute("shapes", shapes);
			System.out.println("> \"shapes\" stored in session: " + shapes.size() + " shapes");
		} catch (Throwable e) {
			System.out.println("*** BrowseServlet.updateShapes(): " + e);
		}
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

	private BaseTopic getCityMap(Session session) {
		return (BaseTopic) session.getAttribute("map");
	}

	private SearchCriteria[] getCriterias(Session session) {
		return (SearchCriteria[]) session.getAttribute("criterias");
	}

	private SearchCriteria getCriteria(int critNr, Session session) {
		return getCriterias(session)[critNr];
	}

	private SearchCriteria getCurrentCriteria(Session session) {
		int i = Integer.parseInt(getSearchMode(session));
		return getCriteria(i, session);
	}

	private BaseTopic getInstitutionType(Session session) {
		return (BaseTopic) session.getAttribute("instType");
	}

	private String getSearchMode(Session session) {
		return (String) session.getAttribute("searchMode");
	}

	private String getSearchValue(Session session) {
		return (String) session.getAttribute("searchValue");
	}

	private Vector getShapeTypes(Session session) {
		return (Vector) session.getAttribute("shapeTypes");
	}

	// ---

	private GeoObjectTopic getSelectedGeo(Session session) {
		return (GeoObjectTopic) as.getLiveTopic(((GeoObject) session.getAttribute("selectedGeo")).geoID, 1);
	}

	private Vector getSelectedCats(Session session) {
		return getCurrentCriteria(session).selectedCategoryIDs;
	}

	private void initSelectedCatAttribute(Session session) {
		session.setAttribute("selectedCatId", null); // necessary for the new cityMap
	}
}
