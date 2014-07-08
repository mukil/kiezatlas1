<%@page import="java.net.URLEncoder"%>
<%@page import="java.net.URL"%>
<%@ page import="de.kiezatlas.deepamehta.KiezAtlas" %>
<%@ page import="de.kiezatlas.deepamehta.GeoObject" %>
<%@ page import="de.kiezatlas.deepamehta.topics.GeoObjectTopic" %>
<%@ page import="de.kiezatlas.deepamehta.SearchCriteria" %>
<%@ page import="de.kiezatlas.deepamehta.ShapeType" %>
<%@ page import="de.kiezatlas.deepamehta.Shape" %>
<%@ page import="de.kiezatlas.deepamehta.Comment" %>
<%@ page import="de.kiezatlas.deepamehta.Cluster" %>

<%@ page import="de.deepamehta.BaseTopic" %>
<%@ page import="de.deepamehta.DeepaMehtaConstants" %>
<%@ page import="de.deepamehta.service.TopicBean" %>
<%@ page import="de.deepamehta.service.TopicBean" %>
<%@ page import="de.deepamehta.service.TopicBeanField" %>
<%@ page import="de.deepamehta.BaseAssociation" %>
<%@ page import="de.deepamehta.DeepaMehtaException" %>
<%@ page import="de.deepamehta.PresentableTopic" %>
<%@ page import="de.deepamehta.PropertyDefinition" %>
<%@ page import="de.deepamehta.service.Session" %>
<%@ page import="de.deepamehta.topics.TypeTopic" %>
<%@ page import="de.deepamehta.service.web.HTMLGenerator" %>

<%@ page import="java.io.IOException" %>
<%@ page import="java.util.Vector" %>
<%@ page import="java.util.Hashtable" %>
<%@ page import="java.util.Enumeration" %>
<%@ page import="java.awt.Point" %>
<%@ page import="java.lang.Thread.State" %>

<%!
	// --- header area ---

	// edit / list / upload / maps-login
	void begin(int servlet, HttpSession session, JspWriter out) throws IOException {
		String title = "Kiezatlas";
		switch (servlet) {
		case KiezAtlas.SERVLET_EDIT:
			BaseTopic geo = (BaseTopic) session.getAttribute("geo");
			if (geo != null) {
        title = title + " - " + geo.getName();
      } else {
        title = title + " - Neuer Eintrag";
      }
			break;
		case KiezAtlas.SERVLET_LIST:
			title = title + " - Listenzugang";
			break;
        case KiezAtlas.SERVLET_WORKSPACE:
			title = title + " - Workspacezugang";
			break;
		case KiezAtlas.SERVLET_MAPS:
			title = title + " - Stadtplanzugang";
			break;
        case KiezAtlas.SERVLET_IMPORT:
			title = title + " - Importzugang";
			break;
		}
		out.println("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\">\r" +
			"<html>" +
			"\r<head>" +
			"<meta http-equiv=\"content-type\" content=\"text/html; charset=iso-8859-1\">" +
			"\r<title>" + title + "</title>");
		out.println("<link href=\"../pages/kiezatlas.css\" rel=\"stylesheet\" type=\"text/css\">");
		out.println("<script src=\"../pages/util.js\" type=\"text/javascript\"></script>");
		out.println("</head>");
		out.println("<body>");
		out.println();
		out.println("<div class=\"header-area\">"); 		// --- begin header area
		out.println("<a href=\"http://www.kiezatlas.de/\" target=\"_blank\"><img src=\"../images/kiezatlas-logo.png\" border=\"0\"></a>");
		out.println("</div>");								// --- end header area
		out.println();
		out.println("<div class=\"content-area\">");		// --- begin content area
	}

	// browse
	void begin(HttpSession session, JspWriter out, boolean refreshMap) throws IOException {
		BaseTopic map = (BaseTopic) session.getAttribute("map");
		SearchCriteria[] criterias = (SearchCriteria[]) session.getAttribute("criterias");
		String searchMode = (String) session.getAttribute("searchMode");
		String searchValue = (String) session.getAttribute("searchValue");
		String stylesheet = (String) session.getAttribute("stylesheet");
		String siteLogo = (String) session.getAttribute("siteLogo");
		String homepageURL = (String) session.getAttribute("homepageURL");
		out.println("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\">\r" +
			"<html>" +
			"\r<head>\n" +
			"<meta http-equiv=\"content-type\" content=\"text/html; charset=iso-8859-1\">" +
			"\r<title>Kiezatlas</title>" +
			"\r<style type=\"text/css\">\r" + stylesheet + "\r</style>\r" +
            "\r<script src=\"../pages/jQuery-min.1.4.2.js\" type=\"text/javascript\"></script>\r" +
            "\r<script src=\"../pages/util.js\" type=\"text/javascript\"></script>\r" +
			"</head>\r" +
			"<body" + (refreshMap ? " onLoad=\"top.frames.left.location.href='controller?action=initFrame&frame=" +
				KiezAtlas.FRAME_LEFT + "'\"" : "") + ">\r\r");
		//
		out.println("<div class=\"header-area\">");			// --- begin header area
		out.println("<table cellpadding=\"0\" width=\"100%\"><tr valign=\"top\">");
		out.println("<td rowspan=\"" + (criterias.length + 1) + "\">");
		out.println("<a href=\"" + homepageURL + "\" target=\"_blank\"><img src=\"" + siteLogo + "\" border=\"0\"></a>");
		out.println("<div class=\"citymap-name\">" + map.getName() + "</div>");
		out.println("</td>");
		//
		boolean byName = searchMode.equals(KiezAtlas.SEARCHMODE_BY_NAME);
		out.println("<td>" + (byName ? "&rarr;" : "") + "</td><td>" +
			"<form>Suchen<br><input type=\"hidden\" name=\"action\" value=\"" + KiezAtlas.ACTION_SEARCH + "\">" +
			"<input type=\"text\" name=\"search\"" + (byName && searchValue != null ? " value=\"" + searchValue + "\"" : "") +
			" size=\"11\"></form></td></tr>");
		for (int i = 0; i < criterias.length; i++) {
			String critName = criterias[i].criteria.getPluralName();
			out.println("<tr valign=\"top\"><td>" + (searchMode.equals(Integer.toString(i)) ? "&rarr;" : "") + "</td><td>" +
				"<a href=\"controller?action=" + KiezAtlas.ACTION_SHOW_CATEGORIES + "&critNr=" + i + "\">" + critName + "</a></td></tr>");
		}
		out.println("</table>");
    // introduce breadcrumb and link to new maps-interface
    out.println("" +
      "<div id=\"navigation-helper\" class=\"secondary-text\" style=\"border-top: 1px dashed #fff; margin-top:3px; " +
      "padding-left: 2px; padding-bottom: 0px; padding-top:3px;\">" +
        "<a href=\"http://www.kiezatlas.de/map/"+session.getAttribute("webAlias")+"\" " +
         "title=\"Zur interaktiven Kartenansicht wechseln\" target=\"_blank\">weitere Ansichten</a>" +
      "</div>");
		out.println("</div>");								// --- end header area
		out.println();
		out.println("<div class=\"content-area\">");		// --- begin content area
	}

	// atlas / maps
	void startMaps(HttpSession session, JspWriter out) throws IOException {
    String header = "<!-- This new comment shall put IE 6, 7 and 8 in quirks mode -->\r" +
      "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\r" +
      "<html xmlns=\"http://www.w3.org/1999/xhtml\">\r";
      out.println(header);
	}

	// --- footer area ---

	// edit / list / upload
	void end(JspWriter out) throws IOException {
		String impressumURL = "http://www.kiezatlas.de/impressum.html";
		out.println(footerArea(impressumURL));
	}

	// browse
	void end(HttpSession session, JspWriter out) throws IOException {
		String impressumURL = (String) session.getAttribute("impressumURL");
		out.println(footerArea(impressumURL));
	}

	private String footerArea(String impressumURL) {
		return "</div>\r" +									// --- end content area
			"\r" +
			"<div class=\"footer-area\">\r" +				// --- begin footer area
			"<table width=\"100%\"><tr>\r" +
			"<td class=\"secondary-text\"><a href=\"http://www.kiezatlas.de\" target=\"_blank\"><b>Kiezatlas</b></a> is powered by<br>" +
      "<a href=\"http://www.deepamehta.de/\" target=\"_blank\"><b>DeepaMehta</b></a></td>\r" +
			"<td class=\"secondary-text\" align=\"right\"><a href=\"" + impressumURL + "\" target=\"_blank\">Impressum +<br>" +
      "Haftungshinweise</a></td>\r" +
			"</tr></table>\r" +
			"</div>\r\r" +									// --- begin footer area
			"</body>\r</html>\r";
	}

	// ---

	void topicImages(Vector cats, HTMLGenerator html, JspWriter out) throws IOException {
		for (int i = 0; i < cats.size(); i++) {
			BaseTopic cat = (BaseTopic) cats.elementAt(i);
			out.println(html.imageTag(cat, true));		// withTooltip=true
		}
	}

	void topicList(Vector topics, String action, HTMLGenerator html, JspWriter out) throws IOException {
		out.println("<table>");
		Enumeration e = topics.elements();
		while (e.hasMoreElements()) {
			BaseTopic topic = (BaseTopic) e.nextElement();
			out.println("<tr><td>" + html.imageTag(topic) + "</td><td><a href=\"controller?action=" + action +
				"&id=" + topic.getID() + "\">" + topic.getName() + "</td></tr>");
		}
		out.println("</table>");
	}

	// ---

	void comment(Comment comment, JspWriter out) throws IOException {
		comment(comment, false, out);
	}

	void comment(Comment comment, boolean includeEmailAddress, JspWriter out) throws IOException {
		String email = comment.email;
		out.println("<br>");
		//
		out.println("<span class=\"small\">");
		if (isSet(comment.author)) {
			out.println(comment.author + commentEmail(includeEmailAddress, email) + " schrieb am ");
		} else {
			out.println("Anonymer Kommentar" + commentEmail(includeEmailAddress, email) + " vom ");
		}
		out.println(comment.date + ":</span><br>");
		//
		out.println(comment.text + "<br>");
	}

	// ---

	private String commentEmail(boolean includeEmailAddress, String email) {
		if (includeEmailAddress) {
			return isSet(email) ? " (<a href=\"mailto:?bcc=" + email + "\">" + email + "</a>)" : " (Emailadresse unbekannt)";
		} else {
			return "";
		}
	}

	// ---
//
	String mapLink(String street, String postalCode, String city) throws IOException {
		// ### System.out.println(">>> mapLink(): street=\"" + street + "\" postalCode=\"" + postalCode + "\" city=\"" + city + "\"");
		StringBuffer html = new StringBuffer();
		// render fahr-info link if address is in berlin
		if (city.startsWith("Berlin") && isSet(street)) {
      String target = URLEncoder.encode(street + " " + postalCode + " " + city, "ISO-8859-1");
			// String mapURL = "http://www.fahrinfo-berlin.de/Stadtplan/index?query=" + target + "&search=Suchen&formquery=&address=true";
      String mapURL = "http://www.fahrinfo-berlin.de/fahrinfo/bin/query.exe/d?Z=" + target + "&REQ0JourneyStopsZA1=2&start=1";
			String imageLink = " <a href=\"" + mapURL + "\" target=\"_blank\"><img src=\"../images/fahrinfo.gif\" border=\"0\" " +
				"hspace=\"20\"></a>";
			html.append(street + imageLink + "<br>" + postalCode + " " + city + googleLink(street, postalCode, city));
			return html.toString();
		} else {
			html.append(isSet(street) ? street + "<br>" : "");
			html.append(isSet(postalCode) ? postalCode + " " : "");
			html.append(isSet(city) ? city + googleLink(street, postalCode, city) : "");
			return html.toString();
		}
	}

	String googleLink(String street, String postalCode, String city) throws IOException {
		// ### System.out.println(">>> googleLink(): street=\"" + street + "\" postalCode=\"" + postalCode + "\" city=\"" + city + "\"");
		StringBuffer html = new StringBuffer();
		// render googlelink if address is complete
		if (isSet(city) && isSet(street) && isSet(postalCode)) {
			String mapURL = "http://maps.google.de/maps?q=" + street + ", " + postalCode + " " + city + "&mrt=loc&lci=lmc:panoramio,lmc:wikipedia_en&layer=tc&t=h";
			String imageLink = " <a href=\"" + mapURL + "\" target=\"_blank\"><img src=\"../images/google_logo_small.png\" alt=\"Ansicht in Google \" hspace=\"20\" border=\"0\"></a>";
			html.append(imageLink);
			return html.toString();
		} else {
		    return "";
		}
	}

	String mailtoUrl(Vector mailboxes) {
		Enumeration e = mailboxes.elements();
		StringBuffer url = new StringBuffer();
		url.append("mailto:?bcc=");
		//
		while(e.hasMoreElements()) {
			String mail = (String) e.nextElement();
			if(mail != null && !mail.equals("")) {
			    url.append(mail);
			}
			if (e.hasMoreElements()) {
			    url.append(",");
			}
		}
		//
		return url.toString();
	}

	// --

	String fieldOptions(TopicBean bean, String[] hiddenProps, String[] hiddenPropsContaining, String checked) {
		StringBuffer html = new StringBuffer();
		for (int j = 0; j < hiddenProps.length; j++) {
		    bean.removeField(hiddenProps[j].toString());
		}
		for (int k = 0; k < hiddenPropsContaining.length; k++) {
		    bean.removeFieldsContaining(hiddenPropsContaining[k].toString());
		}
		for (int i = 0; i < bean.fields.size(); i++) {
		    TopicBeanField field = (TopicBeanField) bean.fields.get(i);
		    html.append("<option value=\""+ field.name +"\"");
		    if (field.name.equals(checked)) {
                html.append(" selected=\"selected\"");
		    }
		    html.append(">" + field.label + "</option> \n ");
		}
		return html.toString();
	}

	/** basetopics given for names and id*/

	String fieldOptionalIds(Vector names, String checked) {
		StringBuffer html = new StringBuffer();
		/** for (int j = 0; j < hiddenProps.length; j++) {
		    bean.removeField(hiddenProps[j].toString());
		}
		for (int k = 0; k < hiddenPropsContaining.length; k++) {
		    bean.removeFieldsContaining(hiddenPropsContaining[k].toString());
		}  */
		for (int i = 0; i < names.size(); i++) {
		    //TopicBeanField field = (TopicBeanField) names.get(i);
            BaseTopic name = (BaseTopic) names.get(i);
		    html.append("<option value=\""+ name.getID() +"\"");
		    if (name.getName().equals(checked)) {
                html.append(" selected=\"selected\"");
		    }
		    html.append(">" + name.getName() + "</option> \n ");
		}
		return html.toString();
	}

	// ---

	boolean isSet(String str) {
		return str != null && str.length() > 0;
	}
%>
