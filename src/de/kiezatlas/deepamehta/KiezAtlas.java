package de.kiezatlas.deepamehta;

import de.deepamehta.DeepaMehtaConstants;



/**
 * Kiezatlas 1.6.8.3<br>
 * Requires DeepaMehta 2.0b8.
 * <p>
 * Last change: 23.11.2011<br>
 * J&ouml;rg Richter / Malte Rei&szlig;ig<br>
 * jri@deepamehta.de / mre@deepamehta.de
 */
public interface KiezAtlas extends DeepaMehtaConstants {



	// *****************
	// *** Constants ***
	// *****************



	// -------------------
	// --- Preferences ---
	// -------------------



	static final int SHAPE_ALPHA = 128;			// 0-transparent ... 255-opaque
	static final int SHAPE_LEGEND_HEIGHT = 17;	// in pixel



	// -----------------
	// --- Workspace ---
	// -----------------



	static final String WORKSPACE_KIEZATLAS = "t-ka-workspace";



	// -------------------
	// --- Topic Types ---
	// -------------------



	static final String TOPICTYPE_CITY = "tt-city";
	static final String TOPICTYPE_CITYMAP = "tt-ka-stadtplan";
	static final String TOPICTYPE_KIEZ_GEO = "tt-ka-geoobject";
	static final String TOPICTYPE_KIEZ_GEO_SEARCH = "tt-ka-geoobject-search";
	static final String TOPICTYPE_AGENCY = "tt-ka-traeger";
	static final String TOPICTYPE_CRITERIA = "tt-ka-kriterium";
	static final String TOPICTYPE_YADE_POINT = "tt-ka-yadepoint";
	static final String TOPICTYPE_GPS_CONVERTER = "tt-ka-gpsconverter";
	static final String TOPICTYPE_FORUM = "tt-ka-forum";
	static final String TOPICTYPE_COMMENT = "tt-ka-kommentar";
	static final String TOPICTYPE_OUTLINE_POINT = "tt-ka-outlinepoint";
	static final String TOPICTYPE_SHAPE = "tt-ka-shape";
	static final String TOPICTYPE_STYLESHEET = "tt-ka-stylesheet";
	static final String TOPICTYPE_IMPORTER_SETTINGS = "tt-ka-importersettings";



	// -------------------------
	// --- Association Types ---
	// -------------------------



	static final String ASSOCTYPE_OUTLINE = "at-ka-outline";
	static final String ASSOCTYPE_AFFILIATED = "at-ka-affiliated";
	static final String ASSOCTYPE_HOMEPAGE_LINK = "at-ka-homepage-link";
	static final String ASSOCTYPE_IMPRESSUM_LINK = "at-ka-impressum-link";



	// -------------------------------------
	// --- Semantic of Association Types ---
	// -------------------------------------



	// direction is from workspace to sub-workspace
	static final String SEMANTIC_SUB_WORKSPACE = ASSOCTYPE_COMPOSITION;

	// direction is from institution to forum
	static final String SEMANTIC_INSTITUTION_FORUM = ASSOCTYPE_ASSOCIATION;

	// direction is from forum to comment
	static final String SEMANTIC_FORUM_COMMENTS = ASSOCTYPE_ASSOCIATION;

	// direction is from workspace to shape-subtype
	static final String SEMANTIC_WORKSPACE_SHAPETYPE = ASSOCTYPE_ASSOCIATION;

	// direction is from workspace to stylesheet
	static final String SEMANTIC_WORKSPACE_STYLESHEET = ASSOCTYPE_ASSOCIATION;

	// direction is from workspace to image
	static final String SEMANTIC_WORKSPACE_SITELOGO = ASSOCTYPE_ASSOCIATION;

	// direction is from workspace to webpage
	static final String SEMANTIC_WORKSPACE_HOMEPAGELINK = ASSOCTYPE_HOMEPAGE_LINK;

	// direction is from workspace to webpage
	static final String SEMANTIC_WORKSPACE_IMPRESSUMLINK = ASSOCTYPE_IMPRESSUM_LINK;

	// direction is from user to workspac
	static final String SEMANTIC_AFFILIATED_MEMBERSHIP = ASSOCTYPE_AFFILIATED;

	// direction is arbitrary
	static final String SEMANTIC_SHAPE_OUTLINE = ASSOCTYPE_OUTLINE;





	// ------------------
	// --- Properties ---
	// ------------------



	static final String PROPERTY_CITY = "Stadt";
	static final String PROPERTY_OEFFNUNGSZEITEN = "Öffnungszeiten";
	static final String PROPERTY_SONSTIGES = "Sonstiges";
	static final String PROPERTY_ADMINISTRATION_INFO = "Administrator Infos";
	static final String PROPERTY_AGENCY_KIND = "Art";
	static final String PROPERTY_YADE_X = "YADE x";
	static final String PROPERTY_YADE_Y = "YADE y";
	static final String PROPERTY_GPS_LONG = "LONG";
	static final String PROPERTY_GPS_LAT = "LAT";
	static final String PROPERTY_LAST_MODIFIED = "Zuletzt geändert";
	static final String PROPERTY_LAST_UPDATED = "Zuletzt aktualisiert";
	//
	static final String PROPERTY_FORUM_ACTIVITION = "Aktivierung";
	static final String PROPERTY_COMMENT_AUTHOR = "Autor";
	static final String PROPERTY_COMMENT_DATE = "Datum";
	static final String PROPERTY_COMMENT_TIME = "Uhrzeit";
	//
	static final String PROPERTY_TAGFIELD = "Stichworte";
	//
	static final String PROPERTY_TARGET_WEBALIAS = "Target Web Alias";
	//
	static final String PROPERTY_CSS = "CSS";
	//
	// TOPICTYPE_IMPORTER_SETTINGS Properties
	static final String PROPERTY_ICONS_MAP = "Icons / Kategorien";
	static final String PROPERTY_ICON_CAT_DIVIDER = ":";
	static final String PROPERTY_IMPORT_CONTENT_REPORT = "Content Report Mailbox";
	static final String PROPERTY_IMPORT_SERVICE_REPORT = "Service Report Mailbox";
	// Event imported Properties
	static final String PROPERTY_EVENT_DESCRIPTION = "Beschreibung";
	static final String PROPERTY_EVENT_TIME = "Datum / Zeit";
	// Event and Engagment imported Properties
	static final String PROPERTY_PROJECT_ORIGIN_ID = "OriginId";
	static final String PROPERTY_PROJECT_LAST_MODIFIED = "Timestamp";
	static final String PROPERTY_PROJECT_ORGANISATION = "Organisation";
	static final String PROPERTY_WORKSPACE_ALIAS = "Workspace Web Alias";
	// KiezAtlas-App Property to publish citymaps in html5 mobile client
	static final String PROPERTY_MOBILE_CITYMAP = "Mobiler Stadtplan";

	// -----------------------
	// --- Property Values ---
	// -----------------------



	static final String AGENCY_KIND_KOMMUNAL = "kommunal";
	static final String AGENCY_KIND_FREI = "frei";



	// ----------------
	// --- Commands ---
	// ----------------


	static final String ITEM_LOAD_COORDINATES = "Load All GPS Coordinates";
	static final String  CMD_START_GEOCODING = "loadGeoCodes";

	static final String ITEM_LOAD_EMPTY_COORDINATES = "Load Empty GPS Coordinates";
	static final String  CMD_START_EMPTY_GEOCODING = "loadEmptyGeoCodes";

	static final String ITEM_LOCK_GEOMETRY = "Lock";
	static final String  CMD_LOCK_GEOMETRY = "lockGeometry";
	//
	static final String ITEM_UNLOCK_GEOMETRY = "Unlock";
	static final String  CMD_UNLOCK_GEOMETRY = "unlockGeometry";
	//
	static final String ITEM_REPOSITION_ALL = "Reposition all";
	static final String  CMD_REPOSITION_ALL = "repositionAll";
	static final String ICON_REPOSITION_ALL = "location.png";
	//
	static final String ITEM_MAKE_SHAPE = "Make Shape";
	static final String  CMD_MAKE_SHAPE = "makeShape";



	// *********************
	// *** Web Constants ***
	// *********************



	// ----------------
	// --- Servlets ---
	// ----------------



	public static final int SERVLET_BROWSE = 1;
	public static final int SERVLET_EDIT = 2;
	public static final int SERVLET_LIST = 3;
	public static final int SERVLET_UPLOAD = 4;
	public static final int SERVLET_IMPORT = 5;
	public static final int SERVLET_MAPS = 6;
	public static final int SERVLET_ATLAS = 7;
	public static final int SERVLET_WORKSPACE = 8;



	// -------------
	// --- Icons ---
	// -------------



	public static final String ICON_HOTSPOT = "redball-middle.gif";
	public static final String ICON_CLUSTER = "redball-bigger.gif";



	// ---------------
	// --- Actions ---
	// ---------------



	// browse servlet
	public static final String ACTION_INIT_FRAME = "initFrame";
	public static final String ACTION_SHOW_CATEGORIES = "showCategories";
	public static final String ACTION_SHOW_INFO_EXTERNAL = "showInfo";
	public static final String ACTION_SHOW_GEO_INFO = "showGeoObjectInfo";
	public static final String ACTION_SEARCH = "search";
	public static final String ACTION_SEARCH_BY_CATEGORY = "searchByCategory";
	public static final String ACTION_SELECT_CATEGORY = "selectCategory";
	public static final String ACTION_SHOW_GEO_FORUM = "showGeoObjectForum";
	public static final String ACTION_SHOW_COMMENT_FORM = "showCommentForm";
	public static final String ACTION_CREATE_COMMENT = "createComment";
	public static final String ACTION_TOGGLE_SHAPE_DISPLAY = "toggleShapeDisplay";
	// edit servlet
	public static final String ACTION_TRY_LOGIN = "tryLogin";				// also used for list servlet
	public static final String ACTION_SHOW_GEO_FORM = "showGeoObjectForm";	// also used for list servlet
	public static final String ACTION_UPDATE_GEO = "updateGeo";				// also used for list servlet
	public static final String ACTION_GO_HOME = "goHome";					// also used for list servlet
	public static final String ACTION_SHOW_FORUM_ADMINISTRATION = "showForumAdmin";
	public static final String ACTION_ACTIVATE_FORUM = "activateForum";
	public static final String ACTION_DEACTIVATE_FORUM = "deactivateForum";
	public static final String ACTION_DELETE_COMMENT = "deleteComment";
	// workspace
	public static final String ACTION_SHOW_WORKSPACE_FORM = "showWorkspaceForm";
	// list servlet
	public static final String ACTION_SHOW_INSTITUTIONS = "showInstitutions";
	public static final String ACTION_SHOW_INSTITUTIONS_SLIM = "showSlimInstitution";
	public static final String ACTION_SHOW_EMPTY_GEO_FORM = "showEmptyGeoObjectForm";
	public static final String ACTION_CREATE_GEO = "createGeo";
	public static final String ACTION_SORT_BY ="sort";
	public static final String ACTION_FILTER ="filter";
	public static final String ACTION_CLEAR_FILTER ="clearFilter";
	public static final String ACTION_CREATE_FORM_LETTER ="createFormLetter";
	public static final String ACTION_FILTER_ROUNDMAILING ="filterRoundMailing";
	public static final String ACTION_FILTER_MAIL_ALL ="filterMailAll";
	public static final String ACTION_CREATE_ROUNDMAILING ="createRoundMailing";
	public static final String ACTION_DELETE_ENTRY ="deleteEntry";
	public static final String ACTION_EXPORT_CITYMAP ="exportCityMap";
	public static final String ACTION_DOWNLOAD_CITYMAP ="downloadCityMap";
	public static final String ACTION_SHOW_LIST_LEGEND ="showListLegend";
	// import servlet
	public static final String ACTION_SHOW_IMPORTS = "showImports";
	public static final String ACTION_SHOW_REPORT = "showImportReport";
	public static final String ACTION_DO_IMPORT = "doImport";
	public static final String ACTION_RESET_CRITERIAS = "resetCritCats";
	// Maps Servlets
	public static final String ACTION_GEO_CODE = "geoCode";
	// Upgrade Servlets
	public static final String ACTION_MIGRATE_CITYMAP = "upgradeCityMap";



	// --------------------
	// --- Search Modes ---
	// --------------------



	static final String SEARCHMODE_BY_NAME = "byName";
	// Note: other search modes are generated dynamically: "0", "1", "2", ....



	// --------------
	// --- Frames ---
	// --------------



	static final String FRAME_LEFT = "left";
	static final String FRAME_RIGHT = "right";



	// -------------
	// --- Pages ---
	// -------------



	// atlas
	static final String PAGE_BERLIN_ATLAS = "be.de/BerlinAtlas";
	static final String PAGE_EHRENAMTS_MAP = "ehrenamt/EhrenamtsMap";
		// browse
	static final String PAGE_FRAMESET = "frameset";
	static final String PAGE_CITY_MAP = "CityMap";
	static final String PAGE_CATEGORY_LIST = "CategoryList";
	static final String PAGE_GEO_LIST = "GeoObjectList";
	static final String PAGE_GEO_INFO = "GeoObjectInfo";
	static final String PAGE_GEO_FORUM = "GeoObjectForum";
	static final String PAGE_COMMENT_FORM = "CommentForm";
	static final String PAGE_MAP_LOGIN = "MapLogin";
	static final String PAGE_MAP_ATLAS = "be.de/AlternativeAtlas";
	// workspace
	static final String PAGE_WORKSPACE_LOGIN = "WorkspaceObjectLogin";
	static final String PAGE_WORKSPACE_FORM = "WorkspaceObjectForm";
	static final String PAGE_WORKSPACE_OBJECT_ADDED = "WorkspaceObjectAdded";
	static final String PAGE_WORKSPACE_ERROR = "error";
	// edit
	static final String PAGE_GEO_LOGIN = "GeoObjectLogin";
	static final String PAGE_GEO_HOME = "GeoObjectHome";
	static final String PAGE_GEO_FORM = "GeoObjectForm";
	static final String PAGE_FORUM_ADMINISTRATION = "ForumAdministration";
	// list
	static final String PAGE_LIST_LOGIN = "ListLogin";
	static final String PAGE_LIST_HOME = "ListHome";
	static final String PAGE_LIST = "List";
	static final String PAGE_SLIM_LIST = "SlimList";
	static final String PAGE_GEO_ADMIN_FORM = "GeoObjectAdminForm";
	static final String PAGE_GEO_EMPTY_FORM = "GeoObjectEmptyForm";
	static final String PAGE_LINK_PAGE = "Print"; // Link Page
	static final String PAGE_DOWNLOAD_PAGE = "Download"; // Link Page
	static final String PAGE_LIST_INFO = "ListHelp";
	static final String PAGE_LIST_MAILING = "ListMailing";
	// import
	static final String PAGE_IMPORTS_LOGIN = "ImportsLogin";
	static final String PAGE_IMPORTS_HOME = "ImportsHome";
	static final String PAGE_REPORT_HOME = "ReportInfo";
	// upgrade
	static final String PAGE_UPGRADE_LOGIN = "upgrade/UpgradeLogin";
	static final String PAGE_UPGRADE_LIST = "upgrade/CityMapList";
	static final String PAGE_UPGRADE_CITYMAP = "upgrade/CityMap";
	// error
	static final String PAGE_ERROR = "error";
	// service
	static final String PAGE_SERVE = "Serve";
}
