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
	String instTypeID = (String) session.getAttribute("instTypeID");
	//
	// institution form
	out.println("<h2>Neues Objekt eingeben</h2>");
	out.println(html.form(instTypeID, KiezAtlas.ACTION_CREATE_GEO, hiddenProps, true));
%>
<% end(out); %>
