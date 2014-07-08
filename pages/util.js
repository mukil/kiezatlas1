/* 
 * Malte Rei&szlig;ig, 15.January 2009 (mre@deepamehta.de)
 * This is a small little helper for the List Page
 * It checks wether the delete action should be forwarded to the server or 
 * the request should is cancelled by the user
 */

function confirmDelete () {
  if (confirm('Wollen Sie den Eintrag wirklich ganz entfernen ?')) {
    return true;
  } else  {
    return false;
  }
}

function toggleNumeration() {
    // citymap
    if (jQuery("#numbers", window.parent.frames["left"].document).css("visibility") == "visible") {
        jQuery("#numbers", window.parent.frames["left"].document).css("visibility", "hidden");
        jQuery("#enumeration", window.parent.frames["right"].document).html("<img src=\"../images/pow-in-numbers-dark.png\" height=\"17\" title=\"Nummern einblenden\" alt=\"Nummern einblenden\" border=\"0\">");
    } else {
        jQuery("#numbers", window.parent.frames["left"].document).css("visibility", "visible");
        jQuery("#enumeration", window.parent.frames["right"].document).html("<img src=\"../images/pow-in-numbers-bright.png\" height=\"17\" title=\"Nummern ausblenden\" alt=\"Nummern ausblenden\" border=\"0\">");
    }
    /** sidebar
    if (jQuery("#numbers", window.parent.frames["right"].document).css("visibility") == "visible") {
        jQuery("#numbers", window.parent.frames["right"].document).css("visibility", "hidden");
    } else {
        jQuery("#numbers", window.parent.frames["right"].document).css("visibility", "visible");
    }*/
    //alert("changed-css attributes");
}

function deselectOptions (id){
   var i;
   var select = document.getElementById(id);
   for (i=0; i<select.options.length; i++) {
     select.options[i].selected = false;
   }
}