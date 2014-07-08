<%@ include file="KiezAtlas.jsp" %>

<% begin(KiezAtlas.SERVLET_LIST, session, out); %>
<%
	HTMLGenerator html = (HTMLGenerator) session.getAttribute("html");
	BaseTopic object = (BaseTopic) session.getAttribute("geo");
%>
<h2>Ihr neuer Datensatz <% object.getName(); %> wurde erfolgreich angelegt.</h2>

<p><a href="?action=showWorkspaceForm">Einen weiteren Datensatz anlegen</a></p>

<p>Danke f&uuml;r Ihre Unterst&uuml;tzung. </p>
<% end(out); %>
