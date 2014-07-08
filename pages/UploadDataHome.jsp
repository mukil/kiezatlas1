<%@ include file="KiezAtlas.jsp" %>

<% begin(KiezAtlas.SERVLET_UPLOAD, session, out); %>
<%
        /*
	Vector workspaces = (Vector) session.getAttribute("workspaces");
	Hashtable cityMaps = (Hashtable) session.getAttribute("cityMaps");
	//
	out.println("<dl>");
	//
	Enumeration e = workspaces.elements();
	while (e.hasMoreElements()) {
		BaseTopic workspace = (BaseTopic) e.nextElement();
		out.println("<dt>" + workspace.getName() + "</dt>");
		Enumeration e2 = ((Vector) cityMaps.get(workspace.getID())).elements();
		while (e2.hasMoreElements()) {
			BaseTopic cityMap = (BaseTopic) e2.nextElement();
			out.println("<dd><a href=\"?action=" + KiezAtlas.ACTION_SHOW_INSTITUTIONS +
				"&cityMapID=" + cityMap.getID() + "\">" + cityMap.getName() + "</a></dd>");
		}
	}
	out.println("</dl>");
        */
%>
<h2>Zugang zum Upload von Neuk&ouml;lln-Stadtinfo-Daten</h2>
    <form name="StadtinfoData" method="POST" enctype="multipart/form-data">
        W&auml;hlen Sie eine Datei aus:<br><br>
        <input type="file" name="File2Upload" value="" width="20"><br><br>
        <input type="submit" value="UploadDataAction" name="Upload">
        <input type="hidden" name="action" value="UploadDataAction">
    </form>
<% end(out); %>
