package de.kiezatlas.deepamehta;

import de.deepamehta.PresentableTopic;
//
import java.awt.Point;
import java.io.Serializable;
import java.util.Vector;



public class Cluster implements KiezAtlas, Serializable {

	private Point p;
	private String icon;
	private Vector presentables = new Vector();

	public Cluster(PresentableTopic presentableOne, PresentableTopic presentableTwo, String clusterIconPath) {		
		p = presentableOne.getGeometry();
		icon = clusterIconPath + ICON_CLUSTER;
		presentables.add(presentableOne);
		presentables.add(presentableTwo);
	}

	public String toString() {
		return ":: " + presentables.toString() + " :: ";
	}

	public void addPresentable(PresentableTopic presentable) {
		if (!presentables.contains(presentable)) {
			presentables.add(presentable);
		}
	}

	public String getIcon() {
		return icon;
	}

	public Vector getPresentables() {
		return presentables;
	}

	public Point getPoint() {
		return p;
	}
}
