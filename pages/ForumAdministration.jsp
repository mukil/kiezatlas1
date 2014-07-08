<%@ include file="KiezAtlas.jsp" %>

<% begin(KiezAtlas.SERVLET_EDIT, session, out); %>
<%
	HTMLGenerator html = (HTMLGenerator) session.getAttribute("html");
	BaseTopic inst = (BaseTopic) session.getAttribute("geo");
	String activition = (String) session.getAttribute("activition");
	Vector comments = (Vector) session.getAttribute("comments");
%>
	<h2><%= inst.getName() %> - Forum Administration</h2>
	Hier k&ouml;nnen Sie das Forum zu Ihrer Einrichtung aktivieren und deaktivieren.<br>
	Wenn das Forum aktiviert ist, haben Websurfer die M&ouml;glichkeit, Kommentare zu Ihrer Einrichtung zu hinterlassen.
	<p>
	Au&szlig;erdem haben Sie hier die M&ouml;glichkeit einzelne Kommentare zu l&ouml;schen.
	<hr>
	<% if (activition.equals(KiezAtlas.SWITCH_ON)) { %>
		<p>Das Forum ist aktiviert</p>
		<form>
			<input type="Hidden" name="action" value="<%= KiezAtlas.ACTION_DEACTIVATE_FORUM %>">
			<input type="Submit" value="Forum deaktivieren">
		</form>
		<hr>		
		<p>Das Forum enth&auml;lt <%= comments.size() %> Kommentare</p>
		<% Enumeration e = comments.elements();
		while (e.hasMoreElements()) {
			Comment comment = (Comment) e.nextElement();
			comment(comment, true, out); %>
			<form>
				<input type="Hidden" name="action" value="<%= KiezAtlas.ACTION_DELETE_COMMENT %>">
				<input type="Hidden" name="commentID" value="<%= comment.id %>">
				<input type="Submit" value="Kommentar l&ouml;schen">
			</form>
			<br>
		<% } %>
	<% } else { %>
		<p>Das Forum ist deaktiviert</p>
		<form>
			<input type="Hidden" name="action" value="<%= KiezAtlas.ACTION_ACTIVATE_FORUM %>">
			<input type="Submit" value="Forum aktivieren">
		</form>
	<% } %>
	<br>
	<p><%= html.link("Zur&uuml;ck zu " + inst.getName(), KiezAtlas.ACTION_GO_HOME) %></p>

<% end(out); %>
