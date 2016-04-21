package de.kiezatlas.deepamehta;

import de.kiezatlas.deepamehta.topics.GeoObjectTopic;
//
import de.deepamehta.BaseTopic;
import de.deepamehta.DeepaMehtaException;
import de.deepamehta.service.ApplicationService;
import de.deepamehta.service.CorporateDirectives;
import de.deepamehta.service.Session;
import de.deepamehta.service.TopicBean;
import de.deepamehta.service.web.DeepaMehtaServlet;
import de.deepamehta.service.web.RequestParameter;
import de.deepamehta.service.web.WebSession;
import de.deepamehta.util.DeepaMehtaUtils;
//
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
//
import java.io.File;
import java.util.*;
//
import org.apache.commons.fileupload.FileItem;



/**
 * Kiezatlas 1.6.8.3<br>
 * Requires DeepaMehta 2.0b8.
 * <p>
 * Last change: 02.08.2009<br>
 * J&ouml;rg Richter<br> / Malte Rei&szlig;
 * jri@deepamehta.de / mre@deepamehta.de
 */
public class EditServlet extends DeepaMehtaServlet implements KiezAtlas {

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
				setGeoObject(GeoObjectTopic.lookupInstitution(alias, as), session);
				return PAGE_GEO_LOGIN;
			} catch (DeepaMehtaException e) {
				System.out.println("*** EditServlet.performAction(): " + e);
				session.setAttribute("error", e.getMessage());
				return PAGE_ERROR;
			}
		}
		// session timeout?
		if (getGeoObject(session) == null) {	// ### doesn't return null but throws exception!
			System.out.println("*** Session Expired ***");
			session.setAttribute("error", "Timeout: Kiezatlas wurde mehr als " +
				((WebSession) session).session.getMaxInactiveInterval() / 60 + " Minuten nicht benutzt");
			return PAGE_ERROR;
		}
		//
		if (action.equals(ACTION_TRY_LOGIN)) {
			GeoObjectTopic geo = getGeoObject(session);
			String password = params.getValue(PROPERTY_PASSWORD);
			String geoPw = as.getTopicProperty(geo, PROPERTY_PASSWORD);
			return password.equals(geoPw) ? PAGE_GEO_HOME : PAGE_GEO_LOGIN;
			//
		} else if (action.equals(ACTION_SHOW_GEO_FORM)) {
			return PAGE_GEO_FORM;
			//
		} else if (action.equals(ACTION_UPDATE_GEO)) {
			GeoObjectTopic geo = getGeoObject(session);
			// --- update geo object ---
			// Note: the timestamp is updated through geo object's propertiesChanged() hook
			updateTopic(geo.getType(), params, session, directives);
			// --- store image ---
			writeFiles(params.getUploads(), geo.getImage(), as);
			if (geo.isPartOfMigratedWorkspaces()) {
				// --- update mirror-topic ---
				geo.synchronizeGeoObject();
			}
			// --- update GPS coordinates ---
			geo.setGPSCoordinates(directives); // will load new geo-coordinates IF values are set to " "
			return PAGE_GEO_HOME;
			//
		} else if (action.equals(ACTION_SHOW_FORUM_ADMINISTRATION)) {
			return PAGE_FORUM_ADMINISTRATION;
			//
		} else if (action.equals(ACTION_ACTIVATE_FORUM)) {
			GeoObjectTopic geo = getGeoObject(session);
			// create forum topic if not yet exist
			BaseTopic forum = geo.getForum();
			String forumID;
			if (forum == null) {
				forumID = as.getNewTopicID();
				cm.createTopic(forumID, 1, TOPICTYPE_FORUM, 1, "");
				String assocID = as.getNewAssociationID();
				cm.createAssociation(assocID, 1, SEMANTIC_INSTITUTION_FORUM, 1, geo.getID(), 1, forumID, 1);
			} else {
				forumID = forum.getID();
			}
			// activate forum
			cm.setTopicData(forumID, 1, PROPERTY_FORUM_ACTIVITION, SWITCH_ON);
			return PAGE_FORUM_ADMINISTRATION;
			//
		} else if (action.equals(ACTION_DEACTIVATE_FORUM)) {
			// deactivate forum
			BaseTopic forum = getGeoObject(session).getForum();
			cm.setTopicData(forum.getID(), 1, PROPERTY_FORUM_ACTIVITION, SWITCH_OFF);
			return PAGE_FORUM_ADMINISTRATION;
			//
		} else if (action.equals(ACTION_DELETE_COMMENT)) {
			String commentID = params.getValue("commentID");
			deleteTopic(commentID);
			return PAGE_FORUM_ADMINISTRATION;
			//
		} else if (action.equals(ACTION_GO_HOME)) {
			return PAGE_GEO_HOME;
			//
		} else {
			return super.performAction(action, params, session, directives);
		}
	}

	protected void preparePage(String page, RequestParameter params, Session session, CorporateDirectives directives) {
		if (page.equals(PAGE_GEO_HOME)) {
			String geoID = getGeoObject(session).getID();
			TopicBean topicBean = as.createTopicBean(geoID, 1);
			session.setAttribute("topicBean", topicBean);
			updateImagefile(session);
		} else if (page.equals(PAGE_FORUM_ADMINISTRATION)) {
			GeoObjectTopic geo = getGeoObject(session);
			boolean isForumActivated = geo.isForumActivated();
			session.setAttribute("activition", isForumActivated ? SWITCH_ON : SWITCH_OFF);
			if (isForumActivated) {
				session.setAttribute("comments", geo.getCommentBeans());
			}
		}
	}



	// *****************
	// *** Utilities ***
	// *****************



	/**
	 * @see		#performAction
	 * @see		ListServlet#performAction
	 */
	static Vector writeFiles(Vector fileItems, BaseTopic topic, ApplicationService as) {
		Vector fileNames = new Vector();
		for (int i = 0; i < fileItems.size(); i++) {
			try {
				System.out.println(">>> EditServlet.writeFiles(): " + fileItems.size() + " files uploaded, writing fileItem: " + i);
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
				System.out.println("*** EditServlet.writeFiles(): " + e);
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



	private void setGeoObject(BaseTopic geo, Session session) {
		session.setAttribute("geo", geo);
		System.out.println("> \"geo\" stored in session: " + geo);
	}

	private GeoObjectTopic getGeoObject(Session session) {
		return (GeoObjectTopic) as.getLiveTopic((BaseTopic) session.getAttribute("geo"));
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
                if (imagefile.indexOf("png") != -1 || imagefile.indexOf("jpg") != -1 || imagefile.indexOf("gif") != -1 ||
                        imagefile.indexOf("PNG") != -1 || imagefile.indexOf("JPG") != -1 || imagefile.indexOf("GIF") != -1) {
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
