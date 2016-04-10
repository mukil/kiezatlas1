package de.kiezatlas.deepamehta.topics;

import com.google.gson.*;
import com.google.gson.stream.JsonWriter;

import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;

import de.kiezatlas.deepamehta.Comment;
import de.kiezatlas.deepamehta.KiezAtlas;
import de.kiezatlas.deepamehta.SearchCriteria;
import de.kiezatlas.deepamehta.etl.Transformation;

import de.deepamehta.AmbiguousSemanticException;
import de.deepamehta.BaseTopic;
import de.deepamehta.DeepaMehtaException;
import de.deepamehta.PresentableTopic;
import de.deepamehta.PropertyDefinition;
import de.deepamehta.service.*;
import de.deepamehta.topics.LiveTopic;
import de.deepamehta.topics.TopicTypeTopic;
import de.deepamehta.topics.TypeTopic;
import de.deepamehta.util.DeepaMehtaUtils;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Kiezatlas 1.7<br>
 * Requires DeepaMehta 2.0b8-rev388
 * <p>
 * Last change: 10.04.2016<br>
 * J&ouml;rg Richter, Malte Rei&szlig;ig<br>
 * jri@deepamehta.de, mre@deepamehta.de
 */
public class GeoObjectTopic extends LiveTopic implements KiezAtlas {



	// --- Constants

	static final String DEFAULT_PASSWORD = "123password";

	// --- HTTP Remote API Constants

	private static final String KA2_SERVICE_URL = "http://api.kiezatlas.de";
	private static final String KA2_DEFAULT_WS_TOPIC_ID = "";
	private static final String KA2_ADMIN_PASSWORD = "";
	private static final String REQUEST_CONTENT_TYPE = "application/json;charset=UTF-8";
	private static final String HTTP_AUTH_ENDPOINT = KA2_SERVICE_URL + "/accesscontrol/login";
	private static final String CORE_TOPIC_ENDPOINT = KA2_SERVICE_URL + "/core/topic/";
	private static final String CORE_GET_BY_URI_ENDPOINT = KA2_SERVICE_URL + "/core/topic/by_value/uri/";
	private static final String FACET_TOPIC_ENDPOINT = KA2_SERVICE_URL + "/facet/";
	private static final String FACET_TOPIC_MULTI_ENDPOINT = KA2_SERVICE_URL + "/facet/multi/";
	private static final String CORE_TOPIC_SEARCH_ENDPOINT = KA2_SERVICE_URL + "/core/topic?search=";
	public static final String KA2_TOPIC_URI_PREFIX	= "de.kiezatlas.topic.";

	// KA 2 Remote Instance HTTP Session ID
	private String validSessionId			= null;

	// --- Constructor
	public GeoObjectTopic(BaseTopic topic, ApplicationService as) {
		super(topic, as);
	}

	// **********************
	// *** Defining Hooks ***
	// **********************

	// ------------------
	// --- Life Cycle ---
	// ------------------

	public CorporateDirectives evoke(Session session, String topicmapID, String viewmode) {
		setProperty(PROPERTY_LOCKED_GEOMETRY, SWITCH_ON);
		setProperty(PROPERTY_PASSWORD, DEFAULT_PASSWORD);
		setProperty(PROPERTY_LAST_MODIFIED, DeepaMehtaUtils.getDate());
		return super.evoke(session, topicmapID, viewmode);
	}

	// --------------------------
	// --- Providing Commands ---
	// --------------------------

	public CorporateCommands contextCommands(String topicmapID, String viewmode, Session session, CorporateDirectives directives) {
		CorporateCommands commands = new CorporateCommands(this.as);
		int editorContext = this.as.editorContext(topicmapID);

		commands.addNavigationCommands(this, editorContext, session);
		commands.addSeparator();
		// --- "Lock"/"Unlock" ---
		boolean isLocked = getProperty(PROPERTY_LOCKED_GEOMETRY).equals(SWITCH_ON);
		int lockState = !isLocked ? COMMAND_STATE_DEFAULT : COMMAND_STATE_DISABLED;
		int unlockState = isLocked ? COMMAND_STATE_DEFAULT : COMMAND_STATE_DISABLED;
		//
		commands.addCommand(ITEM_LOCK_GEOMETRY, CMD_LOCK_GEOMETRY, lockState);
		commands.addCommand(ITEM_UNLOCK_GEOMETRY, CMD_UNLOCK_GEOMETRY, unlockState);
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
		if (cmd.equals(CMD_LOCK_GEOMETRY) || cmd.equals(CMD_UNLOCK_GEOMETRY)) {
			String value = cmd.equals(CMD_LOCK_GEOMETRY) ? SWITCH_ON : SWITCH_OFF;
			directives.add(as.setTopicProperty(this, PROPERTY_LOCKED_GEOMETRY, value, topicmapID, session));
		} else {
			return super.executeCommand(command, session, topicmapID, viewmode);
		}
		return directives;
	}

	// ------------------------------------------
	// --- Reacting upon dedicated situations ---
	// ------------------------------------------

	public CorporateDirectives moved(String topicmapID, int topicmapVersion, int x, int y, Session session) {
		CorporateDirectives directives = super.moved(topicmapID, topicmapVersion, x, y, session);
		// abort if we are not inside a city map, but inside a plain topic map
		if (!(as.getLiveTopic(topicmapID, 1) instanceof CityMapTopic)) {
			return directives;
		}
		//
		Point2D.Float yadePoint = getYadePoint(x, y, topicmapID);
		// set properties
		if (yadePoint != null) {	// Note: yadePoint is null if YADE is "off"
			Hashtable props = new Hashtable();
			props.put(PROPERTY_YADE_X, Float.toString(yadePoint.x));
			props.put(PROPERTY_YADE_Y, Float.toString(yadePoint.y));
			directives.add(DIRECTIVE_SHOW_TOPIC_PROPERTIES, getID(), props, new Integer(1));
		}
		//
		return directives;
	}

	// ---------------------------
	// --- Handling Properties ---
	// ---------------------------

	public boolean propertyChangeAllowed(String propName, String propValue, Session session, CorporateDirectives directives) {
		// compare to CityMapTopic.propertyChangeAllowed()
		if (propName.equals(PROPERTY_WEB_ALIAS)) {
			String webAlias = propValue;
			// ### compare to lookupInstitution()
			try {
				TypeTopic typeTopic = as.type(TOPICTYPE_KIEZ_GEO, 1);
				Vector typeIDs = typeTopic.getSubtypeIDs();
				Hashtable props = new Hashtable();
				props.put(PROPERTY_WEB_ALIAS, webAlias);
				Vector insts = cm.getTopics(typeIDs, props, true);	// caseSensitive=true
				//
				if (insts.size() > 0) {
					BaseTopic inst = (BaseTopic) insts.firstElement();
					String errText = "Web Alias \"" + webAlias + "\" ist bereits an \"" + inst.getName()
						+ "\" vergeben -- Für \"" + getName() + "\" bitte anderen Web Alias verwenden";
					directives.add(DIRECTIVE_SHOW_MESSAGE, errText, new Integer(NOTIFICATION_WARNING));
					System.out.println("*** GeoObjectTopic.propertyChangeAllowed(): " + errText);
					return false;
				}
			} catch (DeepaMehtaException ex) {
				// catches nullpointer in as.type() call, should never happen.
				// but i got a error report for this. i assume that it's session timer issue
				directives.add(DIRECTIVE_SHOW_MESSAGE, "Beim aktualisieren des Webalias einer Einrichtung "
				+ "ist folgender Fehler aufgetreten: " + ex, new Integer(NOTIFICATION_ERROR));
			}
		}
		return super.propertyChangeAllowed(propName, propValue, session, directives);
	}

	public CorporateDirectives propertiesChanged(Hashtable newProps, Hashtable oldProps,
		String topicmapID, String viewmode, Session session) {
		CorporateDirectives directives = super.propertiesChanged(newProps, oldProps, topicmapID, viewmode, session);
		// --- "YADE" ---
		if (newProps.get(PROPERTY_YADE_X) != null || newProps.get(PROPERTY_YADE_Y) != null) {
			try {
				// determine new geometry
				Point p = getPoint(topicmapID);	// throws DME
				// set new geometry
				if (p != null) {	// Note: p is null if YADE is "off"
					directives.add(DIRECTIVE_SET_TOPIC_GEOMETRY, getID(), p, topicmapID);
				} else {
					// ###
					System.out.println(">>> GeoObjectTopic.propertiesChanged(): " + this
						+ " not (re)positioned (VADE is \"off\")");
				}
			} catch (DeepaMehtaException e) {
				throw new DeepaMehtaException("\"" + getName() + "\" konnte nicht automatisch positioniert werden ("
					+ e.getMessage() + ")");
			}
		}
		// --- update timestamp on topic level ---
		cm.setTopicData(getID(), 1, PROPERTY_LAST_MODIFIED, DeepaMehtaUtils.getDate());
		// --- update timestamp on city map level ---
		Vector<BaseTopic> cityMaps = cm.getViews(getID(), 1, VIEWMODE_HIDDEN);
		//     for the ease we now update this property for *all* citymaps a topic is part (also of the unpublished)
		for (int i = 0; i < cityMaps.size(); i++) {
			BaseTopic cityMap = cityMaps.get(i);
			long lastUpdated = new Date().getTime();
			as.setTopicProperty(cityMap, PROPERTY_LAST_UPDATED, "" + lastUpdated + "");
		}
		return directives;
	}

	public void associationRemoved(String assocTypeID, String relTopicID, Session session,
		CorporateDirectives directives) {
		if (isPartOfMigratedWorkspaces()) {
			// 1) delete all category-facet topics related in ka2
			String typeId = as.getLiveTopic(relTopicID, 1).getType();
			BaseTopic type = as.getLiveTopic(typeId, 1);
			System.out.println("    Association \"" + assocTypeID + "\" removed from Geo Object to " + type.getName());
			// 2) Improvement: ### just try to delete specific assoc-types remotely
			getHTTPSession(KA2_ADMIN_PASSWORD);
			JsonObject remoteTopic = getGeoObjectByTopicId(getID());
			if (remoteTopic != null) { // topic is not synchronized
				String remoteTopicId = parseGeoObjectParentId(remoteTopic);
				if (Transformation.CRITERIA_MAP.containsKey(typeId)) {
					String relFacetTypeURI = Transformation.CRITERIA_MAP.get(typeId);
					System.out.println("    categoryRemoval of facetTypeURI: " + relFacetTypeURI);
					JsonArray categories = getCategoryFacetTopics(relFacetTypeURI, remoteTopicId);
					if (categories != null) {
						if (categories.size() > 0) {
							deleteCategoryFacetTopics(relFacetTypeURI, categories, remoteTopicId);
						}
					}
				}
			}
		}
	}
	
	// ------------------------
	// --- Topic Type Hooks ---
	// ------------------------
	/**
	 * @return	the ID of the search type
	 *
	 * @see	TopicTypeTopic#createSearchType
	 */
	public static String getSearchTypeID() {
		return TOPICTYPE_KIEZ_GEO_SEARCH;
	}

	
	// --- Custom Geo Object Methods

	public static Vector hiddenProperties(TypeTopic type) {
		Vector props = new Vector();
		props.addElement(PROPERTY_DESCRIPTION);
		return props;
	}

	public static Vector hiddenProperties(TypeTopic type, String relTopicTypeID) {
		Vector props = null;
		if (relTopicTypeID.equals(TOPICTYPE_EMAIL_ADDRESS)) {
			props = new Vector();
			props.addElement(PROPERTY_MAILBOX_URL);
		} else if (relTopicTypeID.equals(TOPICTYPE_IMAGE)) {
			props = new Vector();
			props.addElement(PROPERTY_NAME);
		}
		return props;
	}

	public static void propertyLabel(PropertyDefinition propDef, ApplicationService as, Session session) {
		String propName = propDef.getPropertyName();
		if (propName.equals(PROPERTY_SONSTIGES)) {
			propDef.setPropertyLabel("Weitere Infos");
		}
	}

	public static String propertyLabel(PropertyDefinition propDef, String relTopicTypeID, ApplicationService as) {
		String propName = propDef.getPropertyName();
		if (relTopicTypeID.equals(TOPICTYPE_ADDRESS)) {
			if (propName.equals(PROPERTY_STREET)) {
				return "Stra&szlig;e";
			} else if (propName.equals(PROPERTY_POSTAL_CODE)) {
				return "Postleitzahl";
			}
		} else if (relTopicTypeID.equals(TOPICTYPE_WEBPAGE)) {
			if (propName.equals(PROPERTY_URL)) {
				return "Website (URL)";
			}
		} else if (relTopicTypeID.equals(TOPICTYPE_AGENCY)) {
			if (propName.equals(PROPERTY_NAME)) {
				return "Tr&auml;ger";
			} else if (propName.equals(PROPERTY_AGENCY_KIND)) {
				return "Art des Tr&auml;gers";
			}
		} else if (relTopicTypeID.equals(TOPICTYPE_PERSON)) {
			if (propName.equals(PROPERTY_FIRST_NAME)) {
				return "Ansprechpartner/in (Vorname)";
			} else if (propName.equals(PROPERTY_NAME)) {
				return "Ansprechpartner/in (Nachname)";
			} else if (propName.equals(PROPERTY_GENDER)) {
				return "Ansprechpartner/in";
			}
		} else if (relTopicTypeID.equals(TOPICTYPE_PHONE_NUMBER)) {
			if (propName.equals(PROPERTY_NAME)) {
				return "Telefon";
			}
		} else if (relTopicTypeID.equals(TOPICTYPE_FAX_NUMBER)) {
			if (propName.equals(PROPERTY_NAME)) {
				return "Fax";
			}
		} else if (relTopicTypeID.equals(TOPICTYPE_EMAIL_ADDRESS)) {
			if (propName.equals(PROPERTY_EMAIL_ADDRESS)) {
				return "E-mail";
			}
		} else if (relTopicTypeID.equals(TOPICTYPE_FORUM)) {
			if (propName.equals(PROPERTY_FORUM_ACTIVITION)) {
				return "Forum Aktivierung";
			}
		}
		return LiveTopic.propertyLabel(propDef, relTopicTypeID, as);
	}

	public static BaseTopic lookupInstitution(String alias, ApplicationService as) throws DeepaMehtaException {
		Vector typeIDs = as.type(TOPICTYPE_KIEZ_GEO, 1).getSubtypeIDs();
		Hashtable props = new Hashtable();
		props.put(PROPERTY_WEB_ALIAS, alias);
		Vector institutions = as.cm.getTopics(typeIDs, props, true);		// caseSensitiv=true
		// error check
		if (institutions.size() == 0) {
			throw new DeepaMehtaException("Fehler in URL: \"" + alias + "\" ist nicht bekannt");
		}
		if (institutions.size() > 1) {
			throw new DeepaMehtaException("Mehrdeutigkeit: es gibt "
				+ institutions.size() + " \"" + alias + "\" Einrichtungen");
		}
		//
		BaseTopic inst = (BaseTopic) institutions.firstElement();
		return inst;
	}

	private SearchCriteria[] getWorkspaceCriterias(String cityMapId) {
		CityMapTopic mapTopic = (CityMapTopic) as.getLiveTopic(cityMapId, 1);
		SearchCriteria[] crits = mapTopic.getSearchCriterias();
		return crits;
	}

	public String getWebAlias() {
		return getProperty(PROPERTY_WEB_ALIAS);
	}

	public BaseTopic getAddress() {
		try {
			return as.getRelatedTopic(getID(), ASSOCTYPE_ASSOCIATION, TOPICTYPE_ADDRESS, 2, true);
											// emptyAllowed=true
		} catch (AmbiguousSemanticException e) {
			System.out.println("*** GeoObjectTopic.getAddress(): " + e);
			return e.getDefaultTopic();
		}
	}

	/**
	 * Encapsulates the logic for backward compatibility after GeoObject's
	 * new Address / City Relation
	 *
	 * @return
	 */
	public String getCity() {
		// if a geoobject has a stadt property, take it and return it
		// else if a geoobject has an addressTopic assigned, check if there is a city, if so return this city
		String city = getProperty(PROPERTY_CITY);
		BaseTopic address = getAddress();
		try {
			if (!city.equals("")) {
				return city;
			} else if (address != null) {
				BaseTopic town = as.getRelatedTopic(address.getID(), ASSOCTYPE_ASSOCIATION,
				    TOPICTYPE_CITY, 2, true);
				if (town != null) {
					city = town.getName();
					return city;
				}
			}
			return "";
		} catch (AmbiguousSemanticException aex) {
			System.out.println("*** GeoObjectTopic.getCity(): " + aex);
			return aex.getDefaultTopic().getName();
		}
	}

	public BaseTopic getEmail() {
		try {
			return as.getRelatedTopic(getID(), ASSOCTYPE_ASSOCIATION, TOPICTYPE_EMAIL_ADDRESS, 2, true);
		} catch (AmbiguousSemanticException e) {
			System.out.println("*** GeoObjectTopic.getEmail(): " + e);
			return e.getDefaultTopic();
		}
	}

	// ---
	public Vector getCategories(String critTypeID) {
		return as.getRelatedTopics(getID(), ASSOCTYPE_ASSOCIATION, critTypeID, 2);
	}

	// ---
	public BaseTopic getImage() {
		try {
			return as.getRelatedTopic(getID(), ASSOCTYPE_ASSOCIATION, TOPICTYPE_IMAGE, 2, true);
		} catch (AmbiguousSemanticException e) {
			System.out.println("*** GeoObjectTopic.getImage(): " + e);
			return e.getDefaultTopic();
		}
	}

	// ---
	/**
	 * Converts the YADE-coordinates of this geo object into screen
	 * coordinates for the specified citymap.
	 *
	 * @return	the screen coordinates, or <code>null</code> if YADE is
	 * "off". YADE is regarded as "off" if there are no YADE-reference
	 * points defined in the specified city map.
	 *
	 * @throws	DeepaMehtaException	if citymapID is <code>null</code>.
	 * @throws	DeepaMehtaException	if there is only one or more than 2
	 * YADE-reference points.
	 * @throws	DeepaMehtaException	if a YADE-reference point has invalid
	 * coordinates (no float format).
	 * @throws	DeepaMehtaException	if this geo object has invalid
	 * coordinates (no float format).
	 *
	 * @see	#propertiesChanged
	 * @see	CityMapTopic#getPresentableTopic
	 * @see	CityMapTopic#repositionAllInstitutions
	 */
	Point getPoint(String citymapID) throws DeepaMehtaException {
		// ### copied
		CityMapTopic citymap = (CityMapTopic) as.getLiveTopic(citymapID, 1);	// throws DME
		int x1, y1, x2, y2;
		float yadeX1, yadeY1, yadeX2, yadeY2;
		try {
			PresentableTopic[] yp = citymap.getYADEReferencePoints();	// throws DME
			if (yp == null) {
				// YADE is "off"
				return null;
			}
			x1 = yp[0].getGeometry().x;
			y1 = yp[0].getGeometry().y;
			x2 = yp[1].getGeometry().x;
			y2 = yp[1].getGeometry().y;
			yadeX1 = Float.parseFloat(as.getTopicProperty(yp[0], PROPERTY_YADE_X));
			yadeY1 = Float.parseFloat(as.getTopicProperty(yp[0], PROPERTY_YADE_Y));
			yadeX2 = Float.parseFloat(as.getTopicProperty(yp[1], PROPERTY_YADE_X));
			yadeY2 = Float.parseFloat(as.getTopicProperty(yp[1], PROPERTY_YADE_Y));
		} catch (NumberFormatException e) {
			throw new DeepaMehtaException("ein YADE-Referenzpunkt von Stadtplan \"" + citymap.getName()
				+ "\" hat ungültigen Wert (" + e.getMessage() + ")");
		}
		// yade -> pixel
		try {
			float yadeX = Float.parseFloat(getProperty(PROPERTY_YADE_X));
			float yadeY = Float.parseFloat(getProperty(PROPERTY_YADE_Y));
			int x = (int) (x1 + (x2 - x1) * (yadeX - yadeX1) / (yadeX2 - yadeX1));
			int y = (int) (y2 + (y1 - y2) * (yadeY - yadeY2) / (yadeY1 - yadeY2));
			return new Point(x, y);
		} catch (NumberFormatException e) {
			throw new DeepaMehtaException("YADE-Koordinate von \"" + getName() + "\" ist ungültig ("
				+ e.getMessage() + ")");
		}
	}

	/**
	 * Converts the specified screen coordinate into a YADE-coordinate.
	 *
	 * @return	the YADE-coordinate, or <code>null</code> if YADE is "off".
	 *
	 * @see	#moved
	 */
	Point2D.Float getYadePoint(int x, int y, String citymapID) throws DeepaMehtaException {
		// ### copied
		CityMapTopic citymap = (CityMapTopic) as.getLiveTopic(citymapID, 1);
		try {
			PresentableTopic[] yp = citymap.getYADEReferencePoints();	// throws DME
			if (yp == null) {
				return null;
			}
			int x1 = yp[0].getGeometry().x;
			int y1 = yp[0].getGeometry().y;
			int x2 = yp[1].getGeometry().x;
			int y2 = yp[1].getGeometry().y;
			float yadeX1 = Float.parseFloat(as.getTopicProperty(yp[0], PROPERTY_YADE_X));
			float yadeY1 = Float.parseFloat(as.getTopicProperty(yp[0], PROPERTY_YADE_Y));
			float yadeX2 = Float.parseFloat(as.getTopicProperty(yp[1], PROPERTY_YADE_X));
			float yadeY2 = Float.parseFloat(as.getTopicProperty(yp[1], PROPERTY_YADE_Y));
			// pixel -> yade
			float yadeX = yadeX1 + (yadeX2 - yadeX1) * (x - x1) / (x2 - x1);
			float yadeY = yadeY2 + (yadeY1 - yadeY2) * (y2 - y) / (y2 - y1);
			return new Point2D.Float(yadeX, yadeY);
		} catch (NumberFormatException e) {
			throw new DeepaMehtaException("ein YADE-Referenzpunkt von Stadtplan \"" + citymap.getName()
				+ "\" hat ungültigen Wert (" + e.getMessage() + ")");
		}
	}

	// ---
	public boolean isForumActivated() {
		BaseTopic forum = getForum();
		if (forum == null) {
			return false;
		}
		return as.getTopicProperty(forum, PROPERTY_FORUM_ACTIVITION).equals(SWITCH_ON);
	}

	public Vector getComments() {
		BaseTopic forum = getForum();
		if (forum == null) {
			throw new DeepaMehtaException("Institution " + getID() + " has no forum topic");
		}
		String[] sortProps = {PROPERTY_COMMENT_DATE, PROPERTY_COMMENT_TIME};
		return cm.getRelatedTopics(forum.getID(), SEMANTIC_FORUM_COMMENTS, TOPICTYPE_COMMENT, 2, sortProps, true);
												// descending=true
	}

	public BaseTopic getForum() {
		try {
			return as.getRelatedTopic(getID(), SEMANTIC_INSTITUTION_FORUM, TOPICTYPE_FORUM, 2, true);
											// emptyAllowed=true
		} catch (AmbiguousSemanticException e) {
			System.out.println("*** GeoObjectTopic.getForum(): " + e);
			return e.getDefaultTopic();
		}
	}

	public Vector getCommentBeans() {
		Vector commentBeans = new Vector();
		//
		Enumeration e = getComments().elements();
		while (e.hasMoreElements()) {
			BaseTopic comment = (BaseTopic) e.nextElement();
			commentBeans.addElement(new Comment(comment.getID(), as));
		}
		//
		return commentBeans;
	}

	private String getAddressString() {
		String result;
		StringBuffer address = new StringBuffer();
		// Related Address Topic
		BaseTopic add = getAddress();
		if (add != null) {
			if (add.getName().equals("")) {
				System.out.println("*** loadGPSCoordinates.WARNING: addressString is empty for: " + getName());
				return "";
			}
			address.append(add.getName());
		} else {
			return "";
		}
		// Postal Code of Address
		address.append(", " + as.getTopicProperty(add, PROPERTY_POSTAL_CODE));
		// City
		address.append(" " + getCity());
		result = address.toString();
		return result;
	}



	// -------------------------------------------------------------------------------- Kiezatlas 2 Upgrade Service API



	/**
	 * Create a mirrored geo-object at the service endpoint.
	 */
	public void postRemoteGeoObject(String mapTopicId, String mapAlias, String workspaceId) {
		postRemoteGeoObject(mapTopicId, mapAlias, workspaceId, null);
	}

	/**
	 * Create a mirrored geo-object at the service endpoint
	 */
	public void postRemoteGeoObject(String mapTopicId, String mapAlias, String workspaceId, SearchCriteria[] criterias) {
		postRemoteGeoObject(mapTopicId, mapAlias, workspaceId, criterias, null);
	}

	/**
	 * Create a mirrored geo-object at the service endpoint.
	 * If topic does not exist sends a "dm4_no_geocoding=true" cookie.
	 *
	 * @param mapAlias	Web Alias of Bezirksregion
	 * @param workspaceId	Name of Bezirks Workspace
	 * @param criterias	SearchCriteria (per Workspace configured TopicTypes)
	 * @param sessionId	JSESSIONID for accessing the service endpoint
	 */
	public void postRemoteGeoObject(String mapTopicId, String mapAlias, String workspaceId,
		SearchCriteria[] criterias, String sessionId) {
		// Fetch workspace criteria system
		if (criterias == null) {
			criterias = getWorkspaceCriterias(mapTopicId);
		}
		// Possibly acquire new HTTP Session
		if (sessionId != null) validSessionId = sessionId;
		if (validSessionId == null) {
			getHTTPSession(KA2_ADMIN_PASSWORD);
		}
		// Get (or Create) Geo Object Topic remotely
		JsonObject newTopic = getGeoObjectByTopicId(getID());
		String newTopicId = "", addressTopicId = "";
		if (newTopic != null) {
			newTopicId = parseGeoObjectParentId(newTopic);
			addressTopicId = parseGeoObjectAddressId(newTopic);
			System.out.println("  Geo Object \"" + getName() + "\" (" + getID() + ") with KA 2 ID (" + newTopicId + ")");
		} else {
			newTopic = postNewTopic(as.getLiveTopic(getID(), 1));
			newTopicId = parseGeoObjectParentId(newTopic);
			addressTopicId = parseGeoObjectAddressId(newTopic);
			if (newTopicId != null) {
				System.out.println("> Created GeoObject \"" + getName() + "\" (" + getID()
					+ ") Topic with KA 2 ID \"" + newTopicId + "\"");
			}
		}
		postGeoCoordinateFacet(getID(), addressTopicId);
		// ..) see also ### compare to updateRemoteTopicFacets
		// Enrich Kiezatlas 2 Geo Object Topic with standard Facets
		postPropertyFacets(getID(), newTopicId);
		postWebpageFacet(getID(), newTopicId);
		postContactFacets(getID(), newTopicId);
		postImageFileFacet(getID(), newTopicId);
		postAgencyFacet(getID(), newTopicId);
		if (!mapAlias.toLowerCase().contains("gesamt")) { // no gesamt-plan..
			postBezirksregionFacet(getID(), newTopicId, mapAlias);
		}
		postBezirksFacet(getID(), newTopicId, as.getLiveTopic(workspaceId, 1).getName());
		// Post Category of GeoObjectTopic
		for (int k = 0; k < criterias.length; k++) {
			SearchCriteria criteria = criterias[k];
			postCategoryFacets(getID(), newTopicId, criteria);
		}
	}

	/**
	 * Synchronize data of a just _edited_ geo-object at the service endpoint.
	 */
	public void synchronizeGeoObject() {
	// Check if we have to update a topic remotely..
		// 1) In any case, we need a new HTTP-Session for this GeoObject-Instance
		getHTTPSession(KA2_ADMIN_PASSWORD);
		JsonObject remoteTopic = getGeoObjectByTopicId(getID());
		// update major facets of existing geo-object (without bezirk and bezirksregion)
		if (remoteTopic != null) {
			String remoteTopicId = parseGeoObjectParentId(remoteTopic);
			System.out.println("INFO: UPDATE " + getName() + " in Famportal-Instance - Session-ID: " + validSessionId);
			updateRemoteTopicFacets(remoteTopicId, getRelatedWorkspaceCriterias());
			// 2) Update Geo Object Name
			String remoteTopicNameId = parseGeoObjectNameId(remoteTopic);
			postTopicName(remoteTopicNameId);
			// 3) Update Geo Object Address Topic // ### find out if address value has changed
			String remoteTopicAddressId = parseGeoObjectAddressId(remoteTopic);
			postTopicAddress(remoteTopicAddressId, false); // without changing the geoCoordinates
		}
	}

	/**
	 * Updates major data-facets of an edited (or later created and not
	 * initially migrated?) geo-object at the service endpoint.
	 */
	public void updateRemoteTopicFacets(String remoteTopicId, SearchCriteria[] criterias) {
		// 1) Update most of the stuff (except bezirk, bezirksregion and geo-coordinate)
		postPropertyFacets(getID(), remoteTopicId); // beschreibung, öffnungszeiten, sonstiges, lor nummer
		postWebpageFacet(getID(), remoteTopicId);
		postContactFacets(getID(), remoteTopicId);
		postImageFileFacet(getID(), remoteTopicId);
		postAgencyFacet(getID(), remoteTopicId);
		// ### Not updating Bezirks and Bezirksregion-Facet
		if (criterias != null) {
			for (int k = 0; k < criterias.length; k++) {
				SearchCriteria criteria = criterias[k];
				postCategoryFacets(getID(), remoteTopicId, criteria);
			}
		} else {
			System.out.println(" ------");
			System.out.println("WARNING: Failed to load necessary SearchCriterias for geo-object and workspace.. ");
			System.out.println(" ------");
		}
	}

	/**
	 * @return boolean	If this GeoObject is part of the (to Famportal-Instance migrated workspaces.
	 */
	public boolean isPartOfMigratedWorkspaces() {
		// ### consider that types and topics may be shared across workspaces .. and citymaps
		String typeId = getType();
		if (typeId.equals("tt-ka-sozialeinrichtung") || // TeSch
			typeId.equals("t-307980") || typeId.equals("t-253476") || // FaSz + Mitte
			typeId.equals("t-188958") || typeId.equals("t-229909") || // Li, TreKoe
			typeId.equals("t-96793") || typeId.equals("t-202842") || // Spa + CW
			typeId.equals("t-239973") || typeId.equals("t-181717")) { // FriKre + NK
			// typeId.equals("t-230507")) { // Defused PK
			return true;
		} else {
			return false;
		}
	}

	/**
	 * If topic present, this methods deletes it in the Famportal-Instance.
	 */
	public void deleteRemoteTopic() {
		// 1) Get HTTP Session first
		getHTTPSession(KA2_ADMIN_PASSWORD);
		// 2) Get Remote Topic ID
		JsonObject remoteTopic = getGeoObjectByTopicId(getID());
		if (remoteTopic != null) {
			String remoteTopicId = parseGeoObjectParentId(remoteTopic);
			try {
				HttpURLConnection connection = (HttpURLConnection) new URL(CORE_TOPIC_ENDPOINT
					+ remoteTopicId).openConnection();
				connection.setRequestProperty("Cookie", "dm4_workspace_id=" + KA2_DEFAULT_WS_TOPIC_ID
					+ "; JSESSIONID=" + validSessionId + ";");
				connection.setRequestProperty("Content-Type", REQUEST_CONTENT_TYPE);
				connection.setRequestMethod("DELETE");
				// 2) Check the request for error status (200 is the expected response here).
				if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
					System.out.println(" ### WARNING: Delete " + remoteTopicId + " (" + connection.getResponseCode() + ")");
				} else if (connection.getResponseCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
					validSessionId = null;
				} else {
					System.out.println("INFO: DELETE " + getName() + " at KA 2 Web Service Endpoint - SUCCESS");
				}
			} catch (UnknownHostException uke) {
				System.out.println("*** [GeoObjectTopic] connecting to " + KA2_SERVICE_URL + " cause is: " + uke.getCause());
			} catch (Exception ex) {
				ex.printStackTrace();
				System.out.println("*** [GeoObjectTopic] deleteRemoteTopic encountered problem: " + ex.getMessage());
			}
		}
	}



	// ---------------------------------------------------------------------------- Kiezatlas 2 Private Upgrade Methods



	private JsonObject postNewTopic(BaseTopic topic) {
		String result = null;
		JsonObject object = null;
		try {
			// 1) Get Geo Object's Address // if this look-up fails the whole topic will not be created
			BaseTopic address = getAddress();
			// 2) Check if streetNr already exists (KA2) and Postal Code
			String postalCode = "", streetNr = "";
			if (address != null) {
				streetNr = cleanUpStreetNrValues(address.getName());
				postalCode = as.getTopicProperty(address, PROPERTY_POSTAL_CODE);
			} else {
				// ..) try to fetch streetNr and postalCode of geo-object directly
				streetNr = as.getTopicProperty(topic, PROPERTY_STREET);
				postalCode = as.getTopicProperty(topic, PROPERTY_POSTAL_CODE);
			}
			String streetNrId = getStreetNrEntityByValue(streetNr);
			if (streetNrId != null) {
				// .. ) Reference the KA 2 Street Nr Topic
				streetNr = "ref_id:" + streetNrId;
			} else {
				System.out.println("> Creating new Street Topic for value \"" + streetNr + "\"");
			}
			postalCode = cleanUpPostalCodeValue(postalCode);
			// ..) Check if postalCode exists (KA 2)
			String existingPostalCodeId = getPostalCodeEntityByValue(postalCode);
			if (existingPostalCodeId != null) {
				// .. ) Reference the KA 2 Postal Code Topic
				postalCode = "ref_id:" + existingPostalCodeId;
			} else {
				System.out.println("> Creating new Postal Code Topic for value \"" + postalCode + "\"");
			}
			// 4) Fetch the correct cityName (KA1)
			String city = getCity(); // getGeoObjectCityName(topic, address);
			// 5) Perform create request ... // .. ### ?include_childs=true
			URLConnection connection = new URL(KA2_SERVICE_URL + "/core/topic").openConnection();
			connection.setRequestProperty("Cookie", "dm4_workspace_id=" + KA2_DEFAULT_WS_TOPIC_ID
				+ "; JSESSIONID=" + validSessionId + "; dm4_no_geocoding=true;");
			connection.setRequestProperty("Content-Type", REQUEST_CONTENT_TYPE);
			connection.setDoOutput(true);
			OutputStream output = null;
			output = connection.getOutputStream();
			JsonWriter writer = new JsonWriter(new OutputStreamWriter(output, "UTF-8"));
			// 6) Writes "Geo Object Name" and its complete "Address"
			writeGeoObject(writer, topic, streetNr, postalCode, city, "Deutschland");
			writer.close();
			// 7) Check the response
			BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8")); //
			String line = "";
			if (rd.ready()) {
				line = rd.readLine();
			}
			result = line;
			object = getTopicObjectFromResponse(result);
			rd.close();
			return object;
		} catch (UnknownHostException uke) {
			System.out.println("*** [GeoObjectTopic] connecting to " + KA2_SERVICE_URL + " cause is: " + uke.getCause());
			return null;
		} catch (Exception ex) {
			// fixme: is thrown, e.g. if topic has no address (currently we log but ignore it)
			ex.printStackTrace();
			System.out.println("*** [GeoObjectTopic] postTopic encountered problem: " + ex.getMessage());
			return null;
		}
	}
	
	private void postTopicAddress(String remoteAddressTopicId, boolean geoCodeRequest) {
		try {
			// 1) Get Geo Object's Address // if this look-up fails the whole topic will not be created
			BaseTopic address = getAddress();
			// 2) Check if streetNr already exists (KA2) and Postal Code
			String postalCode = "", streetNr = "";
			if (address != null) {
				streetNr = cleanUpStreetNrValues(address.getName());
				postalCode = as.getTopicProperty(address, PROPERTY_POSTAL_CODE);
			} else {
				// ..) try to fetch streetNr and postalCode of geo-object directly
				streetNr = as.getTopicProperty(as.getLiveTopic(getID(), 1), PROPERTY_STREET);
				postalCode = as.getTopicProperty(as.getLiveTopic(getID(), 1), PROPERTY_POSTAL_CODE);
			}
			String streetNrId = getStreetNrEntityByValue(streetNr);
			if (streetNrId != null) {
				// .. ) Reference the KA 2 Street Nr Topic
				streetNr = "ref_id:" + streetNrId;
			}
			postalCode = cleanUpPostalCodeValue(postalCode);
			// ..) Check if postalCode exists (KA 2)
			String existingPostalCodeId = getPostalCodeEntityByValue(postalCode);
			if (existingPostalCodeId != null) {
				// .. ) Reference the KA 2 Postal Code Topic
				postalCode = "ref_id:" + existingPostalCodeId;
			} else {
				System.out.println("> Creating new Postal Code Topic for value \"" + postalCode + "\"");
			}
			// 4) Fetch the correct cityName (KA1)
			String city = getCity(); // getGeoObjectCityName(topic, address);
			// 5) Perform create request ...
			String geoCodingCookie = "";
			if (geoCodeRequest) geoCodingCookie = " dm4_no_geocoding=true;";
			HttpURLConnection connection = (HttpURLConnection) new URL(CORE_TOPIC_ENDPOINT
				+ remoteAddressTopicId).openConnection();
			connection.setRequestProperty("Cookie", "dm4_workspace_id=" + KA2_DEFAULT_WS_TOPIC_ID
				+ "; JSESSIONID=" + validSessionId + ";" + geoCodingCookie );
			connection.setRequestProperty("Content-Type", REQUEST_CONTENT_TYPE);
			connection.setRequestMethod("PUT");
			connection.setDoOutput(true);
			OutputStream output = null;
			output = connection.getOutputStream();
			JsonWriter writer = new JsonWriter(new OutputStreamWriter(output, "UTF-8"));
			writeGeoObjectAddress(writer, remoteAddressTopicId, streetNr, postalCode, city, "Deutschland");
			writer.close();
			// 6) Check the request for error status (200 is be the expected response here).
			checkRequestResponseCode("Set Address", getID(), connection.getResponseCode());
		} catch (UnknownHostException uke) {
			System.out.println("*** [GeoObjectTopic] connecting to " + KA2_SERVICE_URL + " cause is: " + uke.getCause());
		} catch (Exception ex) {
			// fixme: is thrown, e.g. if topic has no address (currently we log but ignore it)
			System.out.println("*** [GeoObjectTopic] postTopicAddress encountered problem: " + ex.getMessage());
		}
	}

	private void postTopicName(String remoteAddressTopicId) {
		try {
			// 1) Perform update request ...
			HttpURLConnection connection = (HttpURLConnection) new URL(CORE_TOPIC_ENDPOINT
				+ remoteAddressTopicId).openConnection();
			connection.setRequestProperty("Cookie", "dm4_workspace_id=" + KA2_DEFAULT_WS_TOPIC_ID
				+ "; JSESSIONID=" + validSessionId + ";");
			connection.setRequestProperty("Content-Type", REQUEST_CONTENT_TYPE);
			connection.setRequestMethod("PUT");
			connection.setDoOutput(true);
			OutputStream output = null;
			output = connection.getOutputStream();
			JsonWriter writer = new JsonWriter(new OutputStreamWriter(output, "UTF-8"));
			writeGeoObjectName(writer, remoteAddressTopicId, getName());
			writer.close();
			// 2) Check the request for error status (200 is the expected response here).
			checkRequestResponseCode("Set Name", getName(), connection.getResponseCode());
		} catch (UnknownHostException uke) {
			System.out.println("*** [GeoObjectTopic] connecting to " + KA2_SERVICE_URL + " cause is: " + uke.getCause());
		} catch (Exception ex) {
			// fixme: is thrown, e.g. if topic has no address (currently we log but ignore it)
			System.out.println("*** [GeoObjectTopic] postTopicNameencountered problem: " + ex.getMessage());
		}
	}

	private void postGeoCoordinateFacet(String oldTopicId, String addressTopicId) {
		String latValue = as.getTopicProperty(oldTopicId, 1, PROPERTY_GPS_LAT);
		String lngValue = as.getTopicProperty(oldTopicId, 1, PROPERTY_GPS_LONG);
		double latitude = 0;
		double longitude = 0;
		if (latValue.isEmpty() || lngValue.isEmpty()) {
			//
			System.out.println("--------");
			System.out.println("WARNING: Geo Coordinates of Topic "
				+ as.getLiveTopic(oldTopicId, 1).getName() + " is EMPTY");
			System.out.println("--------");
			// If GPS is not set in KA 1 we can correct this through turning geoCoding=on
			postTopicAddress(addressTopicId, true);
		} else {
			try {
				latitude = Double.parseDouble(latValue);
				longitude = Double.parseDouble(lngValue);
				setGeoCoordinateFacet(latitude, longitude, addressTopicId);
			} catch (NumberFormatException ne) {
				// If GPS is not set in KA 1 we can correct this through turning geoCoding=on
				postTopicAddress(addressTopicId, true);
			}
		}
	}

	/** Sets manually a Geo Coordinate topic for a given Address Topic at the service endpoint. */
	private void setGeoCoordinateFacet(double latValue, double lngValue, String addressTopicId) {
		String geoCoordinateFacetTypeUri = "dm4.geomaps.geo_coordinate_facet";
		try {
			HttpURLConnection connection = (HttpURLConnection) new URL(FACET_TOPIC_ENDPOINT
				+ geoCoordinateFacetTypeUri + "/topic/" + addressTopicId).openConnection();
			connection.setRequestProperty("Cookie", "dm4_workspace_id=" + KA2_DEFAULT_WS_TOPIC_ID
				+ "; JSESSIONID=" + validSessionId + ";");
			connection.setRequestProperty("Content-Type", REQUEST_CONTENT_TYPE);
			connection.setRequestMethod("PUT");
			connection.setDoOutput(true);
			OutputStream output = null;
			output = connection.getOutputStream();
			JsonWriter writer = new JsonWriter(new OutputStreamWriter(output, "UTF-8"));
			writeGeoCoordinateFacet(writer, latValue, lngValue);
			writer.close();
			// 2) Check the request for error status (204 is the expected response here).
			if (connection.getResponseCode() != HttpURLConnection.HTTP_NO_CONTENT) {
				System.out.println(" --------");
				System.out.println(" WARNING: setGeoCoordinateFacets for " + geoCoordinateFacetTypeUri + " did not succceed for "
					+ as.getLiveTopic(addressTopicId, 1).getName()
					+ " HTTPStatusCode: " + connection.getResponseCode());
				System.out.println(" --------");
			}
		} catch (UnknownHostException uke) {
			System.out.println("*** [GeoObjectTopic] connecting to " + KA2_SERVICE_URL + " cause is: " + uke.getCause());
		} catch (Exception ex) {
			System.out.println("*** [GeoObjectTopic] setGeoCoordinateFacets for "
				+ geoCoordinateFacetTypeUri + " encountered problem: " + ex.getMessage());
		}
	}

	private void postPropertyFacets(String oldTopicId, String newTopicId) {
		// Set Sonstiges Facet => Beschreibung
		String sonstigesTypeURI = "ka2.beschreibung.facet"; // HTML
		String sonstigesChildTypeURI = "ka2.beschreibung";
		String sonstigesValue = as.getTopicProperty(oldTopicId, 1, PROPERTY_SONSTIGES);
		setSimpleFacetValue(oldTopicId, newTopicId, sonstigesTypeURI, sonstigesChildTypeURI, sonstigesValue);
		// Set Oeffnungszeiten Facet
		String openingTypeURI = "ka2.oeffnungszeiten.facet"; // Text
		String openingChildTypeURI = "ka2.oeffnungszeiten";
		String openingHours = as.getTopicProperty(oldTopicId, 1, PROPERTY_OEFFNUNGSZEITEN);
		setSimpleFacetValue(oldTopicId, newTopicId, openingTypeURI, openingChildTypeURI, openingHours);
		// Set Administrator Infos => Sonstiges
		String adminInfosTypeURI = "ka2.sonstiges.facet";
		String adminInfosChildTypeURI = "ka2.sonstiges"; // HTML
		String adminInfos = as.getTopicProperty(oldTopicId, 1, PROPERTY_ADMINISTRATION_INFO);
		setSimpleFacetValue(oldTopicId, newTopicId, adminInfosTypeURI, adminInfosChildTypeURI, adminInfos);
		// Set LOR Facet (if LOR-Number was set in field adminInfos)
		String lorNumber = "";
		if (adminInfos != null && adminInfos.indexOf("lor/analysen/") != -1) {
			lorNumber = parseLORNumber(adminInfos);
			if (lorNumber != null) {
				lorNumber = lorNumber.trim();
				String lorFacetTypeURI = "ka2.lor_nummer.facet";
				String lorChildTypeURI = "ka2.lor_nummer";
				// 1) Fetch (possibly) existing LOR Number Topic
				String lorNumberTopicId = getLORNumberEntityByValue(lorNumber);
				if (lorNumberTopicId != null) {
					lorNumber = "ref_id:" + lorNumberTopicId;
				}
				// 2) Set (and possibly reference) LOR Number Topic
				setSimpleFacetValue(oldTopicId, newTopicId, lorFacetTypeURI, lorChildTypeURI, lorNumber);
			} else {
				System.out.println("ERROR: Strange LOR Number Value in " + adminInfos);
			}
		}
		String stichworte = as.getTopicProperty(oldTopicId, 1, PROPERTY_TAGFIELD);
		if (!stichworte.isEmpty()) {
			// Set Stichworte Facet
			String stichwortFacetTypeURI = "ka2.stichworte.facet";
			String stichwortChildTypeURI = "ka2.stichworte";
			System.out.println("INFO: Wrote Stichworte Facet " + stichworte);
			setSimpleFacetValue(oldTopicId, newTopicId, stichwortFacetTypeURI, stichwortChildTypeURI, stichworte);
		}
	}

	private void postContactFacets(String topicId, String newTopicId) {
		try {
			BaseTopic contactPerson = null;
			BaseTopic phone = null;
			BaseTopic fax = null;
			BaseTopic mailbox = null;
			// Get Contact
			try {
				contactPerson = as.getRelatedTopic(topicId, ASSOCTYPE_ASSOCIATION,
					KiezAtlas.TOPICTYPE_PERSON, 2, false);
			} catch (AmbiguousSemanticException ax) {
				contactPerson = ax.getDefaultTopic();
			} catch (DeepaMehtaException de) {
				System.out.println(" EMPTY CONTACT-PERSON in postContactFacet: " + de.getMessage());
			}
			// Get Phone
			try {
				phone = as.getRelatedTopic(topicId, ASSOCTYPE_ASSOCIATION, KiezAtlas.TOPICTYPE_PHONE_NUMBER, 2, false);
			} catch (AmbiguousSemanticException ax) {
				phone = ax.getDefaultTopic();
			} catch (DeepaMehtaException de) {
				System.out.println(" EMPTY PHONE in postContactFacet: " + de.getMessage());
			}
			// Get Fax
			try {
				fax = as.getRelatedTopic(topicId, ASSOCTYPE_ASSOCIATION, KiezAtlas.TOPICTYPE_FAX_NUMBER, 2, false);
			} catch (AmbiguousSemanticException ax) {
				fax = ax.getDefaultTopic();
			} catch (DeepaMehtaException de) {
				System.out.println(" EMPTY FAX in postContactFacet: " + de.getMessage());
			}
			// Get Mailbox
			try {
				mailbox = as.getRelatedTopic(topicId, ASSOCTYPE_ASSOCIATION,
					KiezAtlas.TOPICTYPE_EMAIL_ADDRESS, 2, false);
			} catch (AmbiguousSemanticException ax) {
				mailbox = ax.getDefaultTopic();
			} catch (DeepaMehtaException de) {
				System.out.println(" EMPTY MAIL in postContactFacet: " + de.getMessage());
			}
			//
			HttpURLConnection connection = (HttpURLConnection) new URL(FACET_TOPIC_ENDPOINT
				+ "ka2.kontakt.facet/topic/" + newTopicId).openConnection();
			connection.setRequestProperty("Cookie", "dm4_workspace_id=" + KA2_DEFAULT_WS_TOPIC_ID
				+ "; JSESSIONID=" + validSessionId + ";");
			connection.setRequestProperty("Content-Type", REQUEST_CONTENT_TYPE);
			connection.setRequestMethod("PUT");
			connection.setDoOutput(true);
			OutputStream output = null;
			output = connection.getOutputStream();
			/// BufferedOutputStream buffer = new BufferedOutputStream(output);
			JsonWriter writer = new JsonWriter(new OutputStreamWriter(output, "UTF-8"));
			//
			String phoneValue = (phone != null) ? phone.getName() : "";
			String faxValue = (fax != null) ? fax.getName() : "";
			String personaValue = (contactPerson != null) ? contactPerson.getName() : "";
			String mailboxValue = (mailbox != null) ? mailbox.getName() : "";
			//
			writer.beginObject();
			writer.name("ka2.kontakt");
			//
			writer.beginObject();
			writer.name("ka2.kontakt.telefon").value(phoneValue);
			writer.name("ka2.kontakt.fax").value(faxValue);
			writer.name("ka2.kontakt.ansprechpartner").value(personaValue);
			writer.name("ka2.kontakt.email").value(mailboxValue);
			writer.endObject();
			//
			writer.endObject();
			writer.close();
			// 2) Check the request for error status (204 is the expected response here).
			checkRequestResponseCode("postContactsFacet", topicId, connection.getResponseCode());
		} catch (UnknownHostException uke) {
			System.out.println("*** [GeoObjectTopic] connecting to " + KA2_SERVICE_URL + " cause is: " + uke.getCause());
		} catch (Exception ex) {
			ex.printStackTrace();
			System.out.println("*** [GeoObjectTopic] Setting Contact Facet encountered problem: " + ex.getMessage());
		}
	}

	private void postWebpageFacet(String topicId, String newTopicId) {
		BaseTopic webpage = null;
		try {
			webpage = as.getRelatedTopic(topicId, ASSOCTYPE_ASSOCIATION, KiezAtlas.TOPICTYPE_WEBPAGE, 2, false);
		} catch (AmbiguousSemanticException ax) {
			webpage = ax.getDefaultTopic();
		} catch (DeepaMehtaException ex) {
			System.out.println(" EMPTY WEBPAGE in postWebpageFacet: " + ex.getMessage());
		}
		if (webpage != null) {
			String websiteFacetTypeURI = "ka2.website.facet";
			String websiteFacetChildTypeURI = "dm4.webbrowser.url";
			setSimpleFacetValue(topicId, newTopicId, websiteFacetTypeURI, websiteFacetChildTypeURI,
				webpage.getName());
		}
	}

	private void postImageFileFacet(String topicId, String newTopicId) {
		//
		try {
			BaseTopic image = null;
			String fileName, fileType = "";
			try {
				image = as.getRelatedTopic(topicId, ASSOCTYPE_ASSOCIATION, KiezAtlas.TOPICTYPE_IMAGE, 2, false);
			} catch (AmbiguousSemanticException ax) {
				image = ax.getDefaultTopic();
			} catch (DeepaMehtaException ex) {
				System.out.println(" EMPTY IMAGE in postImageFileFacet: " + ex.getMessage());
			}
			// 1) Do request (if filename is not empty)
			if (image != null) {
				fileName = as.getTopicProperty(image, PROPERTY_FILE);
				if (!fileName.isEmpty()) {
					FileNameMap fileTypeMap = URLConnection.getFileNameMap();
					fileType = fileTypeMap.getContentTypeFor(fileName);
					if (fileType == null) {
						fileType = "";
					}
					//
					HttpURLConnection connection = (HttpURLConnection) new URL(FACET_TOPIC_ENDPOINT
						+ "ka2.bild.facet/topic/" + newTopicId).openConnection();
					connection.setRequestProperty("Cookie", "dm4_workspace_id=" + KA2_DEFAULT_WS_TOPIC_ID
						+ "; JSESSIONID=" + validSessionId + ";");
					connection.setRequestProperty("Content-Type", REQUEST_CONTENT_TYPE);
					connection.setRequestMethod("PUT");
					connection.setDoOutput(true);
					OutputStream output = null;
					output = connection.getOutputStream();
					JsonWriter writer = new JsonWriter(new OutputStreamWriter(output, "UTF-8"));
					// write image-file facet
					writer.beginObject();
					writer.name("dm4.files.file");
					writer.beginObject();
					writer.name("dm4.files.file_name").value(fileName);
					writer.name("dm4.files.path").value("http://www.kiezatlas.de/client/images/");
					writer.name("dm4.files.size").value("");
					writer.name("dm4.files.media_type").value(fileType);
					writer.endObject();
					writer.endObject();
					//
					writer.close();
					checkRequestResponseCode("postImageFileFacet", topicId, connection.getResponseCode());
				}
			}
		} catch (UnknownHostException uke) {
			System.out.println("*** [GeoObjectTopic] connecting to " + KA2_SERVICE_URL + " cause is: " + uke.getCause());
		} catch (Exception ex) {
			ex.printStackTrace();
			System.out.println("*** [GeoObjectTopic] setImageFileFacet encountered problem: " + ex.getMessage());
		}
	}

	/** Check the request for error status (204 is the expected response here). */
	private void checkRequestResponseCode(String context, String topic, int code) {
		String topicName = topic;
		if (topic.startsWith("t-")) topicName = as.getLiveTopic(topic, 1).getName();
		switch (code) {
			case HttpURLConnection.HTTP_NO_CONTENT:
			case HttpURLConnection.HTTP_OK:
				// OK, fine.
				System.out.println(" ### No Content/OK: " +context+ " to " + topicName + " ran FINE!");
				break;
			case HttpURLConnection.HTTP_UNAUTHORIZED:
				System.out.println(" ### UNAUTHORIZED: " +context+ " did not succceed cause of invalid HTTP Session ");
				validSessionId = null;
				break;
			case HttpURLConnection.HTTP_INTERNAL_ERROR:
				System.out.println(" ### INTERNAL_ERROR: "+context+" did produce InternalServerError for " + topicName);
				break;
			default:
				System.out.println(" ### ERROR: "+context+" did produce HTTP StatusCode "
					+ code + " for " + topicName);
				break;
		}
	}

	private void postAgencyFacet(String topicId, String newTopicId) {
		//
		try {
			BaseTopic agency = null;
			String agencyTypeValue = "ref_uri:";
			try {
				agency = as.getRelatedTopic(topicId, ASSOCTYPE_ASSOCIATION, KiezAtlas.TOPICTYPE_AGENCY, 2, false);
				String agencyType = as.getTopicProperty(agency, PROPERTY_AGENCY_KIND);
				if (agencyType.equals("kommunal")) {
					agencyTypeValue += "ka2.traeger.art.kommunal";
				} else if (agencyType.equals("frei")) {
					agencyTypeValue += "ka2.traeger.art.frei";
				} else {
					agencyTypeValue = ""; //  set new empty-topic
				}
			} catch (AmbiguousSemanticException ax) {
				agency = ax.getDefaultTopic();
			} catch (DeepaMehtaException ex) {
				System.out.println(" EMPTY AGENCY in postTraegerFacet: " + ex.getMessage());
			}
			// 1) Do request (if filename is not empty)
			if (agency != null && !agency.getName().isEmpty()) {
				//
				HttpURLConnection connection = (HttpURLConnection) new URL(FACET_TOPIC_ENDPOINT
					+ "ka2.traeger.facet/topic/" + newTopicId).openConnection();
				connection.setRequestProperty("Cookie", "dm4_workspace_id=" + KA2_DEFAULT_WS_TOPIC_ID
					+ "; JSESSIONID=" + validSessionId + ";");
				connection.setRequestProperty("Content-Type", REQUEST_CONTENT_TYPE);
				connection.setRequestMethod("PUT");
				connection.setDoOutput(true);
				OutputStream output = null;
				output = connection.getOutputStream();
				JsonWriter writer = new JsonWriter(new OutputStreamWriter(output, "UTF-8"));
				// write agency facet
				writer.beginObject();
				writer.name("ka2.traeger");
				writer.beginObject();
				writer.name("ka2.traeger.name").value(agency.getName());
				writer.name("ka2.traeger.art").value(agencyTypeValue);
				writer.endObject();
				writer.endObject();
				//
				writer.close();
				// 2) Check the request for error status (204 is the expected response here).
				if (connection.getResponseCode() != HttpURLConnection.HTTP_NO_CONTENT) {
					System.out.println(" --------");
					System.out.println(" WARNING: setTraegerFileFacet did not succceed for "
						+ as.getLiveTopic(topicId, 1).getName()
						+ " HTTPStatusCode: " + connection.getResponseCode());
					System.out.println(" --------");
				}
			}
		} catch (UnknownHostException uke) {
			System.out.println("*** [GeoObjectTopic] connecting to " + KA2_SERVICE_URL + " cause is: " + uke.getCause());
		} catch (Exception ex) {
			ex.printStackTrace();
			System.out.println("*** [GeoObjectTopic] setImageFileFacet encountered problem: " + ex.getMessage());
		}
	}

	private void postBezirksregionFacet(String oldTopicId, String newTopicId, String mapAlias) {
		String facetValue = "ref_uri:ka2.bezirksregion." + mapAlias;
		setSimpleFacetValue(oldTopicId, newTopicId, "ka2.bezirksregion.facet", "ka2.bezirksregion",
			facetValue);
	}

	private void postBezirksFacet(String oldTopicId, String newTopicId, String bezirkName) {
		HashMap<String, String> bezirke = new HashMap<String, String>();
		bezirke.put("Mitte", "ka2.bezirk.mitte");
		bezirke.put("Charlottenburg-Wilmersdorf", "ka2.bezirk.charlottenburg-wilmersdorf");
		bezirke.put("Lichtenberg", "ka2.bezirk.lichtenberg");
		bezirke.put("Pankow", "ka2.bezirk.pankow");
		bezirke.put("Spandau", "ka2.bezirk.spandau");
		bezirke.put("Tempelhof", "ka2.bezirk.tempelhof-schoeneberg");
		bezirke.put("Treptow-Köpenick", "ka2.bezirk.tk");
		bezirke.put("Friedrichshain-Kreuzberg", "ka2.bezirk.friedrichshain-kreuzberg");
		bezirke.put("Neukoelln", "ka2.bezirk.neukoelln");
		bezirke.put("Familienatlas Steglitz-Zehlendorf", "ka2.bezirk.familienatlas-sz");
		String facetValue = "ref_uri:" + bezirke.get(bezirkName);
		System.out.println("Setting Bezirks Facet: \"" + facetValue + "\"");
		setSimpleFacetValue(oldTopicId, newTopicId, "ka2.bezirk.facet", "ka2.bezirk", facetValue);
	}

	private void postCategoryFacets(String oldTopicId, String newTopicId, SearchCriteria criteria) {
		String criteriaFacetTypeUri = Transformation.CRITERIA_MAP.get(criteria.criteria.getID());
		try {
			Vector<BaseTopic> topics = this.as.getRelatedTopics(oldTopicId, ASSOCTYPE_ASSOCIATION, criteria.criteria.getID(), 2);
			//
			if (topics.size() > 0) {
				for (BaseTopic topic : topics) {
					String categoryUri = (String) Transformation.CATEGORY_MAP.get(topic.getID());
					if ((categoryUri == null) && (topics.size() == 1)) {
						System.out.println(" EMPTY Categories, skipping operation");
						return;
					}
				}
				HttpURLConnection connection = (HttpURLConnection) new URL(FACET_TOPIC_ENDPOINT
					+ criteriaFacetTypeUri + "/topic/" + newTopicId).openConnection();
				connection.setRequestProperty("Cookie", "dm4_workspace_id=" + KA2_DEFAULT_WS_TOPIC_ID
					+ "; JSESSIONID=" + validSessionId + ";");
				connection.setRequestProperty("Content-Type", REQUEST_CONTENT_TYPE);
				connection.setRequestMethod("PUT");
				connection.setDoOutput(true);
				OutputStream output = null;
				output = connection.getOutputStream();
				JsonWriter writer = new JsonWriter(new OutputStreamWriter(output, "UTF-8"));
				writeCategoryFacets(writer, oldTopicId, criteria);
				//
				writer.close();
				// 2) Check the request for error status (204 is the expected response here).
				if (connection.getResponseCode() != HttpURLConnection.HTTP_NO_CONTENT) {
					System.out.println(" --------");
					System.out.println(" WARNING: setCategoryFacets for " + criteriaFacetTypeUri + " did not succceed for "
						+ as.getLiveTopic(oldTopicId, 1).getName()
						+ " HTTPStatusCode: " + connection.getResponseCode());
					System.out.println(" --------");
				}
			} else {
				System.out.println("  Skipping to PUT Category Facets for "
					+ criteriaFacetTypeUri + " - No categories related.");
			}
		} catch (UnknownHostException uke) {
			System.out.println("*** [GeoObjectTopic] connecting to " + KA2_SERVICE_URL + " cause is: " + uke.getCause());
		} catch (Exception ex) {
			ex.printStackTrace();
			System.out.println("*** [GeoObjectTopic] setCategoryFacets for "
				+ criteriaFacetTypeUri + " encountered problem: " + ex.getMessage());
		}
	}

	/** Note: Firing a delete is obviously not needed during updating of remote facets. */
	private void deleteCategoryFacetTopics(String facetTypeUri, JsonArray categoryTopics, String newTopicId) {
		try {
			HttpURLConnection connection = (HttpURLConnection) new URL(FACET_TOPIC_ENDPOINT
				+ facetTypeUri + "/topic/" + newTopicId).openConnection();
			connection.setRequestProperty("Cookie", "dm4_workspace_id=" + KA2_DEFAULT_WS_TOPIC_ID
				+ "; JSESSIONID=" + validSessionId + ";");
			connection.setRequestProperty("Content-Type", REQUEST_CONTENT_TYPE);
			connection.setRequestMethod("PUT");
			connection.setDoOutput(true);
			OutputStream output = null;
			output = connection.getOutputStream();
			JsonWriter writer = new JsonWriter(new OutputStreamWriter(output, "UTF-8"));
			writer.beginObject();
			writer.name(facetTypeUri);
			writer.beginArray();
			for (int i = 0; i < categoryTopics.size(); i++) {
				JsonElement categoryTopicElement = categoryTopics.get(i);
				JsonObject categoryTopic = categoryTopicElement.getAsJsonObject();
				JsonElement categoryTopicId = categoryTopic.get("id");
				writer.value("del_id:" + categoryTopicId.getAsString());
			}
			writer.endArray();
			writer.endObject();
			writer.close();
			// 2) Check the request for error status (204 is the expected response here).
			checkRequestResponseCode("deleteCategoryFacetTopics ", getID(), connection.getResponseCode());
		} catch (UnknownHostException uke) {
			System.out.println("*** [GeoObjectTopic] connecting to " + KA2_SERVICE_URL + " cause is: " + uke.getCause());
		} catch (Exception ex) {
			ex.printStackTrace();
			System.out.println("*** [GeoObjectTopic] deleteCategoryFacetTopics for "
				+ facetTypeUri + " encountered problem: " + ex.getMessage());
		}
	}

	private void writeCategoryFacets(JsonWriter writer, String topicId, SearchCriteria criteria) throws IOException {
		String criteriaFacetTypeUri = Transformation.CRITERIA_MAP.get(criteria.criteria.getID());
		String criteriaTopicTypeId = criteria.criteria.getID();
		Vector<BaseTopic> topics = as.getRelatedTopics(topicId, ASSOCTYPE_ASSOCIATION, criteriaTopicTypeId, 2);
		//
		writer.beginObject();
		writer.name(criteriaFacetTypeUri);
		writer.beginArray();
		for (BaseTopic topic : topics) {
			String categoryUri = (String) Transformation.CATEGORY_MAP.get(topic.getID());
			if (categoryUri != null) {
				writer.value("ref_uri:" + categoryUri);
			}
		}
		writer.endArray();
		writer.endObject();
	}

	private void setSimpleFacetValue(String oldTopicId, String topicId, String facetTypeUri,
		String facetChildTypeUri, String facetValue) {
		//
		if (!facetValue.isEmpty() && !topicId.isEmpty() && !facetTypeUri.isEmpty()) {
			try {
				HttpURLConnection connection = (HttpURLConnection) new URL(FACET_TOPIC_ENDPOINT
					+ facetTypeUri + "/topic/" + topicId).openConnection();
				connection.setRequestProperty("Cookie", "dm4_workspace_id=" + KA2_DEFAULT_WS_TOPIC_ID
					+ "; JSESSIONID=" + validSessionId + ";");
				connection.setRequestProperty("Content-Type", REQUEST_CONTENT_TYPE);
				connection.setRequestMethod("PUT");
				connection.setDoOutput(true);
				OutputStream output = null;
				output = connection.getOutputStream();
				JsonWriter writer = new JsonWriter(new OutputStreamWriter(output, "UTF-8"));
				writeNewFacetValue(writer, facetValue, facetChildTypeUri);
				writer.close();
				// 2) Check the request for error status (204 is the expected response here).
				if (connection.getResponseCode() != HttpURLConnection.HTTP_NO_CONTENT) {
					System.out.println(" --------");
					System.out.println(" WARNING: setSimpleFacetValue \"" + facetTypeUri + "\" did not succceed for "
						+ as.getLiveTopic(oldTopicId, 1).getName() + " HTTP Status Code: "
						+ connection.getResponseCode());
					System.out.println(" --------");
				}
			} catch (UnknownHostException uke) {
				System.out.println("*** [GeoObjectTopic] connecting to "
					+ KA2_SERVICE_URL + " cause is: " + uke.getCause());
			} catch (Exception ex) {
				System.out.println("*** [GeoObjectTopic] Setting Simple Facet Value encountered problem: " + ex.getMessage()
					+ " at Geo Object \"" + as.getLiveTopic(oldTopicId, 1).getName() + "\"");
			}
		}
	}

	private void writeGeoObject(JsonWriter writer, BaseTopic topic, String streetNr, String postalCode,
		String city, String country) throws IOException {
		// 1) construct refs if necessary
		String cityValue = city;
		if (city.equals("Berlin") || city.equals(" Berlin") || city.equals("Berlin ")) {
			cityValue = "ref_uri:ka2.city.berlin";
		}
		String countryValue = country;
		if (country.equals("Deutschland") || country.equals(" Deutschland") || country.equals("Deutschland ")) {
			countryValue = "ref_uri:ka2.country.deutschland";
		}
		// 2) write json-object with topicId in URI
		writer.beginObject();
		writer.name("uri").value(KA2_TOPIC_URI_PREFIX + topic.getID());
		writer.name("type_uri").value("ka2.geo_object");
		writer.name("childs");
		writer.beginObject();
		writer.name("ka2.geo_object.name").value(topic.getName());
		writer.name("dm4.contacts.address");
		writer.beginObject();
		writer.name("dm4.contacts.street").value(streetNr);
		writer.name("dm4.contacts.postal_code").value(postalCode);
		writer.name("dm4.contacts.city").value(cityValue);
		writer.name("dm4.contacts.country").value(countryValue);
		writer.endObject();
		writer.endObject();
		writer.endObject();
	}

	private void writeGeoObjectAddress(JsonWriter writer, String remoteAddressTopicId, String streetNr, String postalCode,
		String city, String country) throws IOException {
		// 1) construct refs if necessary
		String cityValue = city;
		if (city.equals("Berlin") || city.equals(" Berlin") || city.equals("Berlin ")) {
			cityValue = "ref_uri:ka2.city.berlin";
		}
		String countryValue = country;
		if (country.equals("Deutschland") || country.equals(" Deutschland") || country.equals("Deutschland ")) {
			countryValue = "ref_uri:ka2.country.deutschland";
		}
		// 2) write json-object with topicId in URI
		writer.beginObject();
		writer.name("id").value(remoteAddressTopicId);
		writer.name("type_uri").value("dm4.contacts.address");
		writer.name("childs");
		writer.beginObject();
		writer.name("dm4.contacts.street").value(streetNr);
		writer.name("dm4.contacts.postal_code").value(postalCode);
		writer.name("dm4.contacts.city").value(cityValue);
		writer.name("dm4.contacts.country").value(countryValue);
		writer.endObject();
		writer.endObject();
	}

	private void writeGeoObjectName(JsonWriter writer, String remoteNameTopicId, String name) throws IOException {
		// 1) write new geo-object name
		writer.beginObject();
		writer.name("id").value(remoteNameTopicId);
		writer.name("type_uri").value("ka2.geo_object.name");
		writer.name("value").value(name);
		writer.endObject();
	}

	private void writeGeoCoordinateFacet(JsonWriter writer, double latValue, double lngValue) throws IOException {
		// 1) write new geo-coordinate facet
		writer.beginObject();
		writer.name("dm4.geomaps.geo_coordinate");
		writer.beginObject();
		writer.name("dm4.geomaps.longitude").value(lngValue);
		writer.name("dm4.geomaps.latitude").value(latValue);
		writer.endObject();
		writer.endObject();
	}

	private void writeNewFacetValue(JsonWriter writer, String facetValue, String facetChildTypeUri)
		throws IOException {
		// re-write line breaks for our HMTL fields
		if (facetChildTypeUri.equals("ka2.sonstiges")
			|| facetChildTypeUri.equals("ka2.oeffnungszeiten")
			|| facetChildTypeUri.equals("ka2.beschreibung")) {
			if (facetValue.indexOf("\r") != -1) {
				facetValue = facetValue.replaceAll("\r", "<br/>");
			}
			facetValue = "<p>" + facetValue + "</p>";
		}
		writer.beginObject();
		writer.name(facetChildTypeUri).value(facetValue);
		writer.endObject();
	}



	// ------------------------------------------------------------------------------------ Kiezatlas 2 Service Getters

	// ### Refactor Postal Code, Street and LOR Number into one Getter Method
	private String getPostalCodeEntityByValue(String postalCode) {
		String result = null;
		String topicId = null;
		try {
			// 1) Create GET-Request
			if (!postalCode.isEmpty()) {
				URLConnection connection = new URL(CORE_TOPIC_SEARCH_ENDPOINT
					+ postalCode + "&field=dm4.contacts.postal_code").openConnection();
				connection.setRequestProperty("Content-Type", REQUEST_CONTENT_TYPE);
				connection.setRequestProperty("Cookie", "dm4_workspace_id=" + KA2_DEFAULT_WS_TOPIC_ID
					+ "; JSESSIONID=" + validSessionId + ";");
				// 2) Read in the response
				BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8")); //
				String line = "";
				if (rd.ready()) {
					line = rd.readLine();
				}
				result = line;
				topicId = getFirstTopicIdFromListResponse(result);
				rd.close();
			}
		} catch (UnknownHostException uke) {
			System.out.println("*** [GeoObjectTopic Upgrade] connecting to " + KA2_SERVICE_URL + " cause is: " + uke.getCause());
			return null;
		} catch (Exception ex) {
			System.out.println("*** [GeoObjectTopic Upgrade] getPostalCodeEntityByValue encountered problem: " + ex.getMessage());
		}
		return topicId;
	}

	private String getStreetNrEntityByValue(String streetNr) {
		String result = null;
		String topicId = null;
		try {
			if (!streetNr.isEmpty()) {
				streetNr = URLEncoder.encode(streetNr, "UTF-8");
				URLConnection connection = new URL(CORE_TOPIC_SEARCH_ENDPOINT
					+ "\"" + streetNr + "\"&field=dm4.contacts.street").openConnection();
				connection.setRequestProperty("Content-Type", REQUEST_CONTENT_TYPE);
				connection.setRequestProperty("Cookie", "dm4_workspace_id=" + KA2_DEFAULT_WS_TOPIC_ID
					+ "; JSESSIONID=" + validSessionId + ";");
				// 2) Read in the response
				BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8")); //
				String line = "";
				if (rd.ready()) {
					line = rd.readLine();
				}
				result = line;
				topicId = getFirstTopicIdFromListResponse(result);
				rd.close();
			}
		} catch (UnknownHostException uke) {
			System.out.println("*** [GeoObjectTopic Upgrade] connecting to " + KA2_SERVICE_URL + " cause is: " + uke.getCause());
		} catch (Exception ex) {
			System.out.println("*** [GeoObjectTopic Upgrade] getStreetNr encountered problem: " + ex.getMessage());
		}
		return topicId;
	}

	private String getLORNumberEntityByValue(String lorNumber) {
		String result = null;
		String topicId = null;
		try {
			// 1) Create GET-Request //
			if (!lorNumber.isEmpty()) {
				URLConnection connection = new URL(CORE_TOPIC_SEARCH_ENDPOINT
					+ lorNumber + "&field=ka2.lor_nummer").openConnection();
				connection.setRequestProperty("Content-Type", REQUEST_CONTENT_TYPE);
				connection.setRequestProperty("Cookie", "dm4_workspace_id=" + KA2_DEFAULT_WS_TOPIC_ID
					+ "; JSESSIONID=" + validSessionId + ";");
				// 2) Read in the response
				BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8")); //
				String line = "";
				if (rd.ready()) {
					line = rd.readLine();
				}
				result = line;
				topicId = getFirstTopicIdFromListResponse(result);
				rd.close();
			}
		} catch (UnknownHostException uke) {
			System.out.println("*** [GeoObjectTopic Upgrade] connecting to " + KA2_SERVICE_URL + " cause is: " + uke.getCause());
		} catch (Exception ex) {
			System.out.println("*** [GeoObjectTopic Upgrade] getLORNumberEntityByValue encountered problem: " + ex.getMessage());
		}
		return topicId;
	}

	private JsonArray getCategoryFacetTopics(String facetTypeUri, String remoteTopicId) {
		String result = null;
		JsonArray topics = null;
		try {
			// 1) Create GET-Request //
			String url = FACET_TOPIC_MULTI_ENDPOINT + facetTypeUri + "/topic/" + remoteTopicId;
			URLConnection connection = new URL(url).openConnection();
			connection.setRequestProperty("Content-Type", REQUEST_CONTENT_TYPE);
			connection.setRequestProperty("Cookie", "dm4_workspace_id=" + KA2_DEFAULT_WS_TOPIC_ID
				+ "; JSESSIONID=" + validSessionId + ";");
			// 2) Read in the response
			BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8")); //
			String line = "";
			if (rd.ready()) {
				line = rd.readLine();
			}
			result = line;
			topics = getTopicListFromResponse(result);
			rd.close();
		} catch (UnknownHostException uke) {
			System.out.println("*** [GeoObjectTopic Upgrade] connecting to " + KA2_SERVICE_URL + " cause is: " + uke.getCause());
			return null;
		} catch (UnsupportedEncodingException ex) {
			System.out.println("### ERROR UnsupportedEncodingException during getting a categoryFacet: " + ex.getMessage());
		} catch (MalformedURLException ex) {
			System.out.println("### ERROR MalformedURLException during getting a categoryFacet: " + ex.getMessage());
		} catch (IOException ex) {
			System.out.println("### ERROR IOException during getting a categoryFacet: " + ex.getMessage());
		}
		return topics;
	}

	public JsonObject getGeoObjectByTopicId(String topicId) {
		String result = null;
		JsonObject geoObject = null;
		try {
			// 1) GET-Request
			String cookieBody = "dm4_workspace_id=" + KA2_DEFAULT_WS_TOPIC_ID + "; JSESSIONID=" + validSessionId + ";";
			HttpURLConnection connection = (HttpURLConnection) new URL(CORE_GET_BY_URI_ENDPOINT
				+ KA2_TOPIC_URI_PREFIX + topicId + "/?include_childs=true").openConnection();
			connection.setRequestProperty("Content-Type", REQUEST_CONTENT_TYPE);
			connection.setRequestProperty("Cookie", cookieBody);
			// 2) Check if auth was succesfull
			if (connection.getResponseCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
				validSessionId = null;
				System.out.println("### ERROR We could not fetch the geo-object because"
				    + "of our HTTP Session that got invalid!");
				// ### need to re-fetch the geo-object!
			}
			// 3) Read in the response
			BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8")); //
			String line = "";
			if (rd.ready()) {
				line = rd.readLine();
			}
			result = line;
			geoObject = getTopicObjectFromResponse(result);
			rd.close();
		} catch (UnknownHostException uke) {
			System.out.println("*** [GeoObjectTopic Upgrade] connecting to " + KA2_SERVICE_URL + " cause is: " + uke.getCause());
		} catch (IOException ex) {
			Logger.getLogger(GeoObjectTopic.class.getName()).log(Level.SEVERE, null, ex);
			System.out.println("### ERROR IOException during getting a geo-object: " + ex.getMessage());
		}
		return geoObject;
	}


	// --
	// --- Private KA 2 Response Helpers
	// --

	private String getFirstTopicIdFromListResponse(String body) {
		try {
			JsonElement response = new JsonParser().parse(body);
			if (response.isJsonNull()) {
				return null;
			} else {
				JsonArray elements = response.getAsJsonArray();
				if (elements.size() == 1) {
					JsonElement first = elements.get(0);
					JsonObject topicObject = first.getAsJsonObject();
					JsonElement topicObjectId = topicObject.get("id");
					return topicObjectId.getAsString();
				} else if (elements.size() > 1) {
					System.out.println(" ### SEVERE KA2 has multiple elements with the same value, something is wrong.");
					throw new Error(" KA1 Upgrade Serlvet has received multiple (identical) value topics, stopping migration.");
				}
			}
		} catch (Exception je) {
			//
			je.printStackTrace();
			System.out.println("### ERROR Could not parse "
				+ "postalCode response from KA 2 response, HTTP Response: " + body);
		}
		return null;
	}

	private JsonObject getTopicObjectFromResponse(String body) {
		try {
			JsonElement response = new JsonParser().parse(body);
			if (response.isJsonObject()) {
				JsonObject new_topic = response.getAsJsonObject();
				return new_topic;
			}
		} catch (Exception je) {
			System.out.println("### ERROR Could not parse KA 2 Ttopic cause of "
			    + je.getMessage() + "; HTTP Response: " + body);
		}
		return null;
	}

	private JsonArray getTopicListFromResponse(String body) {
		JsonArray topics = null;
		try {
			JsonElement response = new JsonParser().parse(body);
			if (response.isJsonObject()) {
				JsonObject object = response.getAsJsonObject();
				if (object.has("items")) {
					JsonElement items = object.get("items");
					topics = items.getAsJsonArray();
				} else {
					System.out.println("### WARNING: No ITEMS in response body found!");
				}
			}
		} catch (Exception je) {
			System.out.println("### ERROR [GeoObjectTopic] Could not parse list of "
				+ "topics from KA 2 "+je.getMessage()+", HTTP response: " + body);
		}
		return topics;
	}

	private String parseGeoObjectParentId(JsonObject response) {
		String errorType = "- Unknown error";
		if (response != null && response.has("id") && response.has("type_uri")) {
			JsonElement geoObjectIdElement = response.get("id");
			JsonElement geoObjectTypeElement = response.get("type_uri");
			String geoObjectTypeUri = geoObjectTypeElement.getAsString();
			if (geoObjectTypeUri.equals("ka2.geo_object")) {
				return geoObjectIdElement.getAsString();
			} else {
				errorType = "- response JsonObject not of type k2.geo_object " + geoObjectTypeUri;
			}
		} else {
			errorType = "- The endpoints response was \"" + response + "\"";
		}
		System.out.println("### ERROR [GeoObjectTopic] Parsing Response for Parent-ID FAILED, errorType: " + errorType);
		return null;
	}

	private String parseGeoObjectAddressId(JsonObject response) {
		try {
			JsonObject geoObjectComposite = response.getAsJsonObject("childs");
			if (geoObjectComposite.has("dm4.contacts.address")) {
				JsonObject geoObjectAddress = geoObjectComposite.getAsJsonObject("dm4.contacts.address");
				JsonElement addressIdElement = geoObjectAddress.get("id");
				return addressIdElement.getAsString();
			} else {
				System.out.println("### ERROR: KA 2 Geo Object was returned without Address childs");
				return null;
			}
		} catch (Exception je) {
			throw new RuntimeException(je);
		}
	}

	private String parseGeoObjectNameId(JsonObject topic) {
		try {
			JsonObject geoObjectComposite = topic.getAsJsonObject("childs");
			JsonObject geoObjectName = geoObjectComposite.getAsJsonObject("ka2.geo_object.name");
			JsonElement nameIdElement = geoObjectName.get("id");
			// System.out.println("Parsing Geo Object Name Response, ID is " + nameIdElement.getAsString());
			return nameIdElement.getAsString();
		} catch (Exception je) {
			throw new RuntimeException(je);
		}
	}

	// --- KA 2 HTTP Authentication

	private String getHTTPSession(String secret) {
		String sessionId = null;
		if (validSessionId != null) {
			return validSessionId;
		}
		try {
			// 1) POST-Authentication Request
			HttpURLConnection connection = (HttpURLConnection) new URL(HTTP_AUTH_ENDPOINT).openConnection();
			connection.setRequestProperty("Content-Type", REQUEST_CONTENT_TYPE);
			connection.setRequestMethod("POST");
			String auth_values = "admin:" + secret;
			String authentication = "Basic " + Base64.encode(auth_values.getBytes());
			connection.setRequestProperty("Authorization", authentication);
			try {
				if (connection.getResponseCode() == HttpURLConnection.HTTP_NO_CONTENT) {
					String cookie = connection.getHeaderField("Set-Cookie");
					String[] cookieValues = cookie.split(";");
					for (int i = 0; i < cookieValues.length; i++) {
						String[] pair = cookieValues[i].split("=");
						if (pair[0].equals("JSESSIONID")) {
							sessionId = pair[1];
						}
					}
					System.out.println("Authentication successfull: JSESSIONID is " + sessionId);
					validSessionId = sessionId;
					return sessionId;
				} else {
					System.out.println("Authentication NOT successfull: " + connection.getResponseCode() + " "
						+ connection.getResponseMessage() + " Cookie: " + connection.getHeaderField("Set-Cookie"));
				}
			} catch (Exception je) {
				//
				je.printStackTrace();
				System.out.println("*** [GeoObjectTopic Upgrade] error getting HTTPSession from KA 2");
			}
		} catch (UnknownHostException uke) {
			System.out.println("*** [GeoObjectTopic Upgrade] connecting to " + KA2_SERVICE_URL + " cause is: " + uke.getCause());
		} catch (Exception ex) {
			System.out.println("*** [GeoObjectTopic Upgrade] getHTTPSession encountered problem: " + ex.getMessage());
		}
		return sessionId;
	}
	
	// --- Helpers
	
	private String cleanUpPostalCodeValue(String postalCode) {
		if (postalCode.contains(" ")) {
			postalCode = postalCode.trim();
		}
		if (postalCode.contains("Berlin")) { // happens with data
			postalCode = postalCode.replaceAll("Berlin", "");
		}
		return postalCode;
	}

	private String cleanUpStreetNrValues(String streetNr) {
		streetNr = streetNr.trim();
		if (streetNr.contains("strasse")) {
			streetNr = streetNr.replace("strasse", "straße");
		}
		if (streetNr.contains(" Str.")) {
			streetNr = streetNr.replace(" Str.", " Straße");
		}
		if (streetNr.contains("-Str.")) {
			streetNr = streetNr.replace("-Str.", "-Straße");
		}
		if (streetNr.contains("str.")) {
			streetNr = streetNr.replace("str.", "straße");
		}
		return streetNr;
	}

	public SearchCriteria[] getRelatedWorkspaceCriterias() {
		// 1) Fetch topic criterias via the (very first) related workspaces.
		Vector workspaces = as.getRelatedTopics(getType(), ASSOCTYPE_USES, 1);
		BaseTopic finalWorkspace = null;
		SearchCriteria[] criterias = null;
		for (int a = 0; a < workspaces.size(); a++) {
			finalWorkspace = (BaseTopic) workspaces.get(a);
			System.out.println("    Preparing remote Geo Object update: Type of WS " + finalWorkspace.getName());
			BaseTopic workspaceMap = as.getRelatedTopic(finalWorkspace.getID(),
				ASSOCTYPE_AGGREGATION, TOPICTYPE_TOPICMAP, 2, true);
			if (workspaceMap != null) {
				Vector<BaseTopic> publishedCityMaps = cm.getViewTopics(workspaceMap.getID(), 1);
				for (int b = 0; b < publishedCityMaps.size(); b++) {
					try {
						System.out.println("INFO: Checking Citymaps in Workspace to load SearchCriterias ... ");
						CityMapTopic someCityMap = (CityMapTopic) as.getLiveTopic(publishedCityMaps.get(b).getID(), 1);
						criterias = someCityMap.getSearchCriterias();
						return criterias;
					} catch (ClassCastException ce) {
						System.out.println("INFO: Workspace has Topicmaps among " + publishedCityMaps.size() + " published ... ");
					}
				}
			}
		}
		return null;
	}

	public String[] loadGPSCoordinates(CorporateDirectives directives) {
		//
		String givenAddress = getAddressString();
		if (givenAddress.equals("")) {
			return new String[4]; // skip gps fetch for just " Berlin" without a street
		} // StringBuffer requestUrl = new StringBuffer("http://maps.google.com/maps/geo?");
		StringBuffer requestUrl = new StringBuffer("https://maps.googleapis.com/maps/api/geocode/json?");
		requestUrl.append("address=");
		requestUrl.append(convertAddressForRequest(givenAddress));
		// as maps-api documentation says, geocoding up to 2500 address does not require an api key at all
		// https://developers.google.com/maps/documentation/geocoding/ and https://developers.google.com/maps/faq
		requestUrl.append("&sensor=false&region=de"); // key=" +key+ " &output=csv
		for (int i = 0; i < 3; i++) {
			try {
				//String http = URLEncoder.encode(requestUrl.toString(), "UTF-8");
				URL url = new URL(requestUrl.toString());
				URLConnection con = url.openConnection();
				BufferedReader in = new BufferedReader(
					new InputStreamReader(
						con.getInputStream()));
				String inputLine;
				StringBuffer response = new StringBuffer();
				while ((inputLine = in.readLine()) != null) {
					response.append(inputLine);
				}
				in.close();
				// parse coordinates of the very first result item
				JsonParser parser = new JsonParser();
				JsonElement result = parser.parse(response.toString());
				JsonObject object = result.getAsJsonObject();
				JsonObject first_result = object.getAsJsonArray("results").get(0).getAsJsonObject();
				String latitude = first_result.getAsJsonObject("geometry").getAsJsonObject("location").get("lat").getAsString();
				String longitude = first_result.getAsJsonObject("geometry").getAsJsonObject("location").get("lng").getAsString();
				System.out.println("[GeoObjectTopic]: Fetched location \"" + latitude + "\", \"" + longitude + "\" for "
					+ givenAddress + " via Gecoding at Google Maps Api v3");
				String[] points = new String[4];
				points[2] = latitude;
				points[3] = longitude;
				return points;
			} catch (IOException e) {
				directives.add(DIRECTIVE_SHOW_MESSAGE, "The Google GeoCoder could not be connected. "
					+ "This GeoObject has no GPS Coordinates.", new Integer(NOTIFICATION_ERROR));
				e.printStackTrace();
				return new String[4];
			}
		}
		System.out.println("[WARNING] GeoObjectTopic (" + this.getID() + ") was not able to load coordinates for " + convertAddressForRequest(givenAddress) + " !");
		return new String[4];
	}

	/**
	 *
	 */
	public void setGPSCoordinates(CorporateDirectives directives) {
		boolean emptyLat = (as.getTopicProperty(this, PROPERTY_GPS_LAT).equals("")) ? true : false;
		boolean emptyLong = (as.getTopicProperty(this, PROPERTY_GPS_LONG).equals("")) ? true : false;
		if (!emptyLat && !emptyLong) {
			directives.add(DIRECTIVE_SHOW_MESSAGE, "Falls Sie gerade die Adresse des Datensatzes ge&auml;ndert haben " +
				"m&uuml;ssten Sie den neuen L&auml;ngen und Breitengrad " +
				"<a href=\"http://maps.google.de\" title=\"in Google Maps Rechtsklick am Ort > Was ist hier? klicken\">manuell</a> korrigieren.",
				new Integer(NOTIFICATION_DEFAULT));
		} else {
			// ### alternatively fetch city property
			String[] point = loadGPSCoordinates(directives);
			if (point.length > 3 && point[2] != null && point[3] != null) {
				if (!point[2].equals("") && !point[3].equals("")) {
					as.setTopicProperty(this, PROPERTY_GPS_LAT, point[2]);
					as.setTopicProperty(this, PROPERTY_GPS_LONG, point[3]);
					directives.add(DIRECTIVE_SHOW_MESSAGE, "Die Adresse hat " + point[2] + "," + point[3] + " als GPS Koordinaten zugewiesen bekommen.", new Integer(NOTIFICATION_DEFAULT));
					// System.out.println("[INFO] GeoObjectTopic.setGPSCoordinates(): successful to " + point[2] +"," + point[3] +" for address:" + getAddressString());
				} else {
					directives.add(DIRECTIVE_SHOW_MESSAGE, "Address could not be resolved to WGS 84 coordinates. Leaving the topic like it is. ", new Integer(NOTIFICATION_ERROR));
					// System.out.println("[WARNING] GeoObjectTopic.setGPSCoordinates(): was not successful for " + getAddressString());
				}
			} else {
				directives.add(DIRECTIVE_SHOW_MESSAGE, "Address could not be resolved to WGS 84 coordinates. Leaving the topic like it is. ", new Integer(NOTIFICATION_ERROR));
				// System.out.println("[WARNING] GeoObjectTopic.setGPSCoordinates(): was not successful for " + getAddressString());
			}
		}
	}

	private String convertAddressForRequest(String address) {
		try {
			address = URLEncoder.encode(address.toString(), "UTF-8");
		} catch (UnsupportedEncodingException ex) {
			Logger.getLogger(GeoObjectTopic.class.getName()).log(Level.SEVERE, null, ex);
		}
		return address;
	}

	private String parseLORNumber(String adminInfo) {
		String lorId = null;
		Pattern pattern = Pattern.compile("(<a href=\"\\D+)(/analysen/)(\\d+)(.pdf)(\\D+)(target=\"_blank\">)(\\D+)(</a>)");
		Matcher matcher = pattern.matcher(adminInfo);
		while (matcher.find()) {
			Pattern idPattern = Pattern.compile("(\\d+)");
			Matcher idMatch = idPattern.matcher(matcher.group());
			while (idMatch.find()) {
				lorId = idMatch.group();
			}
			if (lorId != null) {
				return lorId;
			}
		}
		return null;
	}
}
