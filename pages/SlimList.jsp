<%@ include file="KiezAtlas.jsp" %>

<% begin(KiezAtlas.SERVLET_LIST, session, out); %>

<%
	HTMLGenerator html = (HTMLGenerator) session.getAttribute("html");
	BaseTopic cityMap = (BaseTopic) session.getAttribute("cityMap");
	Vector topics = (Vector) session.getAttribute("topics");
	BaseTopic geo = (BaseTopic) session.getAttribute("geo");
	Vector notifications = (Vector) session.getAttribute("notifications");
	String filterField = (String) session.getAttribute("filterField");
    // String membership = (String) session.getAttribute("membership");
	BaseTopic bean = null;
	if (topics.size() > 0) {
	    // get table headers, just if topics are provided
	    bean = (BaseTopic) topics.get(0);
	}
	//
	String disabledFormString;
	String selectedFilterField;
	if(filterField != null) {
	    disabledFormString = "";
	    selectedFilterField = filterField;
	} else {
	    disabledFormString = " disabled";
	    selectedFilterField = "";
	}
	out.println("<p><span class=\"heading\">" + cityMap.getName() + "</span>" +
		"&nbsp;&nbsp;&nbsp;|&nbsp;&nbsp;&nbsp;" +
		html.link("Alle Stadtpl&auml;ne anzeigen", KiezAtlas.ACTION_GO_HOME) + "</p>");
	out.println("<p>" + topics.size() + " Objekte");
    out.println("&nbsp;&nbsp;&nbsp;|&nbsp;&nbsp;&nbsp;" + html.link("Neues Objekt eingeben", KiezAtlas.ACTION_SHOW_EMPTY_GEO_FORM));
	if (bean != null) {
	    out.println("<form id=\"filterForm\" method=\"GET\" action=\"controller\">Enthält im Namen: ");
			// fieldOptions(bean, hiddenProps, hiddenPropsContaining, filterField) +
	    // -- had to encode the action into a hidden form element. cause of '?'
	    out.println("<input type=\"hidden\" name=\"action\" value=\""+KiezAtlas.ACTION_FILTER+"\">\n");
	    out.println("<input type=\"hidden\" name=\"filterField\" value=\"Name\">\n");
        out.println("<input type=\"text\" name=\"filterText\">\n");
	    out.println("<input type=\"submit\" value=\"Filtern\">\n");
	    out.println("</form>\n");
	}
	out.println("<form method=\"GET\" action=\"controller\" >");
	out.println("<input type=\"hidden\" name=\"action\" value=\""+KiezAtlas.ACTION_CLEAR_FILTER+"\">\n");
	out.println("<input type=\"submit\" value=\"Filter aufheben\"" + disabledFormString + ">\n");
	out.println("</form>\n");
	out.println("<br></p>");
	out.println(html.notification(notifications) + (notifications.size() > 0 ? "<br/><br/>" : ""));
	// --- list of institutions ---
	// String selectedID = geo != null ? geo.getID() : null;
	out.println("<b>Name</b><br/><br/>");
    out.println(html.linkList(topics, KiezAtlas.ACTION_SHOW_GEO_FORM, "deleteEntry"));
	//
%>
<% end(out); %>
