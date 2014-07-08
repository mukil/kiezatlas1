package de.kiezatlas.deepamehta;

import java.io.Serializable;
import java.util.Vector;
//
import de.deepamehta.service.ApplicationService;
import de.kiezatlas.deepamehta.topics.GeoObjectTopic;
import de.kiezatlas.deepamehta.KiezAtlas;



/**
 * A bean-like data container for passing data from the front-controller (servlet)
 * to the presentation layer (JSP engine).
 * <p>
 * Kiezatlas 1.5<br>
 * Requires DeepaMehta 2.0b7-post1.
 * <p>
 * Last change: 15.10.2007<br>
 * Malte Rei&szlig;ig<br>
 * mre@deepamehta.de
 */
public class GeoObject implements KiezAtlas, Serializable {

	public String geoID, name, webAlias;
	public String yadeX, yadeY;
	public Vector categories;

	GeoObject(String newID, SearchCriteria[] criterias, ApplicationService as) {
		GeoObjectTopic geo = (GeoObjectTopic) as.getLiveTopic(newID, 1);
		this.geoID = geo.getID();
		this.name = geo.getName();
		this.webAlias = as.getTopicProperty(geo, PROPERTY_WEB_ALIAS);
		this.yadeX = as.getTopicProperty(geo, PROPERTY_YADE_X);
		this.yadeY = as.getTopicProperty(geo, PROPERTY_YADE_Y);
		this.categories = geo.getCategories(KiezAtlas.TOPICTYPE_KIEZ_GEO);
	}

	public String toString() {
		return "\"" + name + "\"";
	}
}
