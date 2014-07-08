package de.kiezatlas.deepamehta;

import de.deepamehta.service.ApplicationService;
import de.deepamehta.util.DeepaMehtaUtils;
//
import java.io.Serializable;



/**
 * A bean-like data container for passing data from the front-controller (servlet)
 * to the presentation layer (JSP engine).
 * <p>
 * Kiezatlas 1.4<br>
 * Requires DeepaMehta 2.0b7-post1
 * <p>
 * Last change: 9.10.2007<br>
 * J&ouml;rg Richter<br>
 * jri@freenet.de
 */
public class Comment implements KiezAtlas, Serializable {

	public String id, text, author, email, date, time;

	public Comment(String commentID, ApplicationService as) {
		id = commentID;
		text = as.getTopicProperty(commentID, 1, PROPERTY_TEXT);
		text = DeepaMehtaUtils.replaceLF(text);		// needed for "Multiline Input Field"
		author = as.getTopicProperty(commentID, 1, PROPERTY_COMMENT_AUTHOR);
		email = as.getTopicProperty(commentID, 1, PROPERTY_EMAIL_ADDRESS);
		date = as.getTopicProperty(commentID, 1, PROPERTY_COMMENT_DATE);
		time = as.getTopicProperty(commentID, 1, PROPERTY_COMMENT_TIME);
	}
}
