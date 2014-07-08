package de.kiezatlas.deepamehta;

import java.io.Serializable;
import java.awt.Point;
import java.awt.Dimension;



/**
 * A bean-like data container for passing data from the front-controller (servlet)
 * to the presentation layer (JSP engine).
 * <p>
 * Kiezatlas 1.4<br>
 * Requires DeepaMehta 2.0b7-post1
 * <p>
 * Last change: 4.3.2007<br>
 * J&ouml;rg Richter<br>
 * jri@freenet.de
 */
public class Shape implements Serializable {
	
	public String url;
	public Point point;
	public Dimension size;
	public String targetWebalias;

	Shape(String url, Point point, Dimension size, String targetWebalias) {
		this.url = url;
		this.point = point;
		this.size = size;
		this.targetWebalias = targetWebalias;
	}
}
