package de.kiezatlas.deepamehta.topics;

import de.kiezatlas.deepamehta.KiezAtlas;
//
import de.deepamehta.BaseTopic;
import de.deepamehta.DeepaMehtaException;
import de.deepamehta.TopicInitException;
import de.deepamehta.topics.FileTopic;
import de.deepamehta.topics.LiveTopic;
import de.deepamehta.PropertyDefinition;
import de.deepamehta.service.Session;
import de.deepamehta.service.CorporateDirectives;
import de.deepamehta.service.CorporateMemory;
import de.deepamehta.service.ApplicationService;
import de.deepamehta.service.CorporateCommands;
//
import java.awt.Point;
import java.io.*;
import java.util.*;



/**
 * Kiezatlas 1.2.1.<br>
 * Requires DeepaMehta 2.0b4.
 * <p>
 * Last functional change: 1.1.2005<br>
 * J&ouml;rg Richter<br>
 * jri@freenet.de
 */
public class FileImportTopic extends FileTopic implements KiezAtlas {



	static final String TOPICMAP_ID = "t-ka-schoeneberg";	// the imported topics are placed in this map
	static final String GEOMETRY_FILE = "geometry.txt";		// this file is expected on the server

	static final String ITEM_IMPORT_FILE = "Start Import";
	static final String CMD_IMPORT_FILE = "importFile";
	static final String BUTTON_ASSIGN_FILE = "assign file";		// ###
	static final String CMD_ASSIGN_FILE = "assignFile";			// ###

	Hashtable catTable = new Hashtable();
	Hashtable ageTable = new Hashtable();
	Hashtable offerTable = new Hashtable();
	//
	Hashtable codeTable = new Hashtable();
	Hashtable geometryTable = new Hashtable();



	// *******************
	// *** Constructor ***
	// *******************



	public FileImportTopic(BaseTopic topic, ApplicationService as) {
		super(topic, as);
	}



	// **********************
	// *** Defining Hooks ***
	// **********************



	// ------------------
	// --- Life Cycle ---
	// ------------------



	public CorporateDirectives init(int initLevel, Session session) {
		CorporateDirectives directives = super.init(initLevel, session);
		//
		if (initLevel == INITLEVEL_1) {
			initMappingTables();
			initGeometryTables();
		}
		//
		return directives;
	}



	// --------------------------
	// --- Providing Commands ---
	// --------------------------



	/**
	 * Adds context menu items for importing TAB-separated files.
	 *
	 * @see		de.deepamehta.service.ApplicationService#getTopicCommands
	 */
	public CorporateCommands contextCommands(String topicmapID, String viewmode,
								Session session, CorporateDirectives directives) {
		CorporateCommands commands = new CorporateCommands(as);
		// --- import ---
		commands.addCommand(ITEM_IMPORT_FILE, CMD_IMPORT_FILE, FILESERVER_IMAGES_PATH, "import.gif");
		// --- generic topic commands ---
		commands.add(super.contextCommands(topicmapID, viewmode, session, directives));
		//
		return commands;
	}

	public static void buttonCommand(PropertyDefinition propDef, ApplicationService as, Session session) {
		String propName = propDef.getPropertyName();
		if (propName.equals(PROPERTY_FILE)) {
			propDef.setActionButton(BUTTON_ASSIGN_FILE, CMD_ASSIGN_FILE);
		}
	}



	// --------------------------
	// --- Executing Commands ---
	// --------------------------



	public CorporateDirectives executeCommand(String command, Session session,
			     				String topicmapID, String viewmode) {
		CorporateDirectives directives = new CorporateDirectives();
		//
		StringTokenizer st = new StringTokenizer(command, COMMAND_SEPARATOR);
		String cmd = st.nextToken();
		if (cmd.equals(CMD_IMPORT_FILE)) {
			// Hier wir der Import angestossen
			File importedFile = new File(FILESERVER_DOCUMENTS_PATH + getProperty(PROPERTY_FILE));
			System.out.println(">>> Das zu importierende Dokument ist " + getProperty(PROPERTY_FILE));
			try {
				// Note: the import file is expected to be latin-1 encoded
				BufferedReader importReader = new BufferedReader(
					new InputStreamReader(new FileInputStream(importedFile), "ISO-8859-1"));
				// the first line contains the column names
				String colNames = importReader.readLine();
				System.out.println(">>> Die Namen der Spalten lauten " + colNames);
				while (importReader.ready()) {
					String[] tk = split(importReader.readLine());
					System.out.println("-------------------------------------");
					// ### String str = tk[2];		// password
					String nr = tk[3];
					Point p = getGeometry(nr);
					//
					// Einrichtung
					String instName = tk[4];
					System.out.println(">>> Name: " + instName);
					//
					String instID = cm.getNewTopicID();
					LiveTopic inst = as.createLiveTopic(instID, TOPICTYPE_KIEZ_GEO, instName, session);
					as.setTopicProperty(inst, PROPERTY_NAME, instName);
					as.createViewTopic(TOPICMAP_ID, 1, null, instID, 1, p.x, p.y, false);	// ### version=1
					//
					// Kategorien
					String categories = tk[1];
					System.out.println(">>> Kategorie: " + categories);
					StringTokenizer categoryTokens = new StringTokenizer(categories, ",");
					while (categoryTokens.hasMoreTokens()) {
						String thisCategory = categoryTokens.nextToken();
						String catID = mapCategory(thisCategory);
						if (catID != null) {
							as.createAssociation(ASSOCTYPE_ASSOCIATION, instID, catID);
						}
					}
					// Adresse
					Hashtable instAdressProps = new Hashtable();
					String instStreet = tk[5];
					instAdressProps.put(PROPERTY_STREET, instStreet);
					String instPostalCode = tk[6];
					instAdressProps.put(PROPERTY_POSTAL_CODE, instPostalCode);
					String instAdressID = cm.getNewTopicID();
					LiveTopic instAdress = as.createLiveTopic(instAdressID, TOPICTYPE_ADDRESS, instStreet, session);
					as.setTopicProperties(instAdressID, 1, instAdressProps);
					as.createAssociation(ASSOCTYPE_ASSOCIATION, instID, instAdressID);
					// Telefon
					StringTokenizer phoneToken = new StringTokenizer(tk[7], ";");
					while (phoneToken.hasMoreTokens()) {
						String phoneNumber = phoneToken.nextToken();
						if (phoneNumber.startsWith(" ")) {
							phoneNumber = phoneNumber.substring(1);
						}
						String instFonID = cm.getNewTopicID();
						LiveTopic thisInstFon = as.createLiveTopic(instFonID, TOPICTYPE_PHONE_NUMBER, phoneNumber, session);
						as.setTopicProperty(thisInstFon, PROPERTY_NAME, phoneNumber);
						as.createAssociation(ASSOCTYPE_ASSOCIATION, instID, instFonID);
					}
					// Fax
					String instFax = tk[8];
					if (instFax.length() > 0) {
						String instFaxID = cm.getNewTopicID();
						LiveTopic thisInstFax = as.createLiveTopic(instFaxID, TOPICTYPE_FAX_NUMBER, instFax, session);
						as.setTopicProperty(thisInstFax, PROPERTY_NAME, instFax);
						as.createAssociation(ASSOCTYPE_ASSOCIATION, instID, instFaxID);
					}
					// Website
					String instWebUrl = tk[9];
					if (instWebUrl.length() > 0) {
						String instWebpageID = cm.getNewTopicID();
						LiveTopic instWebpage = as.createLiveTopic(instWebpageID, TOPICTYPE_WEBPAGE, instWebUrl, session);
						as.setTopicProperty(instWebpage, PROPERTY_URL, instWebUrl);
						as.createAssociation(ASSOCTYPE_ASSOCIATION, instID, instWebpageID);
					}
					// Email
					StringTokenizer ste = new StringTokenizer(tk[10], ",");
					while (ste.hasMoreTokens()) {
						String emailAdr = ste.nextToken();
						if (emailAdr.startsWith(" ")) {
							emailAdr = emailAdr.substring(1);
						}
						String instMailID = cm.getNewTopicID();
						LiveTopic instMailTopic = as.createLiveTopic(instMailID, TOPICTYPE_EMAIL_ADDRESS, emailAdr, session);
						as.setTopicProperty(instMailTopic, PROPERTY_EMAIL_ADDRESS, emailAdr);
						as.createAssociation(ASSOCTYPE_ASSOCIATION, instID, instMailID);
					}
					// Ansprechpartner
					importPersons(tk[11], instID, session);
					// Tr‰gername, Tr‰gerart
					importTraeger(tk[13], tk[12], instID, session);
					//
					Hashtable instProps = new Hashtable();
					// ÷ffnungszeiten
					String instOpeningHours = tk[14];
					if (instOpeningHours.length() > 0) {
						  instProps.put(PROPERTY_OEFFNUNGSZEITEN, instOpeningHours);
					}
					// Altersgruppen
					String instAgeGroups = tk[15];
					System.out.println(">>> Altersgruppen: " + instAgeGroups);
					StringTokenizer ageGroupTokens = new StringTokenizer(instAgeGroups, ":");
					while (ageGroupTokens.hasMoreTokens()) {
						String thisAgeGroup = ageGroupTokens.nextToken();
						String ageGroupID = mapAgeGroup(thisAgeGroup);
						if (ageGroupID != null) {
							as.createAssociation(ASSOCTYPE_ASSOCIATION, instID, ageGroupID);
						}
					}
					// Angebote
					String instOfferList = tk[16];
					System.out.println(">>> Angebote: " + instOfferList);
					StringTokenizer offerTokens = new StringTokenizer(instOfferList, ":");
					while (offerTokens.hasMoreTokens()) {
						String thisOffer = offerTokens.nextToken();
						String offerID = mapOffers(thisOffer);
						if (offerID != null) {
							as.createAssociation(ASSOCTYPE_ASSOCIATION, instID, offerID);
						}
					}
					// Sonstiges
					String instMisc = tk[17];
					if (instMisc.length() > 0) {
						instProps.put(PROPERTY_SONSTIGES, instMisc);
					}
					as.setTopicProperties(instID, 1, instProps);
					// Stadtplan Link
					/* ### String instCityMapUrl = tk[18];
					if (instCityMapUrl.length() > 0) {
						String instCityMapID = cm.getNewTopicID();
						LiveTopic instCityMap = as.createLiveTopic(instCityMapID, TOPICTYPE_WEBPAGE, instCityMapUrl, session);
						as.setTopicProperty(instCityMap, PROPERTY_URL, instCityMapUrl);
						as.createAssociation(ASSOCTYPE_ASSOCIATION, instID, instCityMapID);
					} */
				}
			} catch (IOException e) {
				System.out.println("*** Fehler beim Importieren: " + e);
			}
	    } else {
			return super.executeCommand(command, session, topicmapID, viewmode);
		}
		//
		return directives;
	}

	public CorporateDirectives executeChainedCommand(String command,
								String result, String topicmapID, String viewmode,
								Session session) {
		StringTokenizer st = new StringTokenizer(command, COMMAND_SEPARATOR);
		String cmd = st.nextToken();
		if (cmd.equals(CMD_ASSIGN_FILE)) {	// ### to be dropped
			CorporateDirectives directives = new CorporateDirectives();
			// Note: the result of a DIRECTIVE_CHOOSE_FILE contains the absolute
			// path of the (client side) selected file
			copyAndUpload(result, FILE_DOCUMENT, PROPERTY_FILE, session, directives);
			return directives;
		} else {
			return super.executeChainedCommand(command, result, topicmapID, viewmode, session);
		}
	}



	// ***********************
	// *** Private Methods ***
	// ***********************



	private void importPersons(String persons, String instID, Session session) {
		if (persons.length() > 0) {
			StringTokenizer st = new StringTokenizer(persons, ",");
			while (st.hasMoreTokens()) {
				String person = st.nextToken();
				if (person.startsWith(" ")) {
					person = person.substring(1);
				}
				System.out.print(">>> Ansprechpartner: \"" + person + "\" ");
				Hashtable props = new Hashtable();
				// fill gender prop
				if (person.startsWith("Fr. ")) {
					props.put(PROPERTY_GENDER, GENDER_FEMALE);
					person = person.substring(4);
				} else if (person.startsWith("Hr. ")) {
					props.put(PROPERTY_GENDER, GENDER_MALE);
					person = person.substring(4);
				}
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
				BaseTopic p = cm.getTopic(TOPICTYPE_PERSON, name, 1);
				if (p != null) {
					personID = p.getID();
					System.out.println("--> schon da (" + personID + ")");
				} else {
					personID = cm.getNewTopicID();
					System.out.println("--> anlegen (" + personID + ")");
					as.createLiveTopic(personID, TOPICTYPE_PERSON, name, session);
					as.setTopicProperties(personID, 1, props);
				}
				//
				as.createAssociation(ASSOCTYPE_ASSOCIATION, instID, personID);
			}
		}
	}

	private void importTraeger(String traegerStr, String traegerArt, String instID, Session session) {
		String art = null;
		if (traegerArt.length() > 0) {
			if (traegerArt.startsWith(":ko:")) {
				art = AGENCY_KIND_KOMMUNAL;
			} else if (traegerArt.startsWith(":fr:")) {
				art = AGENCY_KIND_FREI;
			}
		}
		//
		StringTokenizer st = new StringTokenizer(traegerStr, ";");
		// Note: the traeger kind is ignored if there are more than one traeger
		if (st.countTokens() > 1) {
			art = null;
		}
		//
		if (art != null || traegerStr.length() > 0) {
			do {
				String traeger = st.hasMoreTokens() ? st.nextToken() : "";
				if (traeger.startsWith(" ")) {
					traeger = traeger.substring(1);
				}
				Hashtable props = new Hashtable();
				props.put(PROPERTY_NAME, traeger);
				//
				if (art != null) {
					System.out.print(">>> Träger: \"" + traeger + "\" (" + art + ") ");
					props.put(PROPERTY_AGENCY_KIND, art);
				} else {
					System.out.print(">>> Träger: \"" + traeger + "\" (Trägerart nicht bekannt) ");
				}
				// add only if not exist already
				String traegerID = null;
				if (traeger.length() > 0) {
					BaseTopic p = cm.getTopic(TOPICTYPE_AGENCY, traeger, 1);
					if (p != null) {
						traegerID = p.getID();
						System.out.println("--> schon da (" + traegerID + ")");
					}
				}
				if (traegerID == null) {
					traegerID = cm.getNewTopicID();
					System.out.println("--> anlegen (" + traegerID + ")");
					as.createLiveTopic(traegerID, TOPICTYPE_AGENCY, traeger, session);
					as.setTopicProperties(traegerID, 1, props);
				}
				//
				as.createAssociation(ASSOCTYPE_ASSOCIATION, instID, traegerID);
			} while (st.hasMoreTokens());
		}
	}

	// ---

	private void initMappingTables() {
		// Einrichtungskategorien -- Note: only 12 of 14 categories are mapped
		catTable.put("A","t-ka-kat10"); //Kultur + Bildung"); //+ Bildung
		catTable.put("B","t-ka-kat5"); //"Ausbildung + Arbeit"); //- Selbstst&auml;ndigkeit");
		catTable.put("C","t-ka-kat15");//"Sport"); //-Spiel und Sport");
		catTable.put("D","t-ka-kat11");//"Nachbarschaft + Stadtteil"); //Freizeit und Nachbarschaft"); //-
		catTable.put("E","t-ka-kat4");// Senioren"
		catTable.put("F","t-ka-kat7");//"Gesundheit + Behinderung");
		catTable.put("H","t-ka-kat10");//"Kunst und Kultur");//-
		catTable.put("I","t-ka-kat16");//"Wohnung + Unterkunft");
		catTable.put("J","t-ka-kat6");// "Beratung"); //Beratung und Psyche");//--
		catTable.put("K","t-ka-kat13");//"NOT");// Akute Notlagen
		catTable.put("L","t-ka-kat9");//"Kinderbetreuung"); //--
		catTable.put("N","t-ka-kat12");//"Netzwerk"); //-e, Vereine,Verb&auml;nde");
		// Altersgruppen
		ageTable.put("1","t-ka-alt1"); //Familien/Alleinerziehende mit S‰uglingen');
		ageTable.put("2","t-ka-alt2"); //Familien/Alleinerziehende mit Kleinkindern');
		ageTable.put("3","t-ka-alt3"); //Familien/Alleinerziehende mit Kindern im Schulalter');
		ageTable.put("4","t-ka-alt4"); //Kinder im Schulalter');
		ageTable.put("5","t-ka-alt5"); //Jugendliche/Selbstorganisierte');
		ageTable.put("6","t-ka-alt6"); //'Jugendgruppen');
		ageTable.put("7","t-ka-alt7"); //Auszubildende & junge Erwachsene');
		ageTable.put("8","t-ka-alt8"); //Erwachsene');
		ageTable.put("11","t-ka-alt9"); //Seniorinnen und Senioren');
		ageTable.put("12","t-ka-alt10"); //Alle Altersgruppen');
		ageTable.put("13","t-ka-alt11"); //Sonstige');
		// Angebotsarten
		offerTable.put("Ber","t-ka-ang1"); //Beratung');
		offerTable.put("Betr","t-ka-ang2"); //Betreuung');
		offerTable.put("Ess","t-ka-ang3"); //Essen');
		offerTable.put("Kon","t-ka-ang4"); //Kontakte');
		offerTable.put("MC","t-ka-ang5"); //Medien / Computer');
		offerTable.put("QB","t-ka-ang6"); //Qualifizierung / Aus- /Weiterbildung');
		offerTable.put("RF","t-ka-ang7"); //R‰ume / Freiraumnutzung');
		offerTable.put("Sp","t-ka-ang8"); //Spielzeug / Sportger‰te / Spiel- /Sportplatz');
		offerTable.put("TGW","t-ka-ang9"); //Technische Ger‰te / Werkzeuge / Werkst‰tten');
		offerTable.put("Tra","t-ka-ang10"); //Transportmˆglichkeiten');
		offerTable.put("Unt","t-ka-ang11"); //Unterhaltung');
		offerTable.put("VN","t-ka-ang12"); //Vermittlung von Jobs / Nachbarschaftshilfe');
		offerTable.put("Son","t-ka-ang13"); //Sonstige');
	}

	private void initGeometryTables() {
		File geometryFile = new File(FILESERVER_DOCUMENTS_PATH + GEOMETRY_FILE);
		try {
			BufferedReader in = new BufferedReader(new FileReader(geometryFile));
			String line;
			int count = 0;
			while (!(line = in.readLine()).equals("")) {
				int pos = line.indexOf('=');
				String code = line.substring(0, pos);
				String nr = line.substring(pos + 1);
				if (codeTable.put(nr, code) != null) {
					System.out.println("*** FileImportTopic.initGeometryTables(): nr \"" + nr + "\" is not unique");
					// ### throw new TopicInitException("nr \"" + nr + "\" is not unique");
				}
				count++;
			} while (line.length() > 0);
			System.out.println(">>> " + count + " codes read");
			count = 0;
			while (in.ready()) {
				line = in.readLine();
				int pos1 = line.indexOf('=');
				int pos2 = line.indexOf(',');
				String code = line.substring(0, pos1);
				int x = Integer.parseInt(line.substring(pos1 + 1, pos2));
				int y = Integer.parseInt(line.substring(pos2 + 1));
				if (geometryTable.put(code, new Point(x, y)) != null) {
					throw new TopicInitException("code is not unique");
				}
				count++;
			}
			System.out.println(">>> " + count + " coords read");
		} catch (IOException e) {
			throw new TopicInitException("Fehler beim Lesen der Geometriedaten (" + e + ")");
		}
	}

	// ---

	private String mapCategory(String mapkey) {
		return (String) catTable.get(mapkey);
	}
	
	private String mapAgeGroup(String agekey) {
		return (String) ageTable.get(agekey);
	}
	
	private String mapOffers(String offersKey) {
		return (String) offerTable.get(offersKey);
	}

	// ---

	private Point getGeometry(String nr) {
		String code = (String) codeTable.get(nr);
		if (code == null) {
			throw new DeepaMehtaException("nr \"" + nr + "\" not found in code table");
		}
		Point p = (Point) geometryTable.get(code);
		if (p == null) {
			throw new DeepaMehtaException("code \"" + code + "\" not found in geometry table");
		}
		return p;
	}

	// ---
	
	String[] split(String str) {
		return str.split("\t", 20);		// limit=20
	}
}
