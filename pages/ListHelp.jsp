<%@ include file="KiezAtlas.jsp" %>

<%@page contentType="text/html" pageEncoding="iso-8859-1"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
   "http://www.w3.org/TR/html4/loose.dtd">

<%  BaseTopic loggedIn = (BaseTopic) session.getAttribute("user");
    out.println("<h3>Erl&auml;uterungen zu den neuen Funktionen des Listenzugangs</h3>");
    out.println("<dl>");
    //
    out.println("<dt><img src=\"../images/slimList.png\"></dt><dd>Dieses Icon repr&auml;sentiert den bereits bekannten Zugang zu den <i>schlanken Listen</i></dd>");
    out.println("<dt><img src=\"../images/export.png\"></dt><dd>Dieses Icon veranlasst den Export &ouml;ffentlicher Daten eines <i>neuen</i> Stadtplans</dd>");
    out.println("<dt><img src=\"../images/document-save.png\"></dt><dd>Dieses Icon f&uuml;hrt zur Webseite die eine <i>Downloaddatei mit aktuellen Daten</i> bereitstellt. " +
            "Durch das positionieren des Mauszeigers &uuml;ber dem Symbol sollte in jedem Browser ein Zeitstempel sichtbar werden.</dd>");
    out.println("<dt><img src=\"../images/reload.png\"></dt><dd>Dieses Icon erm&ouml;glicht das <i>erneuern der Stadtplandaten die in der Downloaddatei zur Verf&uuml;gung stehen</i>. " +
            "Diese Funktion wird momentan je Stadtplan einmal alle 6 Stunden erm&ouml;glicht. Nach dem ansto&szlig;en dieser Aktion kann es bis zu zehn Minuten dauern bis eine neue Downloaddatei die bis dahin aktuellste Version tats&auml;lich ersetzt.</dd>");
    out.println("<dt><img src=\"../images/email.gif\">Dieses Icon bietet den Zugang zu der Funktion zum Erstellen einer Rundmail-Empfängerliste.</dt>");
    //
    out.println("</dl>");
    //
    out.println("<h3>Erweiterte Anleitung zum Umgang mit Listen in Microsoft Exel</h3>");
    out.println("<a href=\"http://www.kiezatlas.de/client/documents/AnleitungExceltabellen.pdf\">Download der Anleitung (0.5 MB .pdf)</a> <br/>Der Autor steht Anregungen und Erg&auml;nzungen prinzipiell eher freundlich gegen&uuml;ber (<a href=\"mailto:Ralph.Baumann@libg.verwalt-berlin.de\">Kontakt</a>)");
    //
    if (loggedIn != null) { // session authenticated
        out.println("<p/><span class=\"small\"><a href=\"?action="+KiezAtlas.ACTION_GO_HOME+"\" class=\"small\">zur&uuml;ck zur Listenansicht</a></span>");
    }
%>
