<%@ include file="KiezAtlas.jsp" %>

<% begin(session, out, false); %>
<%
	HTMLGenerator html = (HTMLGenerator) session.getAttribute("html");
	GeoObject geo = (GeoObject) session.getAttribute("selectedGeo");
	Vector comments = (Vector) session.getAttribute("geoComments");
%>
	<p><b><%= geo.name %> - Forum</b></p>
	<p class="small">Das Forum enth&auml;lt <%= comments.size() %> Kommentare</p>
	<p><%= html.link("Kommentar schreiben", KiezAtlas.ACTION_SHOW_COMMENT_FORM) %></p>
	<%
		Enumeration e = comments.elements();
		while (e.hasMoreElements()) {
			Comment comment = (Comment) e.nextElement();
			comment(comment, out);
		}
	%>
	<br>
	<p><%= html.link("Zur&uuml;ck zu " + geo.name, KiezAtlas.ACTION_SHOW_GEO_INFO, "id=" + geo.geoID) %></p>
<% end(session, out); %>
