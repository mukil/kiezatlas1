<%@ include file="KiezAtlas.jsp" %>

<% begin(KiezAtlas.SERVLET_EDIT, session, out); %>
<%!
	static String[] hiddenProps = {
		"Name",
		KiezAtlas.PROPERTY_DESCRIPTION,
		KiezAtlas.PROPERTY_ICON,
		KiezAtlas.PROPERTY_ADMINISTRATION_INFO,
		KiezAtlas.PROPERTY_WEB_ALIAS,
		KiezAtlas.PROPERTY_YADE_X,
		KiezAtlas.PROPERTY_YADE_Y,
		KiezAtlas.PROPERTY_LAST_MODIFIED,
		"Forum Aktivierung",	/* can't use PROPERTY_FORUM_ACTIVITION because       */
								/* relabled by GeoObjectTopic's propertyLabel() hook */
		"Title", "Content",
		"Width", "Height"};
%>
<%
	HTMLGenerator html = (HTMLGenerator) session.getAttribute("html");
	BaseTopic geo = (BaseTopic) session.getAttribute("geo");
	//
	// institution form
	out.println("<H2>" + geo.getName() + "</H2>");
	out.println(html.form(geo.getType(), KiezAtlas.ACTION_UPDATE_GEO,
						  geo.getID(), hiddenProps, true));
%>
<% end(out); %>
