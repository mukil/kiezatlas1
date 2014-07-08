<%@ include file="KiezAtlas.jsp" %>

<%@page contentType="text/html" pageEncoding="iso-8859-1"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
   "http://www.w3.org/TR/html4/loose.dtd">

<%  String link = (String) session.getAttribute("link");
    String title = (String) session.getAttribute("title");
    out.println("<h3>"+title+"</h3>");
    out.println("<p>Bitte klicken Sie mit der rechten Maustaste auf den untenstehenden Link und wählen Sie \"Ziel Speichern unter\".</p>");
    out.println("<a href=\"" + link + "\">Stadtplandaten herunterladen</a>");
    //
    out.println("<p><i>Hinweis: Die aktuellen Daten sind als sog \".csv\"-Datei unter der Adresse st&auml;ndig abrufbar. " +
            "F&uuml;r den Import in ein g&auml;ngiges Tabellenkalkulationsprogramm ist folgendes zu beachten: " +
            "Die Daten liegen in jetzt in der Westeurop&auml;ischen Kodierung vor  (ISO-8859-1) und die Spalten sind mittels <b>Tab</b> voneinander getrennt. " +
            "Kommatas werden zur Trennung nicht eingesetzt. Beim Importvorgang kann es sein dass diese Eigenschaften vom Benutzer anzugeben sind.</i></p>");
%>
