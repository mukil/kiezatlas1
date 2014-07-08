<%@ include file="KiezAtlas.jsp" %>
<%
	String mapImage = (String) session.getAttribute("mapImage");
	Vector hotspots = (Vector) session.getAttribute("hotspots");
	Vector shapeTypes = (Vector) session.getAttribute("shapeTypes");
	Vector shapes = (Vector) session.getAttribute("shapes");
	Vector cluster = (Vector) session.getAttribute("cluster");
    Boolean enumerationFlag = (Boolean) session.getAttribute("enumerationFlag");
    String selectedCatId = (String) session.getAttribute("selectedCatId");
	GeoObject selectedInst = (GeoObject) session.getAttribute("selectedGeo");
	String stylesheet = (String) session.getAttribute("stylesheet");
%>
<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\">
<html>
<head>
	<title>Kiezatlas</title>
	<meta http-equiv="content-type" content="text/html; charset=iso-8859-1">
	<style type="text/css">
        <%= stylesheet %>
        <%
        if (enumerationFlag) {
            out.println("\n.numbers { visibility: hidden; font: Arial, sans; color: #002755; background: #fff; font-size:12px; font-weight: bold; font-decoriation: none; }");
        } else {
            out.println("\n.numbers { visibility: hidden; }");
        }
        %>
	</style>
	<!--[if lt IE 7]>
	<script defer type="text/javascript" src="../pages/pngfix.js"></script>
	<script defer type="text/javascript" src="../pages/fixed.js"></script>
	<![endif]-->
	<!-- For running this app in IE 7, css-strict mode has to be enabled via a doctype definition -->
	<script type="text/javascript">

		var currentActiveMenu = "";
		var YOffset = 0;
		var XOffset = 0;

		function showMenu(id) {
			if (currentActiveMenu != "") {
				hideMenu(currentActiveMenu);
			}
			if (document.getElementById) {
				var currentMenu = "clusterMenu"+id;
				originalPosition = id.split(".");
				var xPosition = parseInt(originalPosition[0]);
				var yPosition = parseInt(originalPosition[1]);
				var myregex = /MSIE 6\.0/i;
				if (navigator.appName == "Microsoft Internet Explorer" && navigator.appVersion.indexOf("6.0") != -1) {
					// ie 6, no recalculation, handled by included 'fixed.js' script
					document.getElementById(currentMenu).style.top = yPosition;
					document.getElementById(currentMenu).style.left = xPosition;
				} else {
					// everything but ie 6, recalculate position after scrolling
					document.getElementById(currentMenu).style.top = yPosition - independentY();
					document.getElementById(currentMenu).style.left = xPosition - independentX();
				}
				document.getElementById(currentMenu).style.visibility = 'visible';
					currentActiveMenu = id;
			}
		}

		function hideMenu(id) {
			if (document.getElementById) {
				var currentMenu = "clusterMenu"+id;
				document.getElementById(currentMenu).style.visibility = 'hidden';
				currentActiveMenu = "";
			}
		}

		// helper called for browser independency
		function independentY() {
			if (navigator.appName == "Microsoft Internet Explorer") {
			    if(navigator.appVersion.indexOf("7.0") != -1) return document.documentElement.scrollTop;
			    else return document.body.scrollTop;
			} else {
				return window.pageYOffset;
			}
		}

		function independentX() {
			if (navigator.appName == "Microsoft Internet Explorer") {
			    if(navigator.appVersion.indexOf("7.0") != -1) return document.documentElement.scrollLeft;
			    else return document.body.scrollLeft;
			} else {
			    return window.pageXOffset;
			}
		}

	</script>
</head>
<body>
	<img src="<%= mapImage %>" style="position:absolute; top:0px; left:0px;">
	<%
		// --- shapes ---
		Enumeration e = shapes.elements();
		while (e.hasMoreElements()) {
			Shape shape = (Shape) e.nextElement();
			if (isSet(shape.targetWebalias)) {
				out.println("<a href=\"" + request.getContextPath() + "/browse/" + shape.targetWebalias + "\" target=\"_top\">");
			}
			out.println("<img src=\"" + shape.url + "\" class=\"fixpng\" style=\"position:absolute; top:" +
				shape.point.y + "px; left:" + shape.point.x + "px; width:" + shape.size.width + "px; height:" +
				shape.size.height + "px;\">");
			if (isSet(shape.targetWebalias)) {
				out.println("</a>");
			}
		}
		// --- marker first ---
		e = hotspots.elements();
		while (e.hasMoreElements()) {
			Vector presentables = (Vector) e.nextElement();
			Enumeration e2 = presentables.elements();
			String icon = (String) e2.nextElement();
			while (e2.hasMoreElements()) {
				PresentableTopic inst = (PresentableTopic) e2.nextElement();
				Point p = inst.getGeometry();
				// marker and hotspot can't overlap exactly, cause of the new icons don't fit in the old convention of 20x20px chel
				if (selectedInst != null && selectedInst.geoID.equals(inst.getID())) {
					out.println("<img src=\"../images/marker.png\" class=\"fixpng\" style=\"position:absolute; top:" +
						(p.y - 20) + "px; left:" + (p.x - 20) + "px; width:40px; height:40px;\">");
					break;
				}
			}
		}

		// --- geoObjects ---
		e = hotspots.elements();
        int catIndex = 0;
		while (e.hasMoreElements()) {
			Vector presentables = (Vector) e.nextElement();
			Enumeration e2 = presentables.elements();
			String icon = (String) e2.nextElement();
            boolean enumerateThis = false;
            int presentableIndex = 0;
            if (selectedCatId != null) {
                if (selectedCatId.equals("") || catIndex == Integer.parseInt(selectedCatId)) {
                    enumerateThis = true;
                }
            }
            catIndex++;
			while (e2.hasMoreElements()) {
                presentableIndex++;
                //
				PresentableTopic inst = (PresentableTopic) e2.nextElement();
				Point p = inst.getGeometry();
				out.println("<a href=\"javascript:top.frames.right.location.href='controller?action=" +
					KiezAtlas.ACTION_SHOW_GEO_INFO + "&id=" + inst.getID() + "'\">" +
					"<img src=\"" + icon + "\" style=\"position:absolute; top:" + (p.y - 7) + "px; left:" +
					(p.x - 7) + "px;\" alt=\"" + inst.getName() + "\" title=\"" + inst.getName() + "\" border=\"0\">");
                    if (enumerateThis) {
                        out.println("<div id=\"numbers\" style=\"position: absolute; top:" + (p.y - 7) + "px; left:" + (p.x - 5) + "px;\" class=\"numbers\">"+ presentableIndex +"</div></a>");
                    }
			}
		}
		// cluster icons
		e = cluster.elements();
		while (e.hasMoreElements()) {
			//entered vector of cluster
			Cluster myCluster = (Cluster) e.nextElement();
			Point p = myCluster.getPoint();
			Vector presentables = myCluster.getPresentables();
			Enumeration e2 = presentables.elements();
			String iconPath = myCluster.getIcon();
			out.println("<a href=\"#\" onMouseOver=\"showMenu('"+p.x+"."+p.y+"')\"><img src=\""+iconPath+"\" style=\"position:absolute; top:" + (p.y - 10) + "px; left:" + (p.x - 10) + "px;\" alt=\"Cluster\" border=\"0\"></a>");
			//out.println("<div id=\"clusterMenu"+p.x+"."+p.y+"\" onMouseOut=\"hideMenu('"+p.x+"."+p.y+"')\" style=\"position:fixed; visibility:hidden; border:1px; dashed black; padding:5px; top:"+ (p.y -10) + "px; left:" + (p.x + 10)+ "px; overflow:visible; background-color:#b0b0ea;\">");
			//while(e2.hasMoreElements()) {
			//	PresentableTopic currentTopic = (PresentableTopic) e2.nextElement();
			//	out.println("<a href=\"javascript:top.frames.right.location.href='controller?action=" +
			//			KiezAtlas.ACTION_SHOW_GEO_INFO + "&id=" + currentTopic.getID() + "'\" onMouseOver=\"showMenu('"+p.x+"."+p.y+"')\">"+currentTopic.getName()+"</a></br>");
			//}
			//out.println("</div>");
		}
		// cluster menus are rendered in an extra loop after the cluster images for optic reasons
		e = cluster.elements();
		while (e.hasMoreElements()) {
			//entered vector of cluster
			Cluster myCluster = (Cluster) e.nextElement();
			Point p = myCluster.getPoint();
			Vector presentables = myCluster.getPresentables();
			Enumeration e2 = presentables.elements();
			//String iconPath = myCluster.getIcon();
			//out.println("<a href=\"#\" onMouseOver=\"showMenu('"+p.x+"."+p.y+"')\"><img src=\""+iconPath+"\" style=\"position:absolute; top:" + (p.y - 10) + "px; left:" + (p.x - 10) + "px;\" alt=\"Cluster\" border=\"0\"></a>");
			out.println("<div id=\"clusterMenu"+p.x+"."+p.y+"\" onMouseOut=\"hideMenu('"+p.x+"."+p.y+"')\" style=\"position:fixed; visibility:hidden; border:1px; dashed black; padding:5px; top:"+ (p.y -10) + "px; left:" + (p.x + 10) + "px; white-space:nowrap; overflow:visible; background-color:#b0b0ea;\">");
			while(e2.hasMoreElements()) {
				PresentableTopic currentTopic = (PresentableTopic) e2.nextElement();
				out.println("<a href=\"javascript:top.frames.right.location.href='controller?action=" +
						KiezAtlas.ACTION_SHOW_GEO_INFO + "&id=" + currentTopic.getID() + "'\" onMouseOver=\"showMenu('"+p.x+"."+p.y+"')\">"+currentTopic.getName()+"</a></br>");
			}
			out.println("</div>");
		}
		// --- shape display switches ---
		if (shapeTypes.size() > 0) {
			out.println("<form style=\"position:fixed; left:0px; bottom:0px; width:300px\">");
			for (int i = 0; i < shapeTypes.size(); i++) {
				ShapeType shapeType = (ShapeType) shapeTypes.elementAt(i);
				int y = KiezAtlas.SHAPE_LEGEND_HEIGHT * (shapeTypes.size() - i - 1);
				out.println("<input type=\"checkbox\"" + (shapeType.isSelected ? " checked" : "") +
					" style=\"position:absolute; left:0px; bottom:" + y + "px;\" onclick=\"location.href='controller?action=" +
					KiezAtlas.ACTION_TOGGLE_SHAPE_DISPLAY + "&typeID=" + shapeType.typeID + "'\">");
				out.println("<div style=\"position:absolute; left:20px; bottom:" + y + "px; color:" + shapeType.color +
					"; background:" + shapeType.color + "; opacity:0.5;\">" + shapeType.name + "</div>");
				out.println("<div style=\"position:absolute; left:20px; bottom:" + y + "px;\">" + shapeType.name + "</div>");
			}
			out.println("</form>");
		}
	%>
</body>
</html>
