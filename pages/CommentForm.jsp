<%@ include file="KiezAtlas.jsp" %>

<% begin(session, out, false); %>
<%
	HTMLGenerator html = (HTMLGenerator) session.getAttribute("html");
	GeoObject inst = (GeoObject) session.getAttribute("selectedGeo");
	Vector comments = (Vector) session.getAttribute("geoComments");
%>
	<p><b><%= inst.name %> - Forum</b></p>
	<p>Kommentar schreiben</p>
	<form>
		<table>
		<tr>
			<td colspan="2"><textarea name="<%=KiezAtlas.PROPERTY_TEXT%>" rows="12" cols="40"></textarea></td>
		</tr>
		<tr>
			<td class="small">Autor</td>
			<td><input type="Text" name="<%=KiezAtlas.PROPERTY_COMMENT_AUTHOR%>"></td>
		</tr>
		<tr>
			<td class="small">Emailadresse<br>(wird nicht ver&ouml;ffentlicht)</td>
			<td><input type="Text" name="<%=KiezAtlas.PROPERTY_EMAIL_ADDRESS%>"></td>
		</tr>
		<tr>
			<td></td>
			<td><input type="Hidden" name="action" value="<%=KiezAtlas.ACTION_CREATE_COMMENT%>">
				<input type="Submit" value="OK"></td>
		</tr>
		</table>
	</form>
	<hr>
	<%
		Enumeration e = comments.elements();
		while (e.hasMoreElements()) {
			Comment comment = (Comment) e.nextElement();
			comment(comment, out);
		}
	%>
	<br>
	<p><%= html.link("Zur&uuml;ck zu " + inst.name, KiezAtlas.ACTION_SHOW_GEO_INFO, "id=" + inst.geoID) %></p>
<% end(session, out); %>
