<%@ include file="KiezAtlas.jsp" %>

<% begin(KiezAtlas.SERVLET_EDIT, session, out); %>
<%
	HTMLGenerator html = (HTMLGenerator) session.getAttribute("html");
	TopicBean topicBean = (TopicBean) session.getAttribute("topicBean");
	String imageFile = (String) session.getAttribute("imagefile");
	//
	// render image if available
	if (imageFile != null) {
		out.println("</br><img src=" + imageFile + ">");
	}
	//
	topicBean.removeFieldsContaining("Image");
	topicBean.removeField("Web Alias");
	topicBean.removeField("Password");
	topicBean.removeField("Address / Name");
	topicBean.removeField("Address / Description");
	topicBean.removeField("Forum / Aktivierung");
	topicBean.removeFieldsContaining("Owner ID");
	topicBean.removeFieldsContaining("Description");
	topicBean.removeFieldsContaining("Locked Geometry");
	topicBean.removeFieldsContaining("Icon");
	topicBean.removeFieldsContaining("YADE");
	//
	out.println("<H2>" + topicBean.getValue("Name") + "</H2>");
	out.println(html.info(topicBean, DeepaMehtaConstants.LAYOUT_STYLE_FLOW));
	//
	// links to form page and forum administration
	out.println("<p>\r<br>\r" + html.link("Zum &Auml;nderungsformular", KiezAtlas.ACTION_SHOW_GEO_FORM) + "</p>");
	out.println("<p>\r<hr>\r" + html.link("Zur Forum Administration", KiezAtlas.ACTION_SHOW_FORUM_ADMINISTRATION) + "</p>");
%>
<% end(out); %>
