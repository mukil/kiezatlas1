<%@ include file="KiezAtlas.jsp" %>

<% begin(session, out, true); %>

<%
	HTMLGenerator html = (HTMLGenerator) session.getAttribute("html");
	String imagePath = (String) session.getAttribute("imagePath");
	String forumActivition = (String) session.getAttribute("forumActivition");
	Integer commentCount = (Integer) session.getAttribute("commentCount");
	// hide properties
	TopicBean topicBean = (TopicBean) session.getAttribute("topicBean");
	topicBean.removeFieldsContaining("Password");
	topicBean.removeFieldsContaining("YADE");
	topicBean.removeFieldsContaining("Owner");
	topicBean.removeFieldsContaining("Web Alias");
	topicBean.removeFieldsContaining("Locked Geometry");
	topicBean.removeFieldsContaining("Description");
	topicBean.removeFieldsContaining("Birthday");
	topicBean.removeFieldsContaining("Gender");
	topicBean.removeField("Person / Mobile Number");
	topicBean.removeField("LONG");
	topicBean.removeField("LAT");
	topicBean.removeField("Person / Fax Number");
	topicBean.removeField("Person / Phone Number");
	topicBean.removeField("Person / Webpage");
	topicBean.removeField("Forum / Aktivierung");
	topicBean.removeField("Timestamp");
	// --- image ---
	String imageFile = topicBean.getValue("Image / File");
	if (!imageFile.equals("")) {
		out.println("<img src=" + imagePath + imageFile + "><br>");
	}	
	// --- name ---
	out.println("<b>" + topicBean.getValue("Name") + "</b><br>");
	// --- address ---
	// a "Stadt" property is preferred compared to a "City" topic (assigned to an "Address" topic)
	String city = topicBean.getValue("Stadt");
	if (city == null) {
		Vector tmp = topicBean.getValues("Address / City");
		if (tmp != null) {
			if (tmp.size() > 0) {
				city = ((BaseTopic) tmp.elementAt(0)).getName();
			} else {
				city = "";
			}						
		} else {
			city = "";
		}
	}			
	String street = topicBean.getValue("Address / Street");
	String postalCode = topicBean.getValue("Address / Postal Code");
	// address, probably with fahr-info link
	out.println(mapLink(street, postalCode, city));
	// --- generic geo object info ---
	// remove fields which are rendered manually
	topicBean.removeFieldsContaining("Image");
	topicBean.removeFieldsContaining("Icon");
	topicBean.removeField("Address / Street");
	topicBean.removeField("Address / Postal Code");
	topicBean.removeField("Address / Name");
	topicBean.removeField("Name");
	topicBean.removeField("Stadt");
	// geo object info
	out.println("<br><br>");
	out.println(html.info(topicBean, DeepaMehtaConstants.LAYOUT_STYLE_FLOW));
	// --- forum ---
	if (forumActivition.equals(KiezAtlas.SWITCH_ON)) {
		// link to forum page
		out.println("<p>\r<hr>\rDas " + html.link("Forum", KiezAtlas.ACTION_SHOW_GEO_FORUM) +
			" enth&auml;lt "+ commentCount + " Kommentare</p>");
	}
%>
<% end(session, out); %>
