<%@ include file="KiezAtlas.jsp" %>

<% begin(session, out, true); %>
<%
	HTMLGenerator html = (HTMLGenerator) session.getAttribute("html");
	Vector insts = (Vector) session.getAttribute("institutions");
	Vector taggedInsts = (Vector) session.getAttribute("taggedInsts");
	Hashtable cats = (Hashtable) session.getAttribute("categories");
	Hashtable addresses = (Hashtable) session.getAttribute("addresses");
	String searchMode = (String) session.getAttribute("searchMode");
	String searchValue = (String) session.getAttribute("searchValue");
	String selectedCriteria = (String) session.getAttribute("defaultCriteria");
	// --- heading ---
	if (searchMode.equals(KiezAtlas.SEARCHMODE_BY_NAME)) {
		out.println("Suchergebnis ");
	}
  // --- ### align new back button at the right side
	out.println("<b>" + searchValue + "</b><br/>");
    out.println("<div class=\"small\">" + insts.size() + " Objekte&nbsp;&nbsp;&nbsp;&nbsp;");
    out.println("</div>");
    out.println("<a style=\"position:relative; top: 3px; left:215px;\" href=\"controller?action=" + KiezAtlas.ACTION_SHOW_CATEGORIES + "&critNr=" +selectedCriteria+ "\"><img src=\"http://www.kiezatlas.de/client/icons/back-arrow.gif\" height=\"13\" text=\"zurück\" title=\"zurück\" alt=\"zurück\" border=\"0\"></a>");
    out.println("<a class=\"small\" style=\"position:relative; left:218px;\" href=\"controller?action=" + KiezAtlas.ACTION_SHOW_CATEGORIES + "&critNr=" +selectedCriteria+ "\">zur&uuml;ck</a>");
    out.println("<a style=\"position:relative; top: 4px; left:222px;\" id=\"enumeration\" href=\"javascript:toggleNumeration();\"><img src=\"../images/pow-in-numbers-dark.png\" height=\"17\" title=\"Nummern einblenden\" alt=\"Nummern einblenden\" border=\"0\"></a>");
    // out.println("<a href=\"controller?action=" + KiezAtlas.ACTION_SHOW_CATEGORIES + "&critNr=" +selectedCriteria+ ">zur&uuml;ck</a>");
	// out.println();
	out.println("<p>");
	// --- list of institutions ---
	out.println("<table cellpadding=\"4\" cellspacing=\"0\">");
	Enumeration e = insts.elements();
  int listIndex = 1;
	while (e.hasMoreElements()) {
		BaseTopic inst = (BaseTopic) e.nextElement();
		out.println("<tr valign=\"top\">");
		out.println("<td align=\"right\">");
    topicImages((Vector) cats.get(inst.getID()), html, out);
		out.println("</td>");
		out.println("<td><span id=\"numbers\"><b>" + listIndex + ".</b></span>");
		out.println("<a href=\"controller?action=" + KiezAtlas.ACTION_SHOW_GEO_INFO + "&id=" + inst.getID() +
			"\">" + inst.getName() + "</a>");
		// address
		Hashtable address = (Hashtable) addresses.get(inst.getID());

		String street = (String) address.get(KiezAtlas.PROPERTY_STREET);
	  String postcode = (String) address.get(KiezAtlas.PROPERTY_POSTAL_CODE);
		String city = (String) address.get(KiezAtlas.PROPERTY_CITY);
		out.println("<br><small>" + (street != null ? street + "&nbsp;&nbsp;&nbsp;" : "") + (postcode != null ? postcode + " " : "") + city + "</small>");
		out.println("</td>");
		out.println("</tr>");
    listIndex++;
	}
	out.println("</table>");

%>
<% end(session, out); %>
