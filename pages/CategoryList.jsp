<%@ include file="KiezAtlas.jsp" %>

<% begin(session, out, true); %>
<%
	HTMLGenerator html = (HTMLGenerator) session.getAttribute("html");
	Vector cats = (Vector) session.getAttribute("categories");
	Vector selCats = (Vector) session.getAttribute("selectedCats");
	//
	out.println(html.topicSelector(cats, selCats, KiezAtlas.ACTION_SEARCH_BY_CATEGORY, KiezAtlas.ACTION_SELECT_CATEGORY));
%>
<% end(session, out); %>
