/*
 * UploadDataServlet.java
 *
 * Created on 19. November 2006, 22:14
 *
 */

package de.swers.kiezatlas.tools;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;

import de.deepamehta.BaseAssociation;
import de.deepamehta.BaseTopic;
import de.deepamehta.DeepaMehtaException;
import de.deepamehta.service.CorporateDirectives;
import de.deepamehta.service.Session;
import de.deepamehta.service.web.DeepaMehtaServlet;
import de.deepamehta.service.web.RequestParameter;
import de.deepamehta.topics.LiveTopic;
import de.kiezatlas.deepamehta.topics.CityMapTopic;
import de.swers.kiezatlas.tools.StadtinfoData;
import de.swers.kiezatlas.tools.XMLAddressFileReader;


/**
 *
 * @author Ramazan Sarikaya
 */
public class UploadDataServlet extends DeepaMehtaServlet implements de.kiezatlas.deepamehta.KiezAtlas {
    
  static final String PAGE_LOGIN = "UploadDataLogin";
  static final String PAGE_HOME = "UploadDataHome";
  static final String PAGE_LOGIN_TRY_AGAIN = "UploadDataLoginTryAgain";

  static final String ACTION_UPLOAD_DATA = "UploadDataAction";
  static final String ACTION_UPLOAD_DONE = "UploadDataDone";

  static final String PREFIX_NEUKOELLN =  "t-NKLSTI-";

  public void doPost(HttpServletRequest request, HttpServletResponse response)
                          throws IOException, ServletException {
      super.doPost(request, response);	// ###
  }
        
	protected String performAction(String action, RequestParameter params, Session session, CorporateDirectives directives) throws ServletException {
		if (action == null) {
			System.out.println("Hello from the external UploadServlet");
			return PAGE_LOGIN;
		} else if (action.equals(ACTION_TRY_LOGIN)) {
			//String username = params.getValue("username");
                        String username = "nk-kiez";
			String password = params.getValue("password");
			if (as.loginCheck(username, password)) {
				BaseTopic user = null;
				try {
					user = cm.getTopic(TOPICTYPE_USER, username, 1);
				} catch (Throwable e) {
					System.out.println(">>>@UserGetTopicAtUploadData" + e);
				} finally {
					setUser(user, session);
				}
				return PAGE_HOME;
			} else {
				return PAGE_LOGIN_TRY_AGAIN;
			}
		} else if (action.equals(ACTION_UPLOAD_DATA)) {
			String filepath= writeData(params.getUploads());
                        if (filepath!=null) {
                            doUpdate(filepath, session);
                        }
			return ACTION_UPLOAD_DONE;
			//
		}
		//
		return super.performAction(action, params, session, directives);
	}

	// **********************
	// *** Custom Methods ***
	// **********************


    private String writeData(Vector fileItems) {
		System.out.println(">>> UploadDataServlet.writeData(): " + fileItems.size() + " files uploaded");
		try {
			Enumeration e = fileItems.elements();
			if  (e.hasMoreElements()) {
				FileItem item = (FileItem) e.nextElement();
				// item.write(new File("/home/jrichter/deepamehta/install/client/images/" + filename));	// ###
				// ### item.write(new File(as.getCorporateWebBaseURL().substring(5) + "images/" + filename));
                                File f = java.io.File.createTempFile("stadtinfo",".xml");     // new File("stadtinfo.xml");
                                item.write(f);	// ###                           
                                return f.getAbsolutePath();
			}
		} catch (Exception e) {
			System.out.println("*** UploadDataServlet.writeData(): " + e);
		}
                
                return null;
	}
        
        
        private void removeInstitutions(String cityMapID) {
		CityMapTopic cityMap = (CityMapTopic) as.getLiveTopic(cityMapID, 1);
		String instTypeID = cityMap.getInstitutionType().getID();
		Vector insts = cm.getTopicIDs(instTypeID, cityMapID, true);		// sortByTopicName=true                        
                
                Enumeration em = insts.elements();
                String instID="";
                int n = insts.size();
                
                while (em.hasMoreElements()) {
                    instID = (String) em.nextElement();
                    Vector assocs = cm.getAssociationIDs(instID, 1); 
                    Enumeration ae = assocs.elements();
                    while (ae.hasMoreElements()) {
                        String aID = (String) ae.nextElement();
                        BaseAssociation ba = cm.getAssociation(aID,1);
                        if (ba != null) {
                            String tID1 = ba.getTopicID1();
                            if  (tID1.equalsIgnoreCase(instID)) {
                                String tID2 = ba.getTopicID2();
                                if (tID2.startsWith(PREFIX_NEUKOELLN)) {
                                    cm.deleteTopicData(tID2, 1);
                                    cm.deleteTopic(tID2);
                                    cm.deleteViewTopic(tID2);
                                }
                            }
                        }
                        cm.deleteAssociationData(aID,1);
                        cm.deleteAssociation(aID);
                        cm.deleteViewAssociation(aID);
                    }
                    cm.deleteTopicData(instID, 1);
                    cm.deleteTopic(instID);
                    cm.deleteViewTopic(instID);
                }
        }
        
        
	private java.awt.Point getPoint(CityMapTopic citymap, String propX,  String propY) throws DeepaMehtaException {
		// ### copied
		int x1, y1, x2, y2;
		float yadeX1, yadeY1, yadeX2, yadeY2;
		try {
			de.deepamehta.PresentableTopic[] yp = citymap.getYADEReferencePoints();	// throws DME
			if (yp == null) {
				// YADE is "off"
				return null;
			}
			x1 = yp[0].getGeometry().x;
			y1 = yp[0].getGeometry().y;
			x2 = yp[1].getGeometry().x;
			y2 = yp[1].getGeometry().y;
			yadeX1 = Float.parseFloat(as.getTopicProperty(yp[0], PROPERTY_YADE_X));
			yadeY1 = Float.parseFloat(as.getTopicProperty(yp[0], PROPERTY_YADE_Y));
			yadeX2 = Float.parseFloat(as.getTopicProperty(yp[1], PROPERTY_YADE_X));
			yadeY2 = Float.parseFloat(as.getTopicProperty(yp[1], PROPERTY_YADE_Y));
		} catch (NumberFormatException e) {
			//throw new DeepaMehtaException("Administrator-Fehler: ein YADE Referenzpunkt von " +
			//	"Stadtplan \"" + citymap.getName() + "\" hat ung�ltigen Wert (" + e.getMessage() + ")");
                        return null;
		}
		// yade -> pixel
		try {
			float yadeX = Float.parseFloat(propX);
			float yadeY = Float.parseFloat(propY);
			int x = (int) (x1 + (x2 - x1) * (yadeX - yadeX1) / (yadeX2 - yadeX1));
			int y = (int) (y2 + (y1 - y2) * (yadeY - yadeY2) / (yadeY1 - yadeY2));
			return new java.awt.Point(x, y);
		} catch (NumberFormatException e) {
			return null;
		}
	}

        
       private int topicIndex = 0;
       private int associationIndex = 0;            

        
        private String getTopicID(String baseID) {
            topicIndex++;
            return baseID+"-topic-"+topicIndex;
        }
        
        private String getAssociationID(String baseID) {
            associationIndex++;
            return baseID+"-assoc-"+associationIndex;
        }
        
        private void storeInstitution(String cityMapID, StadtinfoData stadtinfoData, Session session) {
		CityMapTopic cityMap = (CityMapTopic) as.getLiveTopic(cityMapID, 1);
		System.out.println("instTypeFetchedOverCityMapForImport::: " + cityMap.getInstitutionType().getID());
		String instTypeID = cityMap.getInstitutionType().getID();
            
                String Zeilennr= (stadtinfoData.getData("Zeilennr"));
                String Kategorie= (stadtinfoData.getData("Kategorie"));
                String KENNZAHL= (stadtinfoData.getData("KENNZAHL"));
                String TYP= (stadtinfoData.getData("TYP"));
                String NAME= (stadtinfoData.getData("NAME"));
                String NAME2= (stadtinfoData.getData("NAME2"));
                String STRASSE= (stadtinfoData.getData("STRASSE"));
                String HAUSNR= (stadtinfoData.getData("HAUSNR"));
                String PLZ= (stadtinfoData.getData("PLZ"));
                String TELEFON= (stadtinfoData.getData("TELEFON"));
                String TELEFON_2= (stadtinfoData.getData("TELEFON_2"));
                String LEITER_x002F_PAR= (stadtinfoData.getData("LEITER_x002F_PAR"));
                String TRAEGER= (stadtinfoData.getData("TRAEGER"));
                String GEBIET= (stadtinfoData.getData("GEBIET"));
                String GEOEFFNET= (stadtinfoData.getData("GEOEFFNET"));
                String FAX= (stadtinfoData.getData("FAX"));
                String E_Mail= (stadtinfoData.getData("E_Mail"));
                String Angebot= (stadtinfoData.getData("Angebot"));
                String Zielgruppe= (stadtinfoData.getData("Zielgruppe"));
                String Selbstdarstellung= (stadtinfoData.getData("Selbstdarstellung"));
                String Homepage= (stadtinfoData.getData("Homepage"));
                String xcoordinate= (stadtinfoData.getData("xcoordinate"));
                String ycoordinate= (stadtinfoData.getData("ycoordinate"));
                String BezReg= (stadtinfoData.getData("BezReg"));
                    
		//System.out.println("-------------------------------------");
                        
                //
		// Einrichtung
		String instName = NAME+","+NAME2;
		//System.out.println(">>> Name: " + instName);
		//
		String instID =  PREFIX_NEUKOELLN+Zeilennr;    // cm. getNewTopicID();
		LiveTopic inst = as.createLiveTopic(instID, instTypeID, instName, session);
		as.setTopicProperty(inst, PROPERTY_NAME, instName);
                as.setTopicProperty(inst, PROPERTY_YADE_X, xcoordinate);
                as.setTopicProperty(inst, PROPERTY_YADE_Y, ycoordinate);
                        
                java.awt.Point p = getPoint(cityMap, xcoordinate, ycoordinate);
                        
                if (p != null) {
                    as.createViewTopic(cityMapID, 1, null, instID, 1, p.x, p.y, false);	// ### version=1
                }
		//
		// Kategorien
		String categories = Kategorie+",alle";
		//System.out.println(">>> Kategorie: " + categories);
		StringTokenizer categoryTokens = new StringTokenizer(categories, ",");
		while (categoryTokens.hasMoreTokens()) {
                    String thisCategory = categoryTokens.nextToken();
                    String catID = getKategorieID(thisCategory);
                    if (catID != null) {
			//as.createAssociation(ASSOCTYPE_ASSOCIATION, instID, catID);
                        cm.createAssociation(getAssociationID(instID), 1, ASSOCTYPE_ASSOCIATION, 1, instID, 1, catID, 1);
                    }
		}
		//
		// Thema
		String thema = mapKennzahlThema(KENNZAHL)+",alle";
		//System.out.println(">>> Thema: " + thema);
		StringTokenizer themaTokens = new StringTokenizer(thema, ",");
		while (themaTokens.hasMoreTokens()) {
                    String thisThema = themaTokens.nextToken();
                    String themaID = getThemaID(thisThema);
                    if (themaID != null) {
			// as.createAssociation(ASSOCTYPE_ASSOCIATION, instID, themaID);
                        cm.createAssociation(getAssociationID(instID), 1, ASSOCTYPE_ASSOCIATION, 1, instID, 1, themaID, 1);
                    }
		}
		//
		// Tr�ger
		String traeger = mapKennzahlTraeger(KENNZAHL)+",alle";
		//System.out.println(">>> Tr�ger: " + traeger);
		StringTokenizer traegerTokens = new StringTokenizer(traeger, ",");
		while (traegerTokens.hasMoreTokens()) {
                    String thisTraeger = traegerTokens.nextToken();
                    String traegerID = getTraegerID(thisTraeger);
                    if (traegerID != null) {
			//as.createAssociation(ASSOCTYPE_ASSOCIATION, instID, traegerID);
                        cm.createAssociation(getAssociationID(instID), 1, ASSOCTYPE_ASSOCIATION, 1, instID, 1, traegerID, 1);
                    }
		}
		// Adresse
		Hashtable instAdressProps = new Hashtable();
		String instStreet = STRASSE;
		instAdressProps.put(PROPERTY_STREET, instStreet);
		String instPostalCode = PLZ;
		instAdressProps.put(PROPERTY_POSTAL_CODE, instPostalCode);
		String instAdressID = getTopicID(instID);
		LiveTopic instAdress = as.createLiveTopic(instAdressID, TOPICTYPE_ADDRESS, instStreet, session);
		as.setTopicProperties(instAdressID, 1, instAdressProps);
		// as.createAssociation(ASSOCTYPE_ASSOCIATION, instID, instAdressID);
                cm.createAssociation(getAssociationID(instID), 1, ASSOCTYPE_ASSOCIATION, 1, instID, 1, instAdressID, 1);
		// Telefon
		StringTokenizer phoneToken = new StringTokenizer(TELEFON, ";");
		while (phoneToken.hasMoreTokens()) {
                    String phoneNumber = phoneToken.nextToken();
                    if (phoneNumber.startsWith(" ")) {
                        phoneNumber = phoneNumber.substring(1);
                    }
                    String instFonID = getTopicID(instID);
                    LiveTopic thisInstFon = as.createLiveTopic(instFonID, TOPICTYPE_PHONE_NUMBER, phoneNumber, session);
                    as.setTopicProperty(thisInstFon, PROPERTY_NAME, phoneNumber);
                    //as.createAssociation(ASSOCTYPE_ASSOCIATION, instID, instFonID);
                    cm.createAssociation(getAssociationID(instID), 1, ASSOCTYPE_ASSOCIATION, 1, instID, 1, instFonID, 1);
		}
		// Fax
		String instFax = FAX;
		if (instFax.length() > 0) {
                    String instFaxID = getTopicID(instID);
                    LiveTopic thisInstFax = as.createLiveTopic(instFaxID, TOPICTYPE_FAX_NUMBER, instFax, session);
                    as.setTopicProperty(thisInstFax, PROPERTY_NAME, instFax);
                    //as.createAssociation(ASSOCTYPE_ASSOCIATION, instID, instFaxID);
                    cm.createAssociation(getAssociationID(instID), 1, ASSOCTYPE_ASSOCIATION, 1, instID, 1, instFaxID, 1);
		}
		// Website
                if (Homepage.length()>0) {
                    if (!Homepage.startsWith("http://")) {
                        Homepage="http://"+Homepage;
                    }
                }
		String instWebUrl = Homepage;
                if (instWebUrl.length() > 0) {
                    String instWebpageID = getTopicID(instID);
                    LiveTopic instWebpage = as.createLiveTopic(instWebpageID, TOPICTYPE_WEBPAGE, instWebUrl, session);
                    as.setTopicProperty(instWebpage, PROPERTY_URL, instWebUrl);
                    //as.createAssociation(ASSOCTYPE_ASSOCIATION, instID, instWebpageID);
                    cm.createAssociation(getAssociationID(instID), 1, ASSOCTYPE_ASSOCIATION, 1, instID, 1, instWebpageID, 1);
		}
                // Email
                StringTokenizer ste = new StringTokenizer(E_Mail, ",");
                while (ste.hasMoreTokens()) {
                    String emailAdr = ste.nextToken();
                    if (emailAdr.startsWith(" ")) {
                        emailAdr = emailAdr.substring(1);
                    }
                    String instMailID = getTopicID(instID);
                    LiveTopic instMailTopic = as.createLiveTopic(instMailID, TOPICTYPE_EMAIL_ADDRESS, emailAdr, session);
                    as.setTopicProperty(instMailTopic, PROPERTY_EMAIL_ADDRESS, emailAdr);
                    //as.createAssociation(ASSOCTYPE_ASSOCIATION, instID, instMailID);
                     cm.createAssociation(getAssociationID(instID), 1, ASSOCTYPE_ASSOCIATION, 1, instID, 1, instMailID, 1);
		}
		// Ansprechpartner
		importPersons(LEITER_x002F_PAR, instID, session);

                // Tr�ger
		String traegerID = getTopicID(instID);
		//System.out.println("--> anlegen (" + traegerID + ")");
		as.createLiveTopic(traegerID, TOPICTYPE_AGENCY, TRAEGER, session);
                Hashtable props = new Hashtable();
		props.put(PROPERTY_NAME, TRAEGER);
		as.setTopicProperties(traegerID, 1, props);
		//as.createAssociation(ASSOCTYPE_ASSOCIATION, instID, traegerID);
                cm.createAssociation(getAssociationID(instID), 1, ASSOCTYPE_ASSOCIATION, 1, instID, 1, traegerID, 1);

                Hashtable instProps = new Hashtable();
		// �ffnungszeiten
		String instOpeningHours = GEOEFFNET;
		if (instOpeningHours.length() > 0) {
			  instProps.put(PROPERTY_OEFFNUNGSZEITEN, instOpeningHours);
		}
                        
		// Sonstiges
		String instMisc = Selbstdarstellung;
		if (instMisc.length() > 0) {
			instProps.put(PROPERTY_SONSTIGES, instMisc);
		}
                instProps.put(PROPERTY_CITY, "Berlin");
                instProps.put(PROPERTY_WEB_ALIAS, instID);
                instProps.put(PROPERTY_PASSWORD, "neukoellnatlas");
		as.setTopicProperties(instID, 1, instProps);
        }
        
        
	private void importPersons(String persons, String instID, Session session) {
		if (persons.length() > 0) {
			StringTokenizer st = new StringTokenizer(persons, ",");
			while (st.hasMoreTokens()) {
				String person = st.nextToken();
				if (person.startsWith(" ")) {
					person = person.substring(1);
				}
				//System.out.print(">>> Ansprechpartner: \"" + person + "\" ");
				Hashtable props = new Hashtable();
				// fill name prop
				String name;
				int pos = person.indexOf(' ');
				if (pos == -1) {
					props.put(PROPERTY_NAME, person);	// last name only
					name = person;
				} else {
					String firstName = person.substring(0, pos);
					String lastName = person.substring(pos + 1);
					props.put(PROPERTY_NAME, lastName);
					props.put(PROPERTY_FIRST_NAME, firstName);
					name = firstName + " " + lastName;
				}
				// add only if not exist already
				String personID;
				personID = getTopicID(instID);
				//System.out.println("--> anlegen (" + personID + ")");
				as.createLiveTopic(personID, TOPICTYPE_PERSON, name, session);
				as.setTopicProperties(personID, 1, props);
				//
				//as.createAssociation(ASSOCTYPE_ASSOCIATION, instID, personID);
                                cm.createAssociation(getAssociationID(instID), 1, ASSOCTYPE_ASSOCIATION, 1, instID, 1, personID, 1);
			}
		}
	}
        
        
        private String getCityMapID(String BezReg) {
            if (BezReg.equalsIgnoreCase("01")) return "t-181771";       // Schillerpromenade
            if (BezReg.equalsIgnoreCase("02"))  return "t-181770";      // Neuk�llner-Mitte
            if (BezReg.equalsIgnoreCase("03"))  return "t-181724";      // Reuter-kiez
            if (BezReg.equalsIgnoreCase("04"))  return "t-181769";      // Rixdorf
            if (BezReg.equalsIgnoreCase("05"))  return "t-181767";      // K�llnische Heide
            if (BezReg.equalsIgnoreCase("06"))  return "t-181766";      // Britz
            if (BezReg.equalsIgnoreCase("07"))  return "t-181759";      // Buckow
            if (BezReg.equalsIgnoreCase("08"))  return "t-181762";      // Gropiusstadt
            if (BezReg.equalsIgnoreCase("09"))  return "t-181765";      // Buckow-Nord
            if (BezReg.equalsIgnoreCase("10"))  return "t-181772";      // Rudow

            return null;
        }
        
        
        private String getKategorieID(String kategorie) {
            if (kategorie.equalsIgnoreCase("alle")) return "t-181739";
            if (kategorie.equalsIgnoreCase("Freizeit"))  return "t-181731";
            if (kategorie.equalsIgnoreCase("Koord./Geschäftsst."))  return "t-181734";
            if (kategorie.equalsIgnoreCase("Hilfen"))  return "t-181732";
            if (kategorie.equalsIgnoreCase("Kieztreff"))  return "t-181733";
            if (kategorie.equalsIgnoreCase("Politik"))  return "t-181735";
            if (kategorie.equalsIgnoreCase("Schule"))  return "t-181736";
            if (kategorie.equalsIgnoreCase("Sonstiges"))  return "t-181738";
            if (kategorie.equalsIgnoreCase("Tagesbetreuung"))  return "t-181737";

            return null;
        }
        
        
        private String mapKennzahlThema(String kennzahl) {
            try {
                int l = kennzahl.length();
                if (l>0) {
                    int k = (new Integer(kennzahl.substring(0,1))).intValue();
                
                    switch (k) {
                        case 1: return "Kinder";
                        case 2: return "Jugendliche";
                        case 3: return "Familie"; 
                        case 4: return "Sport";
                        case 5: return "Gemeinwesen";
                        case 6: return "Schule";
                        case 7: return "Verwaltung";
                    }
                }
            }
            catch (java.lang.NumberFormatException nfe) {
                return "";
            }
            return "";
        }

        
        private String getThemaID(String thema) {
            if (thema.equalsIgnoreCase("alle")) return "t-181748";
            if (thema.equalsIgnoreCase("Familie"))  return "t-181743";
            if (thema.equalsIgnoreCase("Gemeinwesen"))  return "t-181745";
            if (thema.equalsIgnoreCase("Jugendliche"))  return "t-181742";
            if (thema.equalsIgnoreCase("Kinder"))  return "t-181741";
            if (thema.equalsIgnoreCase("Schule"))  return "t-181746";
            if (thema.equalsIgnoreCase("Sport"))  return "t-181744";
            if (thema.equalsIgnoreCase("Verwaltung"))  return "t-181747";

            return null;
        }
        
        
        private String mapKennzahlTraeger(String kennzahl) {
            int k;
            
            try {
                k = (new Integer(kennzahl)).intValue();
            
                switch (k) {
                    case 710:
                    case 720:
                    case 780: 
                        return "städtische";
                    case 730:
                    case 740:
                    case 750:
                    case 770:
                    case 790:
                        return "diverse";
                    case 760:
                        return "sonstige";
                }
            
                int l = kennzahl.length();
                if (l>0) {
                    k = (new Integer(kennzahl.substring(l-1))).intValue();
                
                    switch (k) {
                        case 1: return "städtische";
                        case 2: return "diverse";
                        case 3: return "evang."; 
                        case 4: return "kath.";
                        case 5: return "AWO";
                        case 6: return "DRK";
                        case 7: return "dpw";
                        case 8: 
                        case 9: return "sonstige";
                    }
                }
            }
            catch (java.lang.NumberFormatException nfe) {
                return "";
            }
            
            return "";
        }

        
        private String getTraegerID(String traeger) {
            if (traeger.equalsIgnoreCase("alle")) return "t-181758";
            if (traeger.equalsIgnoreCase("AWO"))  return "t-181754";
            if (traeger.equalsIgnoreCase("diverse"))  return "t-181751";
            if (traeger.equalsIgnoreCase("dpw"))  return "t-181756";
            if (traeger.equalsIgnoreCase("DRK"))  return "t-181755";
            if (traeger.equalsIgnoreCase("evang."))  return "t-181752";
            if (traeger.equalsIgnoreCase("kath."))  return "t-181753";
            if (traeger.equalsIgnoreCase("sonstige"))  return "t-181757";
            if (traeger.equalsIgnoreCase("städtische"))  return "t-181750";

            return null;
        }

        
        private void doUpdate(String filepath, Session session) {
            if (filepath==null) return;
            try {
                de.swers.kiezatlas.tools.XMLAddressFileReader xmlAddressFileReader = new XMLAddressFileReader(filepath);
                StadtinfoData[] stadtinfoData = xmlAddressFileReader.getXMLAddressHandler().stadtinfoData;
                int noOfItems = xmlAddressFileReader.getXMLAddressHandler().getNoOfItems();
                
                if (noOfItems>0) {

                    removeInstitutions(this.getCityMapID("01"));
                    removeInstitutions(this.getCityMapID("02"));
                    removeInstitutions(this.getCityMapID("03"));
                    removeInstitutions(this.getCityMapID("04"));
                    removeInstitutions(this.getCityMapID("05"));
                    removeInstitutions(this.getCityMapID("06"));
                    removeInstitutions(this.getCityMapID("07"));
                    removeInstitutions(this.getCityMapID("08"));
                    removeInstitutions(this.getCityMapID("09"));
                    removeInstitutions(this.getCityMapID("10"));
                
                    for (int i=0; i<noOfItems; i++){
                        
                        String BezReg= (stadtinfoData[i].getData("BezReg"));

                        String cityMapID = getCityMapID(BezReg);
                        
                        if (cityMapID != null) {
                            storeInstitution(cityMapID, stadtinfoData[i], session);
                        }                        
                    }
                }
            } catch (Exception ex) {
                System.out.println("Exception");
                ex.printStackTrace();
            }
        }




	// *************************
	// *** Session Utilities ***
	// *************************



	// --- Methods to maintain data in the session

	private void setUser(BaseTopic user, Session session) {
		session.setAttribute("user", user);
		//System.out.println("> \"user\" stored in session: " + user);
	}

	private boolean isSessionActive(Session session) {
                if (session==null) return false;
		return true;
	}

}
