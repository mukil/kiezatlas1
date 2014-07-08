<%@ include file="KiezAtlas.jsp" %>

<% begin(KiezAtlas.SERVLET_LIST, session, out); %>
<%!
	static String[] hiddenProps = {
		"Image", "Image / Height", "Image / Width", 
		"Image / File",  "Image / Name", "Width", "Height",
		"Address / Name", "Person / Birthday"}; // "Address / City" can not be removed for the listtopicBean generator, due to round letter feature
	static String[] hiddenPropsContaining = { "Owner", "Locked Geometry", KiezAtlas.PROPERTY_DESCRIPTION, KiezAtlas.PROPERTY_ICON, "Person / Address" };
	static String[] hiddenPropsContainingAffiliated = { "Owner", "Locked Geometry", KiezAtlas.PROPERTY_DESCRIPTION, KiezAtlas.PROPERTY_ICON, "Person / Address","YADE", "Alias", "Password", "LAT", "LONG"};
%>
<%

	HTMLGenerator html = (HTMLGenerator) session.getAttribute("html");
	BaseTopic cityMap = (BaseTopic) session.getAttribute("cityMap");
	Vector topics = (Vector) session.getAttribute("topics");
	BaseTopic geo = (BaseTopic) session.getAttribute("geo");
	Vector notifications = (Vector) session.getAttribute("notifications");
	String filterField = (String) session.getAttribute("filterField");
	String sortField = (String) session.getAttribute("sortField");
	Vector mailboxes = (Vector) session.getAttribute("emailList");
  String membership = (String) session.getAttribute("membership");
	TopicBean bean = null;
	if (topics.size() > 0) {
	    // get table headers, just if topics are provided
	    bean = (TopicBean) topics.get(0);
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
    if (membership.equals("")) out.println("&nbsp;&nbsp;&nbsp;|&nbsp;&nbsp;&nbsp;" + html.link("Neues Objekt eingeben", KiezAtlas.ACTION_SHOW_EMPTY_GEO_FORM));
	if (bean != null) {
	    out.println("<form id=\"filterForm\" method=\"GET\" action=\"controller\" >\n<select name=\"filterField\">" + 
			fieldOptions(bean, hiddenProps, hiddenPropsContaining, filterField) + "</select>\n");
	    // -- had to encode the action into a hidden form element. cause of '?'
	    out.println("<input type=\"hidden\" name=\"action\" value=\""+KiezAtlas.ACTION_FILTER+"\">\n");
        out.println("<input type=\"text\" name=\"filterText\">\n");
	    out.println("<input type=\"submit\" value=\"Filtern\">\n");
	    out.println("</form>\n");
	}
	out.println("<form method=\"GET\" action=\"controller\" >");
	out.println("<input type=\"hidden\" name=\"action\" value=\""+KiezAtlas.ACTION_CLEAR_FILTER+"\">\n");
	out.println("<input type=\"submit\" value=\"Filter aufheben\"" + disabledFormString + ">\n");
	out.println("</form>\n");
	out.println("<br></p>");
	out.println(html.notification(notifications) + (notifications.size() > 0 ? "<br><br>" : ""));
	// --- list of institutions ---
	String selectedID = geo != null ? geo.getID() : null;
	if (sortField == null) {
	    if (membership.equals("")) {
            out.println(html.listTopicBeans(topics, selectedID, hiddenProps, hiddenPropsContaining, true, KiezAtlas.ACTION_SHOW_GEO_FORM, KiezAtlas.ACTION_SORT_BY, "deleteEntry", true, null));
        } else {
            out.println(html.listTopicBeans(topics, selectedID, hiddenProps, hiddenPropsContainingAffiliated, true, "", KiezAtlas.ACTION_SORT_BY, "", true, null));
        }
	} else {
        if (membership.equals("")) {
            System.out.println(">>> highlight Column: " + sortField);
            out.println(html.listTopicBeans(topics, selectedID, hiddenProps, hiddenPropsContaining, true, KiezAtlas.ACTION_SHOW_GEO_FORM, KiezAtlas.ACTION_SORT_BY, "deleteEntry", true, sortField));
        } else {
            out.println(html.listTopicBeans(topics, selectedID, hiddenProps, hiddenPropsContainingAffiliated, true, "", KiezAtlas.ACTION_SORT_BY, "", true, sortField));
        }
	}
	//
	out.println("<p>");
	// check for displaying mailbox feature
	if (mailboxes != null && mailboxes.size() > 0) {
	    out.println("<b><a href=\""+mailtoUrl(mailboxes)+"\">Rundmail verfassen</a></b>");
	    String adressLabel = mailboxes.size() == 1 ? " Adresse" : " Adressen";
	    out.println(" (an " + mailboxes.size()  + adressLabel + ")");
	}
	out.println(html.link("Steuerdatei für Serienbrief erstellen", KiezAtlas.ACTION_CREATE_FORM_LETTER));
	out.println("</p>");
%>
<% end(out); %>
