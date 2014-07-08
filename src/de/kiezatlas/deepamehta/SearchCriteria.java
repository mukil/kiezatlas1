package de.kiezatlas.deepamehta;

import de.deepamehta.topics.TypeTopic;
//
import java.io.Serializable;
import java.util.Vector;



/**
 * A bean-like data container for passing data from the front-controller (servlet)
 * to the presentation layer (JSP engine).
 * <p>
 * Kiezatlas 1.4.1.<br>
 * Requires DeepaMehta 2.0b7-post1.
 * <p>
 * Last functional change: 16.3.2007<br>
 * J&ouml;rg Richter<br>
 * jri@freenet.de
 */
public class SearchCriteria implements Serializable {
	
	public TypeTopic criteria;
	public Vector selectedCategoryIDs;

	public SearchCriteria(TypeTopic criteria, Vector selectedCategoryIDs) {
		this.criteria = criteria;
		this.selectedCategoryIDs = selectedCategoryIDs;
	}
}
