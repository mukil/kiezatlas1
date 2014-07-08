package de.kiezatlas.deepamehta.topics;

import de.kiezatlas.deepamehta.KiezAtlas;
//
import de.deepamehta.BaseTopic;
import de.deepamehta.BaseAssociation;
import de.deepamehta.Commands;
import de.deepamehta.DeepaMehtaException;
import de.deepamehta.PresentableTopic;
import de.deepamehta.PropertyDefinition;
import de.deepamehta.service.ApplicationService;
import de.deepamehta.service.CorporateDirectives;
import de.deepamehta.service.CorporateCommands;
import de.deepamehta.service.DeepaMehtaServiceUtils;
import de.deepamehta.service.Session;
import de.deepamehta.topics.LiveTopic;
import de.deepamehta.util.DeepaMehtaUtils;
//
import java.io.File;
import javax.swing.ImageIcon;
import java.awt.image.BufferedImage;
import java.awt.geom.Rectangle2D;
import java.awt.*;
import java.util.*;



/**
 * Kiezatlas 1.4.1.<br>
 * Requires DeepaMehta 2.0b8.
 * <p>
 * Last change: 14.9.2008<br>
 * J&ouml;rg Richter<br>
 * jri@freenet.de
 */
public class OutlinePointTopic extends LiveTopic implements KiezAtlas {



	// *******************
	// *** Constructor ***
	// *******************



	public OutlinePointTopic(BaseTopic topic, ApplicationService as) {
		super(topic, as);
	}



	// **********************
	// *** Defining Hooks ***
	// **********************



	// --------------------------
	// --- Providing Commands ---
	// --------------------------



	public CorporateCommands contextCommands(String topicmapID, String viewmode,
								Session session, CorporateDirectives directives) {
		CorporateCommands commands = new CorporateCommands(as);
		int editorContext = as.editorContext(topicmapID);
		//
		commands.addNavigationCommands(this, editorContext, session);
		commands.addSeparator();
		//
		// --- "Make Shape" ---
		Commands shapeTypeGroup = commands.addCommandGroup(ITEM_MAKE_SHAPE /*, FILESERVER_ICONS_PATH, "color.gif" */);
		BaseTopic workspace = as.getOriginWorkspace(topicmapID);	// is null if never published
		if (workspace != null) {
			Vector typeIDs = as.type(TOPICTYPE_SHAPE, 1).getSubtypeIDs();	// ### use central business logic implemented in CityMapTopic.getShapeTypes()
			Vector shapeTypes = cm.getRelatedTopics(workspace.getID(), SEMANTIC_WORKSPACE_SHAPETYPE, TOPICTYPE_TOPICTYPE, 2, typeIDs, true);	// sortAssociations=true
			Vector disabledShapeTypes = getDisabledShapeTypes(shapeTypes);
			commands.addTopicCommands(shapeTypeGroup, shapeTypes, CMD_MAKE_SHAPE, COMMAND_STATE_DEFAULT,
				null, disabledShapeTypes, null, session, directives);	// selectedTopicIDs=null, title=null
		}
		//
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
		//
		if (cmd.equals(CMD_MAKE_SHAPE)) {
			String shapeTypeID = st.nextToken();
			makeShape(shapeTypeID, topicmapID, session, directives);
		} else {
			return super.executeCommand(command, session, topicmapID, viewmode);
		}
		return directives;
	}



	// -----------------------------
	// --- Handling Associations ---
	// -----------------------------



	public String associationAllowed(String assocTypeID, String relTopicID, int relTopicPos, CorporateDirectives directives) {
		return SEMANTIC_SHAPE_OUTLINE;
	}



	// **********************
	// *** Custom Methods ***
	// **********************



	private void makeShape(String shapeTypeID, String topicmapID, Session session, CorporateDirectives directives) {
		try {
			Polygon polygon = createPolygon(topicmapID);	// throws DME
			Rectangle bounds = polygon.getBounds();
			System.out.println(">>> OutlinePointTopic.makeShape(): polygon bounds= " + bounds);
			// --- create topic ---
			String topicID = as.getNewTopicID();
			int x = bounds.x + bounds.width / 2;
			int y = bounds.y + bounds.height / 2;
			directives.add(as.createTopic(topicID, shapeTypeID, x, y, topicmapID, session));
			directives.add(DIRECTIVE_SELECT_TOPIC, topicID);
			directives.add(DIRECTIVE_FOCUS_PROPERTY);
			// set icon property
			String shapeFileName = "shape-" + topicID + ".png";
			as.setTopicProperty(topicID, 1, PROPERTY_ICON, shapeFileName);
			as.setTopicProperty(topicID, 1, PROPERTY_LOCKED_GEOMETRY, SWITCH_ON);
			// --- create icon dynamically ---
			BufferedImage bufferedImage = new BufferedImage(bounds.width, bounds.height, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = bufferedImage.createGraphics();
			String colorCode = getProperty(shapeTypeID, 1, PROPERTY_COLOR);
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setColor(DeepaMehtaUtils.parseHexColor(colorCode, SHAPE_ALPHA));	// throws DME
			g.translate(-bounds.x, -bounds.y);
			g.fill(polygon);
			// save icon to a file
			File file = new File(FILESERVER_ICONS_PATH + shapeFileName);
			DeepaMehtaServiceUtils.createImageFile(bufferedImage, file);
		} catch (DeepaMehtaException e) {
			System.out.println("*** OutlinePointTopic.makeShape(): " + e);
			directives.add(DIRECTIVE_SHOW_MESSAGE, "Fehler beim Erzeugen einer Fläche: " + e.getMessage(),
				new Integer(NOTIFICATION_WARNING));
		}
	}

	/**
	 * @return	the polygon this outline point is a part of
	 */
	private Polygon createPolygon(String topicmapID) throws DeepaMehtaException {
		Polygon polygon = new Polygon();
		String currentPoint = getID();
		String behindPoint = null;
		//
		do {
			Vector points = cm.getRelatedViewTopics(topicmapID, 1, currentPoint, SEMANTIC_SHAPE_OUTLINE,
																					TOPICTYPE_OUTLINE_POINT);
			// an outline point must have 2 neighboured outline points
			if (points.size() < 2) {
				throw new DeepaMehtaException("der Umriss ist nicht geschlossen");
			} else if (points.size() > 2) {
				throw new DeepaMehtaException("der Umriss ist nicht eindeutig");
			}
			// decide which point to add
			int i;
			if (behindPoint == null) {
				// traversal starts. Direction is arbitraty.
				i = 0;
			} else {
				if (((PresentableTopic) points.get(0)).getID().equals(behindPoint)) {
					i = 1;
				} else if (((PresentableTopic) points.get(1)).getID().equals(behindPoint)) {
					i = 0;
				} else {
					throw new DeepaMehtaException("internal error while createPolygon()");
				}
			}
			// add point to polygon
			PresentableTopic point = (PresentableTopic) points.get(i);
			Point p = point.getGeometry();
			polygon.addPoint(p.x, p.y);
			// proceed to next point
			behindPoint = currentPoint;
			currentPoint = point.getID();
			//
		} while (!currentPoint.equals(getID()));
		//
		System.out.println("  > polygon has " + polygon.npoints + " vertex points");
		return polygon;
	}

	private Vector getDisabledShapeTypes(Vector shapeTypes) {
		Vector disabledShapeTypes = new Vector();
		Enumeration e = shapeTypes.elements();
		while (e.hasMoreElements()) {
			BaseTopic shapeType = (BaseTopic) e.nextElement();
			if (as.getTopicProperty(shapeType, PROPERTY_COLOR).equals("")) {
				disabledShapeTypes.addElement(shapeType.getID());
			}
		}
		return disabledShapeTypes;
	}
}
