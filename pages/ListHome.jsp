<%@ include file="KiezAtlas.jsp" %>

<% begin(KiezAtlas.SERVLET_LIST, session, out); %>
<%
	Vector workspaces = (Vector) session.getAttribute("workspaces");
	Hashtable cityMaps = (Hashtable) session.getAttribute("cityMaps");
	Hashtable mapCounts = (Hashtable) session.getAttribute("mapCounts");
	Hashtable mapTimes = (Hashtable) session.getAttribute("mapTimes");
	String membership = (String) session.getAttribute("membership");
    BaseTopic user = (BaseTopic) session.getAttribute("user");
    out.println("<div class=\"small\" style=\"position: absolute; right: 75px;\"><a href=\"?action="+KiezAtlas.ACTION_SHOW_LIST_LEGEND+"\">zur Legende</a></div>");
    //
	out.println("<dl style=\"width: 700px;\">");
	//
	Enumeration e = workspaces.elements();
	while (e.hasMoreElements()) {
		BaseTopic workspace = (BaseTopic) e.nextElement();
		out.println("<dt><b>" + workspace.getName() + "</b></dt>");
		Enumeration e2 = ((Vector) cityMaps.get(workspace.getID())).elements();
		while (e2.hasMoreElements()) {
			BaseTopic cityMap = (BaseTopic) e2.nextElement();
					out.println("<dd><a href=\"?action=" + KiezAtlas.ACTION_SHOW_INSTITUTIONS + "&cityMapID=" + cityMap.getID() + "&workspaceID=" + workspace.getID() + "\">" +
                            ""+cityMap.getName()+"</a>&nbsp;<span class=\"small\">("+ mapCounts.get(cityMap.getID()) +")</span>&nbsp;&nbsp;");
                if (!membership.equals("Affiliated")) {
                    out.println("<a href=\"?action=" + KiezAtlas.ACTION_SHOW_INSTITUTIONS_SLIM +
                    "&cityMapID=" + cityMap.getID() + "&workspaceID=" + workspace.getID() + "\" class=\"small\"><img src=\"../images/slimList.png\" " +
                            "border=\"0\" height=\"15px\" width=\"15px\" title=\"zur schlanken Liste\" alt=\"zur schlanken Liste\"></a>");
                }
                // !membership.equals("Affiliated")
                if (mapTimes.get(cityMap.getID()).equals("")) {
                    // out.println("<a href=\"?action=" + KiezAtlas.ACTION_EXPORT_CITYMAP +
                       // "&cityMapID=" + cityMap.getID() + "\" class=\"small\">erstelle Downloaddatei</a></dd>");
                    out.println("<a href=\"?action=" + KiezAtlas.ACTION_EXPORT_CITYMAP +
                        "&cityMapID=" + cityMap.getID() + "\" class=\"small\"><img src=\"../images/export.png\" " +
                        "border=\"0\" height=\"17px\" width=\"17px\" title=\"Erzeuge Downloaddatei\" alt=\"Erzeuge Downloaddatei\"></a>");
                } else {
                    if (!mapTimes.get(cityMap.getID()).equals("isUpToDate")) {
                        out.println("<a href=\"?action=" + KiezAtlas.ACTION_DOWNLOAD_CITYMAP +
                        "&cityMapID=" + cityMap.getID() + "\" class=\"small\"><img src=\"../images/document-save.png\" " +
                                "border=\"0\" height=\"15px\" width=\"15px\" title=\"zur Downloaddatei vom "+mapTimes.get(cityMap.getID()) + "\" alt=\"zur Downloaddatei vom "+mapTimes.get(cityMap.getID()) + "\"></a>");
                        out.println("<a href=\"?action=" + KiezAtlas.ACTION_EXPORT_CITYMAP +
                            "&cityMapID=" + cityMap.getID() + "\" class=\"small\"><img src=\"../images/reload.png\" " +
                            "border=\"0\" height=\"15px\" width=\"15px\" title=\"Downloaddatei aktualisieren\" alt=\"Downloaddatei aktualisieren\"></a>");
                    } else {
                        out.println("<a href=\"?action=" + KiezAtlas.ACTION_DOWNLOAD_CITYMAP +
                        "&cityMapID=" + cityMap.getID() + "\" class=\"small\"><img src=\"../images/document-save.png\" " +
                                "border=\"0\" height=\"15px\" width=\"15px\" title=\"zur aktuellen Downloaddatei\" alt=\"zur aktuellen Downloaddatei\"></a>");
                    }
                }
                out.println("<a href=\"?action=" + KiezAtlas.ACTION_FILTER_ROUNDMAILING +
                    "&cityMapID=" + cityMap.getID() + "\" class=\"small\"><img src=\"http://www.kiezatlas.de/client/icons/mail.gif\" " +
                    "border=\"0\" height=\"15px\" width=\"15px\" title=\"zum verfassen einer Rundmail\" alt=\"zum verfassen einer Rundmail\"></a>");
            out.println("</dd>");
		}
	}
	out.println("</dl>");
%>
<% end(out); %>
