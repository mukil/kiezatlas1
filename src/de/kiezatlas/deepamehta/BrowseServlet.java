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
import javax.swing.ImageIcon;

import de.deepamehta.BaseTopic;
import de.deepamehta.DeepaMehtaException;
import de.deepamehta.PresentableTopic;
import de.deepamehta.service.CorporateDirectives;
import de.deepamehta.service.Session;
import de.deepamehta.service.TopicBean;
import de.deepamehta.service.TopicBeanField;
import de.deepamehta.service.web.DeepaMehtaServlet;
import de.deepamehta.service.web.RequestParameter;
import de.deepamehta.service.web.WebSession;
import de.deepamehta.topics.EmailTopic;
import de.deepamehta.topics.TypeTopic;
import de.deepamehta.util.DeepaMehtaUtils;

import de.kiezatlas.deepamehta.topics.CityMapTopic;
import de.kiezatlas.deepamehta.topics.GeoObjectTopic;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



/**
 * Kiezatlas 1.6.5<br>
 * Requires DeepaMehta 2.0b8
 * <p>
 * Last change: 15.06.2010<br>
 * J&ouml;rg Richter / Malte Rei&szlig;ig <br>
 * jri@deepamehta.de / mre@deepamehta.de
 */
public class BrowseServlet extends DeepaMehtaServlet implements KiezAtlas {

	protected String performAction(String action, RequestParameter params, Session session, CorporateDirectives directives)
																									throws ServletException {
		if (action == null) {
			try {
				String pathInfo = params.getPathInfo();
				int additionParam = pathInfo.indexOf("&");
				if (additionParam != -1) {
					String selectCriteria = pathInfo.substring(additionParam+1);
					System.out.println("Info: the default action comes along with a criteria: " + selectCriteria);
					session.setAttribute("defaultCriteria", selectCriteria);
					// clear citymap alias from additional params
					pathInfo = pathInfo.substring(0, additionParam);
				} else {
					// no external criteria link in
					session.setAttribute("defaultCriteria", "0");
				}
				// error check
				if (pathInfo == null || pathInfo.length() == 1) {
					throw new DeepaMehtaException("Fehler in URL");
				}
				//
				String alias = pathInfo.substring(1);
				BaseTopic map = CityMapTopic.lookupCityMap(alias, true, as); // throwIfNotFound=true
				setCityMap(map, session);
				if (!CityMapTopic.isProtected(map, as)) {
					//
					initCityMap(session);
					return PAGE_FRAMESET;
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
		//
		if (action.equals(ACTION_TRY_LOGIN)) {
			String password = params.getValue("password");
			if (CityMapTopic.passwordCorrect(getCityMap(session), as, password)) {
				initCityMap(session);
				return PAGE_FRAMESET;
			} else {
				return PAGE_MAP_LOGIN;
			}
		} else if (action.equals(ACTION_INIT_FRAME)) {
			String frame = params.getValue("frame");
			// initialize frames
			if (frame.equals(FRAME_LEFT)) {
				return PAGE_CITY_MAP;
			} else if (frame.equals(FRAME_RIGHT)) {
				// list categories of 1st search criteria, if there is a criteria at all
				if (getCriterias(session).length > 0) {
					String criteria = (String) session.getAttribute("defaultCriteria");
					if (criteria != null) {
						setSearchMode(criteria, session);
					} else {
						session.setAttribute("defaultCriteria", "0");
						setSearchMode("0", session);	// ### was SEARCHMODE_BY_CATEGORY
					}
					return PAGE_CATEGORY_LIST;
				} else {
					// otherwise list all institutions
					setSearchMode(SEARCHMODE_BY_NAME, session);
					setSearchValue("", session);	// searching for "" retrieves all institutions
					return PAGE_GEO_LIST;
				}
			} else {
				throw new DeepaMehtaException("unexpected frame \"" + frame + "\"");
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
			String adminInfo = topicBean.getValue(KiezAtlas.PROPERTY_ADMINISTRATION_INFO);
			if (adminInfo != null && adminInfo.indexOf("lor/analysen/") != -1) {
				topicBean = prepareNewLorPageLink(topicBean);
			}
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
		// show forum if wanted
		} else if (action.equals(ACTION_SHOW_GEO_FORUM)) {
			return PAGE_GEO_FORUM;
		// show comment form
		} else if (action.equals(ACTION_SHOW_COMMENT_FORM)) {
			// Note: "instComments" are still in the session
			return PAGE_COMMENT_FORM;
		// create comment
		} else if (action.equals(ACTION_CREATE_COMMENT)) {
			// create comment and set date & time
			String commentID = createTopic(TOPICTYPE_COMMENT, params, session, directives);
			as.setTopicProperty(commentID, 1, PROPERTY_COMMENT_DATE, DeepaMehtaUtils.getDate());
			as.setTopicProperty(commentID, 1, PROPERTY_COMMENT_TIME, DeepaMehtaUtils.getTime());
			// associate comment with forum
			GeoObjectTopic geo = getSelectedGeo(session);
			String forumID = geo.getForum().getID();
			String assocID = as.getNewAssociationID();
			cm.createAssociation(assocID, 1, SEMANTIC_FORUM_COMMENTS, 1, forumID, 1, commentID, 1);
			// send notification email
			sendNotificationEmail(geo.getID(), commentID);
			//
			return PAGE_GEO_FORUM;
		// shape display
		} else if (action.equals(ACTION_TOGGLE_SHAPE_DISPLAY)) {
			String shapeTypeID = params.getValue("typeID");
			toggleShapeDisplay(shapeTypeID, session);
			updateShapes(session);
			return PAGE_CITY_MAP;
		} else {
			return super.performAction(action, params, session, directives);
		}
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
			Vector taggedInsts;
			Hashtable propertyFilter;
			// enable the rendering of enumeration
			session.setAttribute("enumerationFlag", true);
			if (searchMode.equals(SEARCHMODE_BY_NAME)) {
				insts = cm.getViewTopics(mapID, 1, instTypeID, getSearchValue(session));
				if (!getSearchValue(session).equals("")) {
				propertyFilter = new Hashtable();
				propertyFilter.put(PROPERTY_TAGFIELD, getSearchValue(session));
				taggedInsts = cm.getTopics(getInstitutionType(session).getID(), propertyFilter, mapID);
				for (int k = 0; k < taggedInsts.size(); k++) {
					BaseTopic taggedTopic = (BaseTopic) taggedInsts.get(k);
					Vector pts = cm.getViewTopics(mapID, 1, instTypeID, taggedTopic.getName());
					System.out.println("newSearchMode fetched additionally " + pts.size() + " tagged Institutions byName!");
					if (pts.size() >= 1) {
					if (!insts.contains(pts.get(0))) {
						insts.add(pts.get(0));
						System.out.println("newSearchMode added fetched PresentableTopic => " + ((BaseTopic)pts.get(0)).getName() + " !byName");
					} else {
						System.out.println("newSearchMode skipped fetched PresentableTopic => " + ((BaseTopic)pts.get(0)).getName() + " !byName");
					}
					} // insts.add(new PresentableTopic(topic, new Point()));
				}
				session.setAttribute("taggedInstitutions", taggedInsts);
				}
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



	// *****************
	// *** Utilities ***
	// *****************


	private void initCityMap(Session session) {
		initInstitutaionType(session);	// relies on city map
		initSearchCriterias(session);	// relies on city map
		initShapeTypes(session);		// relies on city map
		initStylesheet(session);		// relies on city map
		initSiteLogo(session);			// relies on city map
		initSiteLinks(session);			// relies on city map
		initSelectedCatAttribute(session);
		//
		updateShapes(session);			// relies on shape types;
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

	// ---

	private void sendNotificationEmail(String instID, String commentID) {
		try {
			GeoObjectTopic inst = (GeoObjectTopic) as.getLiveTopic(instID, 1);
			// "from"
			String from = as.getEmailAddress("t-rootuser");		// ###
			if (from == null || from.equals("")) {
				throw new DeepaMehtaException("email address of root user is unknown");
			}
			// "to"
			BaseTopic email = inst.getEmail();
			if (email == null || email.getName().equals("")) {
				throw new DeepaMehtaException("email address of \"" + inst.getName() + "\" is unknown");
			}
			String to = email.getName();
			// "subject"
			String subject = "Kiezatlas: neuer Forumsbeitrag für \"" + inst.getName() + "\"";
			// "body"
			Hashtable comment = cm.getTopicData(commentID, 1);
			String body = "Dies ist eine automatische Benachrichtigung von www.kiezatlas.de\r\r" +
				"Im Forum der Einrichtung \"" + inst.getName() + "\" wurde ein neuer Kommentar eingetragen:\r\r" +
				"------------------------------\r" +
				comment.get(PROPERTY_TEXT) + "\r\r" +
				"Autor: " + comment.get(PROPERTY_COMMENT_AUTHOR) + "\r" +
				"Email: " + comment.get(PROPERTY_EMAIL_ADDRESS) + "\r" +
				"Datum: " + comment.get(PROPERTY_COMMENT_DATE) + "\r" +
				"Uhrzeit: " + comment.get(PROPERTY_COMMENT_TIME) + "\r" +
				"------------------------------\r\r" +
				"Im Falle des Mißbrauchs: In der \"Forum Administration\" ihres persönlichen Kiezatlas-Zugangs haben " +
				"Sie die Möglichkeit, einzelne Kommentare zu löschen, bzw. das Forum ganz zu deaktivieren.\r" +
				"www.kiezatlas.de/edit/" + inst.getWebAlias() + "\r\r" +
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



	// *************************
	// *** Session Utilities ***
	// *************************



	// --- Methods to maintain data in the session

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
		} else {
			// ### session.setAttribute("homepageURL", "");
			System.out.println("*** NO HOMEPAGE LINK FOUND (there is no webpage topic assigned to the Kiez-Atlas workspace)");
		}
		// impressum link
		BaseTopic impressumLink = ((CityMapTopic) as.getLiveTopic(getCityMap(session))).getImpressumLink();		// ### ugly
		if (impressumLink != null) {
			String impressumURL = as.getTopicProperty(impressumLink, PROPERTY_URL);
			session.setAttribute("impressumURL", impressumURL);
		} else {
			// ### session.setAttribute("impressumLink", "");
			System.out.println("*** NO IMPRESSUM LINK FOUND (there is no webpage topic assigned to the Kiez-Atlas workspace)");
		}
		String webAlias = as.getTopicProperty(getCityMap(session), PROPERTY_WEB_ALIAS);
		session.setAttribute("webAlias", webAlias);
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
		//
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
			// ### System.out.println("> \"clusters\": "+clusters.toString());
		}
        //removeClusteredHotspots(hotspots, clusters);
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
		for (int p=0; p < hotspots.size(); p++) {
			Vector scnd = (Vector) hotspots.get(p);
			for (int o=1; o < scnd.size(); o++) {
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

	private TopicBean prepareNewLorPageLink(TopicBean topicBean) {
		// start migrating all links in the system to the new lor-pages
		String lorId = null;
		String adminInfo = topicBean.getValue(KiezAtlas.PROPERTY_ADMINISTRATION_INFO);
		Pattern pattern = Pattern.compile("(<a href=\"\\D+)(/analysen/)(\\d+)(.pdf)(\\D+)(target=\"_blank\">)(\\D+)(</a>)");
		Matcher matcher = pattern.matcher(adminInfo);
		while (matcher.find()) {
			Pattern idPattern = Pattern.compile("(\\d+)");
			Matcher idMatch = idPattern.matcher(matcher.group());
			while (idMatch.find()) { lorId = idMatch.group(); }
			if (lorId != null) {
				String before = adminInfo.substring(0, matcher.start());
				String link = "<a href=\"http://jugendserver.spinnenwerk.de/~lor/seiten/2014/06/?lor="
					+ lorId + "\" target=\"_blank\">Sozialdaten Planungsraum</a>";
				String after = adminInfo.substring(matcher.end());
				adminInfo = before + link + after;
				TopicBeanField adminField = topicBean.getField(KiezAtlas.PROPERTY_ADMINISTRATION_INFO);
				adminField.value = adminInfo; // migration sucessfull
			}
		}
		return topicBean;
	}

	// ---

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
