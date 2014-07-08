<%@ include file="KiezAtlas.jsp" %>

<% begin(KiezAtlas.SERVLET_LIST, session, out); %>
<%
	Vector categories = (Vector) session.getAttribute("availableCategories");
	// Vector topicIds = (Vector) session.getAttribute("topicIds");
	// Hashtable cityMaps = (Hashtable) session.getAttribute("cityMaps");
	// Hashtable mapCounts = (Hashtable) session.getAttribute("mapCounts");
	// Hashtable mapTimes = (Hashtable) session.getAttribute("mapTimes");
	String cityMapName = (String) session.getAttribute("cityMapName");
	String cityMapId = (String) session.getAttribute("cityMapID");
	String filterField = (String) session.getAttribute("filterField");
	Vector filterFieldNames = (Vector) session.getAttribute("filterFieldNames");
	String recipients = (String) session.getAttribute("recipients");
    //
    out.println("<h3>Filtern zum Erstellen einer Rundmail-Empf&auml;ngerliste im Stadtplan \""+cityMapName+"\"</h3>");
    out.println("<div class=\"small\" style=\"position: absolute; right: 75px;\"><a href=\"?action="+KiezAtlas.ACTION_GO_HOME+"\">zur&uuml;ck</a></div>");
    //
    if (filterFieldNames.size() >= 1) {
        // out.println("<div class=\"notification.info\">Alle <i>Geo-Objekte</i> aus der Kategorie \""+filterFieldNames.get(filterFieldNames.size()-1)+"\" wurden der Empf&auml;ngerliste hinzugefügt.</div><p/>");
        out.println("<div class=\"notification.info\">Die aktuelle Empf&auml;ngerliste beinhaltet <i>" +((recipients.split(",").length) - 1) + " Geo-Objekte</i> die den folgenden Kriterien zugeordnet sind:</div><p/>");
        for (int i = 0; i < filterFieldNames.size(); i++) {
            out.println("&nbsp;&nbsp;&nbsp;<b>" + filterFieldNames.get(i) + "</b><br/>");
        }
        out.println("<p/>");
    }
    out.println("<form id=\"filterForm\" method=\"GET\" action=\"controller\" >\n<select height=\"400\" name=\"filterField\">"
         + fieldOptionalIds(categories, filterField) + "</select>\n");
    // -- had to encode the action into a hidden form element. cause of '?'
    out.println("<input type=\"hidden\" name=\"action\" value=\""+KiezAtlas.ACTION_CREATE_ROUNDMAILING+"\">\n");
    // out.println("<input type=\"text\" name=\"filterText\">\n");
    out.println("<input type=\"submit\" value=\"zum Verteiler hinzuf&uuml;gen\">\n");
    out.println("</form>\n");
    out.println("<p/><b><a href=\"?action="+KiezAtlas.ACTION_FILTER_MAIL_ALL+"&cityMapID="+cityMapId+"\">Alle Adressen Hinzufügen</a></b>");
    if (!recipients.equals("")) {
        out.println("<b>&nbsp;&nbsp;&nbsp;<a href=\"mailto:?bcc="+recipients+"\">Rundmail erstellen</a>");
        out.println("&nbsp;&nbsp;&nbsp;<a href=\"?action="+KiezAtlas.ACTION_FILTER_ROUNDMAILING+"&cityMapID="+cityMapId+"\">Empf&auml;ngerliste zur&uuml;cksetzen</a></b>");
    }
%>
<% end(out); %>
