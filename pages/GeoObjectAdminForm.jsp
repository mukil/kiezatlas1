<%@ include file="KiezAtlas.jsp" %>

<% begin(KiezAtlas.SERVLET_LIST, session, out); %>
<%!
	static String[] hiddenProps = {
		KiezAtlas.PROPERTY_DESCRIPTION,
		KiezAtlas.PROPERTY_ICON,
		KiezAtlas.PROPERTY_LAST_MODIFIED,
		"Title", "Content",
		"Width", "Height"};
%>
<%
	HTMLGenerator html = (HTMLGenerator) session.getAttribute("html");
	BaseTopic geo = (BaseTopic) session.getAttribute("geo");
	//
	// institution form
	out.println("<h2>" + geo.getName() + "</h2>");
	out.println(html.form(geo.getType(), KiezAtlas.ACTION_UPDATE_GEO,
						  geo.getID(), hiddenProps, true));
%>
<% end(out); %>
