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
import javax.servlet.http.HttpServletRequest;
import javax.swing.ImageIcon;

import de.deepamehta.BaseTopic;
import de.deepamehta.DeepaMehtaException;
import de.deepamehta.PresentableTopic;
import de.deepamehta.service.CorporateDirectives;
import de.deepamehta.service.Session;
import de.deepamehta.service.TopicBean;
import de.deepamehta.service.web.DeepaMehtaServlet;
import de.deepamehta.service.web.RequestParameter;
import de.deepamehta.service.web.WebSession;
import de.deepamehta.topics.TypeTopic;

import de.kiezatlas.deepamehta.topics.CityMapTopic;
import de.kiezatlas.deepamehta.topics.GeoObjectTopic;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.Date;



/**
 * Kiezatlas 1.7<br>
 * Requires DeepaMehta 2.0b8-rev388
 * <p>
 * Last change: 09.04.2016<br>
 * Malte Rei&szlig;ig <br>
 * <mre@deepamehta.de>
 */
public class MobileServlet extends DeepaMehtaServlet implements KiezAtlas {

	protected String performAction(String action, RequestParameter params, Session session,
						CorporateDirectives directives) throws ServletException {
		if (action == null) {
			try {
				String pathInfo = params.getPathInfo();
				// error check
				if (pathInfo == null || pathInfo.length() == 1) {
					System.out.println("Routing Mobile Web App: Index");
					return PAGE_MOBILE_INDEX;
				}
				String[] elements = pathInfo.split("/");
				String alias = pathInfo.substring(1);
				System.out.println("Routing Mobile Web App: " +  pathInfo + " Alias: " + alias);
				if (pathInfo.contains("&")) {
					alias = pathInfo.substring(1, pathInfo.indexOf("&"));
				} else if (pathInfo.indexOf("/", 1) != -1) {
					// if / behind mapname... slice map-alias out
					alias = elements[1];
				}
				if (alias.equals("ehrenamt")) {
					if (elements.length > 2) {
						String subpath = elements[2];
						if (subpath.equals("list")) {
							return PAGE_EHRENAMTS_APP_LIST;
						} else if (subpath.equals("info")) {
							return PAGE_EHRENAMTS_APP_INFO;
						}
					}
					return PAGE_EHRENAMTS_APP;
				}
			} catch (DeepaMehtaException e) {
				System.out.println("*** MobileServlet.performAction(): " + e + ", Routing Mobile Web App Index");
				return PAGE_MOBILE_INDEX;
			}
		}
		return PAGE_MOBILE_INDEX;
	}

	protected void preparePage(String page, RequestParameter params, Session session,
						CorporateDirectives directives) {
		System.out.println("Preparing Mobile Page: " +  page);
	}
}
