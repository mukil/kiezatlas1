-----------------
--- Workspace ---
-----------------



---
--- Workspace "Kiez-Atlas"
---
INSERT INTO Topic VALUES ('tt-workspace', 1, 1, 't-ka-workspace', 'Kiez-Atlas');
INSERT INTO TopicProp VALUES ('t-ka-workspace', 1, 'Name', 'Kiez-Atlas');
INSERT INTO TopicProp VALUES ('t-ka-workspace', 1, 'Public', 'on');
INSERT INTO TopicProp VALUES ('t-ka-workspace', 1, 'Default', 'off');
-- workspace topicmap
INSERT INTO Topic VALUES ('tt-topicmap', 1, 1, 't-ka-workmap', 'Kiez-Atlas');
INSERT INTO TopicProp VALUES ('t-ka-workmap', 1, 'Name', 'Kiez-Atlas');
INSERT INTO Association VALUES ('at-aggregation', 1, 1, 'a-ka-1', '', 't-ka-workspace', 1, 't-ka-workmap', 1);
-- chat
INSERT INTO Topic VALUES ('tt-chatboard', 1, 1, 't-ka-chat', 'Kiez-Atlas Chats');
INSERT INTO TopicProp VALUES ('t-ka-chat', 1, 'Name', 'Kiez-Atlas Chats');
INSERT INTO ViewTopic VALUES ('t-ka-workmap', 1, 't-ka-chat', 1, 500, 50);
-- forum
INSERT INTO Topic VALUES ('tt-messageboard', 1, 1, 't-ka-forum', 'Kiez-Atlas Forum');
INSERT INTO TopicProp VALUES ('t-ka-forum', 1, 'Name', 'Kiez-Atlas Forum');
INSERT INTO ViewTopic VALUES ('t-ka-workmap', 1, 't-ka-forum', 1, 520, 100);
-- assign types to workspace
INSERT INTO Association VALUES ('at-uses', 1, 1, 'a-ka-2', '', 't-ka-workspace', 1, 'tt-ka-stadtplan', 1);
INSERT INTO Association VALUES ('at-uses', 1, 1, 'a-ka-3', '', 't-ka-workspace', 1, 'tt-ka-einrichtung', 1);
INSERT INTO Association VALUES ('at-uses', 1, 1, 'a-ka-4', '', 't-ka-workspace', 1, 'tt-ka-traeger', 1);
INSERT INTO Association VALUES ('at-uses', 1, 1, 'a-ka-24', '', 't-ka-workspace', 1, 'tt-ka-kategorie', 1);
INSERT INTO Association VALUES ('at-uses', 1, 1, 'a-ka-30', '', 't-ka-workspace', 1, 'tt-ka-angebot', 1);
INSERT INTO Association VALUES ('at-uses', 1, 1, 'a-ka-31', '', 't-ka-workspace', 1, 'tt-ka-altersgruppe', 1);
INSERT INTO AssociationProp VALUES ('a-ka-2', 1, 'Access Permission', 'create in workspace');
INSERT INTO AssociationProp VALUES ('a-ka-3', 1, 'Access Permission', 'create');
INSERT INTO AssociationProp VALUES ('a-ka-4', 1, 'Access Permission', 'create');
INSERT INTO AssociationProp VALUES ('a-ka-24', 1, 'Access Permission', 'create');
INSERT INTO AssociationProp VALUES ('a-ka-30', 1, 'Access Permission', 'create');
INSERT INTO AssociationProp VALUES ('a-ka-31', 1, 'Access Permission', 'create');
-- assign type to user
INSERT INTO Association VALUES ('at-uses', 1, 1, 'a-ka-40', '', 't-rootuser', 1, 'tt-ka-fileimport', 1);
INSERT INTO AssociationProp VALUES ('a-ka-40', 1, 'Access Permission', 'create');



-------------------
--- Topic Types ---
-------------------



--- "Stadtplan" ---
INSERT INTO Topic VALUES ('tt-topictype', 1, 1, 'tt-ka-stadtplan', 'Stadtplan');
INSERT INTO TopicProp VALUES ('tt-ka-stadtplan', 1, 'Name', 'Stadtplan');
INSERT INTO TopicProp VALUES ('tt-ka-stadtplan', 1, 'Plural Name', 'Stadtpläne');
INSERT INTO TopicProp VALUES ('tt-ka-stadtplan', 1, 'Description', '<html><body><p>Ein <i>Stadtplan</i> ...</p></body></html>');
INSERT INTO TopicProp VALUES ('tt-ka-stadtplan', 1, 'Description Query', 'Was ist ein Stadtplan?');
-- INSERT INTO TopicProp VALUES ('tt-ka-stadtplan', 1, 'Icon', 'Kompetenzstern.gif');
-- INSERT INTO TopicProp VALUES ('tt-ka-stadtplan', 1, 'Creation Icon', 'createTopicmap.gif');
INSERT INTO TopicProp VALUES ('tt-ka-stadtplan', 1, 'Unique Topic Names', 'on');
INSERT INTO TopicProp VALUES ('tt-ka-stadtplan', 1, 'Custom Implementation', 'de.kiezatlas.deepamehta.topics.CityMapTopic');
-- assign properties
INSERT INTO Association VALUES ('at-composition', 1, 1, 'a-ka-17', '', 'tt-ka-stadtplan', 1, 'pp-webalias', 1);
INSERT INTO AssociationProp VALUES ('a-ka-17', 1, 'Ordinal Number', '350');
-- super type
INSERT INTO Association VALUES ('at-derivation', 1, 1, 'a-ka-5', '', 'tt-topicmap', 1, 'tt-ka-stadtplan', 1);
-- container type
INSERT INTO Topic VALUES ('tt-topictype', 1, 1, 'tt-ka-stadtplansuche', 'Stadtplan-Suche');
INSERT INTO TopicProp VALUES ('tt-ka-stadtplansuche', 1, 'Name', 'Stadtplan-Suche');
-- INSERT INTO TopicProp VALUES ('tt-ka-stadtplansuche', 1, 'Icon', 'KompetenzsternContainer.gif');
-- derive container type
INSERT INTO Association VALUES ('at-derivation', 1, 1, 'a-ka-6', '', 'tt-topiccontainer', 1, 'tt-ka-stadtplansuche', 1);
-- assign type to container type
INSERT INTO Association VALUES ('at-aggregation', 1, 1, 'a-ka-7', '', 'tt-ka-stadtplansuche', 1, 'tt-ka-stadtplan', 1);

--- "Einrichtung" ---
INSERT INTO Topic VALUES ('tt-topictype', 1, 1, 'tt-ka-einrichtung', 'Einrichtung');
INSERT INTO TopicProp VALUES ('tt-ka-einrichtung', 1, 'Name', 'Einrichtung');
INSERT INTO TopicProp VALUES ('tt-ka-einrichtung', 1, 'Plural Name', 'Einrichtungen');
INSERT INTO TopicProp VALUES ('tt-ka-einrichtung', 1, 'Description', '<html><body><p>Eine <i>Einrichtung</i> ist ...</p></body></html>');
INSERT INTO TopicProp VALUES ('tt-ka-einrichtung', 1, 'Description Query', 'Was ist eine Einrichtung?');
INSERT INTO TopicProp VALUES ('tt-ka-einrichtung', 1, 'Icon', 'redball.png');
-- INSERT INTO TopicProp VALUES ('tt-ka-einrichtung', 1, 'Creation Icon', 'createKompetenzstern.gif');
INSERT INTO TopicProp VALUES ('tt-ka-einrichtung', 1, 'Hidden Topic Names', 'on');
INSERT INTO TopicProp VALUES ('tt-ka-einrichtung', 1, 'Unique Topic Names', 'on');
INSERT INTO TopicProp VALUES ('tt-ka-einrichtung', 1, 'Custom Implementation', 'de.kiezatlas.deepamehta.topics.InstitutionTopic');
-- assign properties
INSERT INTO Association VALUES ('at-composition', 1, 1, 'a-ka-8', '', 'tt-ka-einrichtung', 1, 'pp-ka-oeffnungszeiten', 1);
INSERT INTO Association VALUES ('at-composition', 1, 1, 'a-ka-9', '', 'tt-ka-einrichtung', 1, 'pp-ka-sonstiges', 1);
INSERT INTO Association VALUES ('at-composition', 1, 1, 'a-ka-18', '', 'tt-ka-einrichtung', 1, 'pp-webalias', 1);
INSERT INTO Association VALUES ('at-composition', 1, 1, 'a-ka-41', '', 'tt-ka-einrichtung', 1, 'pp-password', 1);
INSERT INTO AssociationProp VALUES ('a-ka-8', 1, 'Ordinal Number', '185');
INSERT INTO AssociationProp VALUES ('a-ka-9', 1, 'Ordinal Number', '250');
INSERT INTO AssociationProp VALUES ('a-ka-18', 1, 'Ordinal Number', '350');
INSERT INTO AssociationProp VALUES ('a-ka-41', 1, 'Ordinal Number', '360');
-- super type
INSERT INTO Association VALUES ('at-derivation', 1, 1, 'a-ka-11', '', 'tt-institution', 1, 'tt-ka-einrichtung', 1);
-- container type
INSERT INTO Topic VALUES ('tt-topictype', 1, 1, 'tt-ka-einrichtungsuche', 'Einrichtungs-Suche');
INSERT INTO TopicProp VALUES ('tt-ka-einrichtungsuche', 1, 'Name', 'Einrichtungs-Suche');
-- INSERT INTO TopicProp VALUES ('tt-ka-einrichtungsuche', 1, 'Icon', 'KompetenzsternContainer.gif');
-- assign properties
INSERT INTO Association VALUES ('at-composition', 1, 1, 'a-ka-42', '', 'tt-ka-einrichtungsuche', 1, 'pp-webalias', 1);
INSERT INTO AssociationProp VALUES ('a-ka-42', 1, 'Ordinal Number', '150');
-- derive container type
INSERT INTO Association VALUES ('at-derivation', 1, 1, 'a-ka-12', '', 'tt-topiccontainer', 1, 'tt-ka-einrichtungsuche', 1);
-- assign type to container type
INSERT INTO Association VALUES ('at-aggregation', 1, 1, 'a-ka-13', '', 'tt-ka-einrichtungsuche', 1, 'tt-ka-einrichtung', 1);
-- relation to "Ansprechpartner"
INSERT INTO Association VALUES ('at-relation', 1, 1, 'a-ka-34', 'Ansprechpartner/in', 'tt-ka-einrichtung', 1, 'tt-person', 1);
INSERT INTO AssociationProp VALUES ('a-ka-34', 1, 'Name', 'Ansprechpartner/in');
INSERT INTO AssociationProp VALUES ('a-ka-34', 1, 'Cardinality', 'many');
INSERT INTO AssociationProp VALUES ('a-ka-34', 1, 'Association Type ID', 'at-association');
INSERT INTO AssociationProp VALUES ('a-ka-34', 1, 'Web Info', 'Related Topic Name');
INSERT INTO AssociationProp VALUES ('a-ka-34', 1, 'Web Form', 'Related Form');
INSERT INTO AssociationProp VALUES ('a-ka-34', 1, 'Ordinal Number', '155');
-- relation to "Träger"
INSERT INTO Association VALUES ('at-relation', 1, 1, 'a-ka-19', '', 'tt-ka-einrichtung', 1, 'tt-ka-traeger', 1);
INSERT INTO AssociationProp VALUES ('a-ka-19', 1, 'Cardinality', 'many');
INSERT INTO AssociationProp VALUES ('a-ka-19', 1, 'Association Type ID', 'at-association');
INSERT INTO AssociationProp VALUES ('a-ka-19', 1, 'Web Info', 'Related Info');
INSERT INTO AssociationProp VALUES ('a-ka-19', 1, 'Web Form', 'Related Form');
INSERT INTO AssociationProp VALUES ('a-ka-19', 1, 'Ordinal Number', '180');
-- relation to "Kategorie"
INSERT INTO Association VALUES ('at-relation', 1, 1, 'a-ka-23', '', 'tt-ka-einrichtung', 1, 'tt-ka-kategorie', 1);
INSERT INTO AssociationProp VALUES ('a-ka-23', 1, 'Cardinality', 'many');
INSERT INTO AssociationProp VALUES ('a-ka-23', 1, 'Association Type ID', 'at-association');
INSERT INTO AssociationProp VALUES ('a-ka-23', 1, 'Web Info', 'Related Topic Name');
INSERT INTO AssociationProp VALUES ('a-ka-23', 1, 'Web Form', 'Related Topic Selector');
INSERT INTO AssociationProp VALUES ('a-ka-23', 1, 'Ordinal Number', '188');
-- relation to "Angebot"
INSERT INTO Association VALUES ('at-relation', 1, 1, 'a-ka-32', '', 'tt-ka-einrichtung', 1, 'tt-ka-angebot', 1);
INSERT INTO AssociationProp VALUES ('a-ka-32', 1, 'Cardinality', 'many');
INSERT INTO AssociationProp VALUES ('a-ka-32', 1, 'Association Type ID', 'at-association');
INSERT INTO AssociationProp VALUES ('a-ka-32', 1, 'Web Info', 'Related Topic Name');
INSERT INTO AssociationProp VALUES ('a-ka-32', 1, 'Web Form', 'Related Topic Selector');
INSERT INTO AssociationProp VALUES ('a-ka-32', 1, 'Ordinal Number', '190');
-- relation to "Altersgruppe"
INSERT INTO Association VALUES ('at-relation', 1, 1, 'a-ka-33', '', 'tt-ka-einrichtung', 1, 'tt-ka-altersgruppe', 1);
INSERT INTO AssociationProp VALUES ('a-ka-33', 1, 'Cardinality', 'many');
INSERT INTO AssociationProp VALUES ('a-ka-33', 1, 'Association Type ID', 'at-association');
INSERT INTO AssociationProp VALUES ('a-ka-33', 1, 'Web Info', 'Related Topic Name');
INSERT INTO AssociationProp VALUES ('a-ka-33', 1, 'Web Form', 'Related Topic Selector');
INSERT INTO AssociationProp VALUES ('a-ka-33', 1, 'Ordinal Number', '195');
-- relation to "Image"
INSERT INTO Association VALUES ('at-relation', 1, 1, 'a-ka-38', '', 'tt-ka-einrichtung', 1, 'tt-image', 1);
INSERT INTO AssociationProp VALUES ('a-ka-38', 1, 'Cardinality', 'one');
INSERT INTO AssociationProp VALUES ('a-ka-38', 1, 'Association Type ID', 'at-association');
INSERT INTO AssociationProp VALUES ('a-ka-38', 1, 'Web Info', 'Related Info');
INSERT INTO AssociationProp VALUES ('a-ka-38', 1, 'Web Form', 'Related Form');
INSERT INTO AssociationProp VALUES ('a-ka-38', 1, 'Ordinal Number', '260');
--
INSERT INTO AssociationProp VALUES ('a-163', 1, 'Name', 'Telefon');
INSERT INTO AssociationProp VALUES ('a-106', 1, 'Name', 'Fax');
INSERT INTO AssociationProp VALUES ('a-406', 1, 'Name', 'E-mail');
INSERT INTO AssociationProp VALUES ('a-417', 1, 'Name', 'Website');
UPDATE Association SET Name='Telefon' WHERE ID='a-163';
UPDATE Association SET Name='Fax' WHERE ID='a-106';
UPDATE Association SET Name='E-mail' WHERE ID='a-406';
UPDATE Association SET Name='Website' WHERE ID='a-417';

--- "Träger" ---
INSERT INTO Topic VALUES ('tt-topictype', 1, 1, 'tt-ka-traeger', 'Träger');
INSERT INTO TopicProp VALUES ('tt-ka-traeger', 1, 'Name', 'Träger');
-- INSERT INTO TopicProp VALUES ('tt-ka-traeger', 1, 'Plural Name', 'Träger');
INSERT INTO TopicProp VALUES ('tt-ka-traeger', 1, 'Description', '<html><body><p>Ein <i>Tr&auml;ger</i> ist ...</p></body></html>');
INSERT INTO TopicProp VALUES ('tt-ka-traeger', 1, 'Description Query', 'Was ist ein Träger?');
-- INSERT INTO TopicProp VALUES ('tt-ka-traeger', 1, 'Icon', 'Kompetenzstern.gif');
-- INSERT INTO TopicProp VALUES ('tt-ka-traeger', 1, 'Creation Icon', 'createKompetenzstern.gif');
INSERT INTO TopicProp VALUES ('tt-ka-traeger', 1, 'Unique Topic Names', 'on');
-- INSERT INTO TopicProp VALUES ('tt-ka-traeger', 1, 'Custom Implementation', 'de.deepamehta.kompetenzstern.topics.KompetenzsternTopic');
-- assign properties
INSERT INTO Association VALUES ('at-composition', 1, 1, 'a-ka-37', '', 'tt-ka-traeger', 1, 'pp-ka-kind', 1);
INSERT INTO AssociationProp VALUES ('a-ka-37', 1, 'Ordinal Number', '110');
-- super type
INSERT INTO Association VALUES ('at-derivation', 1, 1, 'a-ka-14', '', 'tt-institution', 1, 'tt-ka-traeger', 1);
-- container type
INSERT INTO Topic VALUES ('tt-topictype', 1, 1, 'tt-ka-traegersuche', 'Träger-Suche');
INSERT INTO TopicProp VALUES ('tt-ka-traegersuche', 1, 'Name', 'Träger-Suche');
-- INSERT INTO TopicProp VALUES ('tt-ka-traegersuche', 1, 'Icon', 'KompetenzsternContainer.gif');
-- derive container type
INSERT INTO Association VALUES ('at-derivation', 1, 1, 'a-ka-15', '', 'tt-topiccontainer', 1, 'tt-ka-traegersuche', 1);
-- assign type to container type
INSERT INTO Association VALUES ('at-aggregation', 1, 1, 'a-ka-16', '', 'tt-ka-traegersuche', 1, 'tt-ka-traeger', 1);

--- "Kategorie" ---
INSERT INTO Topic VALUES ('tt-topictype', 1, 1, 'tt-ka-kategorie', 'Kategorie');
INSERT INTO TopicProp VALUES ('tt-ka-kategorie', 1, 'Name', 'Kategorie');
INSERT INTO TopicProp VALUES ('tt-ka-kategorie', 1, 'Plural Name', 'Kategorien');
INSERT INTO TopicProp VALUES ('tt-ka-kategorie', 1, 'Description', '<html><body><p>Eine <i>Kategorie</i> ist ...</p></body></html>');
INSERT INTO TopicProp VALUES ('tt-ka-kategorie', 1, 'Description Query', 'Was ist eine Kategorie?');
INSERT INTO TopicProp VALUES ('tt-ka-kategorie', 1, 'Icon', 'blackdot.gif');
-- INSERT INTO TopicProp VALUES ('tt-ka-kategorie', 1, 'Creation Icon', 'createKompetenzstern.gif');
INSERT INTO TopicProp VALUES ('tt-ka-kategorie', 1, 'Unique Topic Names', 'on');
INSERT INTO TopicProp VALUES ('tt-ka-kategorie', 1, 'Custom Implementation', 'de.kiezatlas.deepamehta.topics.CategoryTopic');
-- super type
INSERT INTO Association VALUES ('at-derivation', 1, 1, 'a-ka-20', '', 'tt-generic', 1, 'tt-ka-kategorie', 1);
-- container type
INSERT INTO Topic VALUES ('tt-topictype', 1, 1, 'tt-ka-kategoriesuche', 'Kategorie-Suche');
INSERT INTO TopicProp VALUES ('tt-ka-kategoriesuche', 1, 'Name', 'Kategorie-Suche');
-- INSERT INTO TopicProp VALUES ('tt-ka-kategoriesuche', 1, 'Icon', 'KompetenzsternContainer.gif');
-- derive container type
INSERT INTO Association VALUES ('at-derivation', 1, 1, 'a-ka-21', '', 'tt-topiccontainer', 1, 'tt-ka-kategoriesuche', 1);
-- assign type to container type
INSERT INTO Association VALUES ('at-aggregation', 1, 1, 'a-ka-22', '', 'tt-ka-kategoriesuche', 1, 'tt-ka-kategorie', 1);

--- "Angebot" ---
INSERT INTO Topic VALUES ('tt-topictype', 1, 1, 'tt-ka-angebot', 'Angebot');
INSERT INTO TopicProp VALUES ('tt-ka-angebot', 1, 'Name', 'Angebot');
INSERT INTO TopicProp VALUES ('tt-ka-angebot', 1, 'Plural Name', 'Angebote');
INSERT INTO TopicProp VALUES ('tt-ka-angebot', 1, 'Description', '<html><body><p>Ein <i>Angebot</i> ist ...</p></body></html>');
INSERT INTO TopicProp VALUES ('tt-ka-angebot', 1, 'Description Query', 'Was ist ein Angebot?');
INSERT INTO TopicProp VALUES ('tt-ka-angebot', 1, 'Icon', 'blackdot.gif');
-- INSERT INTO TopicProp VALUES ('tt-ka-angebot', 1, 'Creation Icon', 'createKompetenzstern.gif');
INSERT INTO TopicProp VALUES ('tt-ka-angebot', 1, 'Unique Topic Names', 'on');
INSERT INTO TopicProp VALUES ('tt-ka-angebot', 1, 'Custom Implementation', 'de.kiezatlas.deepamehta.topics.OfferTopic');
-- super type
INSERT INTO Association VALUES ('at-derivation', 1, 1, 'a-ka-10', '', 'tt-generic', 1, 'tt-ka-angebot', 1);
-- container type
INSERT INTO Topic VALUES ('tt-topictype', 1, 1, 'tt-ka-angebotsuche', 'Angebots-Suche');
INSERT INTO TopicProp VALUES ('tt-ka-angebotsuche', 1, 'Name', 'Angebots-Suche');
-- INSERT INTO TopicProp VALUES ('tt-ka-angebotsuche', 1, 'Icon', 'KompetenzsternContainer.gif');
-- derive container type
INSERT INTO Association VALUES ('at-derivation', 1, 1, 'a-ka-25', '', 'tt-topiccontainer', 1, 'tt-ka-angebotsuche', 1);
-- assign type to container type
INSERT INTO Association VALUES ('at-aggregation', 1, 1, 'a-ka-26', '', 'tt-ka-angebotsuche', 1, 'tt-ka-angebot', 1);

--- "Altersgruppe" ---
INSERT INTO Topic VALUES ('tt-topictype', 1, 1, 'tt-ka-altersgruppe', 'Altersgruppe');
INSERT INTO TopicProp VALUES ('tt-ka-altersgruppe', 1, 'Name', 'Altersgruppe');
INSERT INTO TopicProp VALUES ('tt-ka-altersgruppe', 1, 'Plural Name', 'Altersgruppen');
INSERT INTO TopicProp VALUES ('tt-ka-altersgruppe', 1, 'Description', '<html><body><p>Eine <i>Altersgruppe</i> ist ...</p></body></html>');
INSERT INTO TopicProp VALUES ('tt-ka-altersgruppe', 1, 'Description Query', 'Was ist eine Altersgruppe?');
INSERT INTO TopicProp VALUES ('tt-ka-altersgruppe', 1, 'Icon', 'blackdot.gif');
-- INSERT INTO TopicProp VALUES ('tt-ka-altersgruppe', 1, 'Creation Icon', 'createKompetenzstern.gif');
INSERT INTO TopicProp VALUES ('tt-ka-altersgruppe', 1, 'Unique Topic Names', 'on');
INSERT INTO TopicProp VALUES ('tt-ka-altersgruppe', 1, 'Custom Implementation', 'de.kiezatlas.deepamehta.topics.AgegroupTopic');
-- super type
INSERT INTO Association VALUES ('at-derivation', 1, 1, 'a-ka-27', '', 'tt-generic', 1, 'tt-ka-altersgruppe', 1);
-- container type
INSERT INTO Topic VALUES ('tt-topictype', 1, 1, 'tt-ka-altersgruppesuche', 'Altersgruppen-Suche');
INSERT INTO TopicProp VALUES ('tt-ka-altersgruppesuche', 1, 'Name', 'Altersgruppen-Suche');
-- INSERT INTO TopicProp VALUES ('tt-ka-altersgruppesuche', 1, 'Icon', 'KompetenzsternContainer.gif');
-- derive container type
INSERT INTO Association VALUES ('at-derivation', 1, 1, 'a-ka-28', '', 'tt-topiccontainer', 1, 'tt-ka-altersgruppesuche', 1);
-- assign type to container type
INSERT INTO Association VALUES ('at-aggregation', 1, 1, 'a-ka-29', '', 'tt-ka-altersgruppesuche', 1, 'tt-ka-altersgruppe', 1);

--- "File Import" ---
INSERT INTO Topic VALUES ('tt-topictype', 1, 1, 'tt-ka-fileimport', 'File Import');
INSERT INTO TopicProp VALUES ('tt-ka-fileimport', 1, 'Name', 'File Import');
INSERT INTO TopicProp VALUES ('tt-ka-fileimport', 1, 'Plural Name', 'File Imports');
INSERT INTO TopicProp VALUES ('tt-ka-fileimport', 1, 'Description', '<html><body><p>Ein <i>File Import</i> ist ...</p></body></html>');
INSERT INTO TopicProp VALUES ('tt-ka-fileimport', 1, 'Description Query', 'Was ist ein File Import?');
INSERT INTO TopicProp VALUES ('tt-ka-fileimport', 1, 'Custom Implementation', 'de.kiezatlas.deepamehta.topics.FileImportTopic');
-- super type
INSERT INTO Association VALUES ('at-derivation', 1, 1, 'a-ka-39', '', 'tt-file', 1, 'tt-ka-fileimport', 1);



------------------
--- Properties ---
------------------



--- "Öffnungszeiten" (Einrichtung) ---
INSERT INTO Topic VALUES ('tt-property', 1, 1, 'pp-ka-oeffnungszeiten', 'Öffnungszeiten');
INSERT INTO TopicProp VALUES ('pp-ka-oeffnungszeiten', 1, 'Name', 'Öffnungszeiten');
INSERT INTO TopicProp VALUES ('pp-ka-oeffnungszeiten', 1, 'Visualization', 'Multiline Input Field');

--- "Sonstiges" (Einrichtung) ---
INSERT INTO Topic VALUES ('tt-property', 1, 1, 'pp-ka-sonstiges', 'Sonstiges');
INSERT INTO TopicProp VALUES ('pp-ka-sonstiges', 1, 'Name', 'Sonstiges');
INSERT INTO TopicProp VALUES ('pp-ka-sonstiges', 1, 'Visualization', 'Multiline Input Field');

--- "Art" (Trger) ---
INSERT INTO Topic VALUES ('tt-property', 1, 1, 'pp-ka-kind', 'Art');
INSERT INTO TopicProp VALUES ('pp-ka-kind', 1, 'Name', 'Art');
INSERT INTO TopicProp VALUES ('pp-ka-kind', 1, 'Visualization', 'Option Buttons');
-- property values
INSERT INTO Topic VALUES ('tt-constant', 1, 1, 't-ka-kommunal', 'kommunal');
INSERT INTO TopicProp VALUES ('t-ka-kommunal', 1, 'Name', 'kommunal');
INSERT INTO Topic VALUES ('tt-constant', 1, 1, 't-ka-frei', 'frei');
INSERT INTO TopicProp VALUES ('t-ka-frei', 1, 'Name', 'frei');
-- assign property values
INSERT INTO Association VALUES ('at-composition', 1, 1, 'a-ka-35', '', 'pp-ka-kind', 1, 't-ka-kommunal', 1);
INSERT INTO Association VALUES ('at-composition', 1, 1, 'a-ka-36', '', 'pp-ka-kind', 1, 't-ka-frei', 1);
INSERT INTO AssociationProp VALUES ('a-ka-35', 1, 'Ordinal Number', '1');
INSERT INTO AssociationProp VALUES ('a-ka-36', 1, 'Ordinal Number', '2');



------------------
--- Kategorien ---
------------------



INSERT INTO Topic VALUES ('tt-ka-kategorie', 1, 1, 't-ka-kat1', 'Familien');
INSERT INTO TopicProp VALUES ('t-ka-kat1', 1, 'Name', 'Familien');
INSERT INTO TopicProp VALUES ('t-ka-kat1', 1, 'Icon', 'ka-M.gif');

INSERT INTO Topic VALUES ('tt-ka-kategorie', 1, 1, 't-ka-kat2', 'Kinder');
INSERT INTO TopicProp VALUES ('t-ka-kat2', 1, 'Name', 'Kinder');
INSERT INTO TopicProp VALUES ('t-ka-kat2', 1, 'Icon', 'ka-Q.gif');

INSERT INTO Topic VALUES ('tt-ka-kategorie', 1, 1, 't-ka-kat3', 'Jugend');
INSERT INTO TopicProp VALUES ('t-ka-kat3', 1, 'Name', 'Jugend');
INSERT INTO TopicProp VALUES ('t-ka-kat3', 1, 'Icon', 'ka-G.gif');

INSERT INTO Topic VALUES ('tt-ka-kategorie', 1, 1, 't-ka-kat4', 'Senioren');
INSERT INTO TopicProp VALUES ('t-ka-kat4', 1, 'Name', 'Senioren');
INSERT INTO TopicProp VALUES ('t-ka-kat4', 1, 'Icon', 'ka-E.gif');

INSERT INTO Topic VALUES ('tt-ka-kategorie', 1, 1, 't-ka-kat5', 'Ausbildung + Arbeit');
INSERT INTO TopicProp VALUES ('t-ka-kat5', 1, 'Name', 'Ausbildung + Arbeit');
INSERT INTO TopicProp VALUES ('t-ka-kat5', 1, 'Icon', 'ka-B.gif');

INSERT INTO Topic VALUES ('tt-ka-kategorie', 1, 1, 't-ka-kat6', 'Beratung');
INSERT INTO TopicProp VALUES ('t-ka-kat6', 1, 'Name', 'Beratung');
INSERT INTO TopicProp VALUES ('t-ka-kat6', 1, 'Icon', 'ka-J.gif');

INSERT INTO Topic VALUES ('tt-ka-kategorie', 1, 1, 't-ka-kat7', 'Gesundheit + Behinderung');
INSERT INTO TopicProp VALUES ('t-ka-kat7', 1, 'Name', 'Gesundheit + Behinderung');
INSERT INTO TopicProp VALUES ('t-ka-kat7', 1, 'Icon', 'ka-F.gif');

INSERT INTO Topic VALUES ('tt-ka-kategorie', 1, 1, 't-ka-kat8', 'Jugendamt');
INSERT INTO TopicProp VALUES ('t-ka-kat8', 1, 'Name', 'Jugendamt');
INSERT INTO TopicProp VALUES ('t-ka-kat8', 1, 'Icon', 'ka-P.gif');

INSERT INTO Topic VALUES ('tt-ka-kategorie', 1, 1, 't-ka-kat9', 'Kinderbetreuung');
INSERT INTO TopicProp VALUES ('t-ka-kat9', 1, 'Name', 'Kinderbetreuung');
INSERT INTO TopicProp VALUES ('t-ka-kat9', 1, 'Icon', 'ka-L.gif');

INSERT INTO Topic VALUES ('tt-ka-kategorie', 1, 1, 't-ka-kat10', 'Kultur + Bildung');
INSERT INTO TopicProp VALUES ('t-ka-kat10', 1, 'Name', 'Kultur + Bildung');
INSERT INTO TopicProp VALUES ('t-ka-kat10', 1, 'Icon', 'ka-A.gif');

INSERT INTO Topic VALUES ('tt-ka-kategorie', 1, 1, 't-ka-kat11', 'Nachbarschaft + Stadtteil');
INSERT INTO TopicProp VALUES ('t-ka-kat11', 1, 'Name', 'Nachbarschaft + Stadtteil');
INSERT INTO TopicProp VALUES ('t-ka-kat11', 1, 'Icon', 'ka-D.gif');

-- INSERT INTO Topic VALUES ('tt-ka-kategorie', 1, 1, 't-ka-kat12', 'Netzwerk');
-- INSERT INTO TopicProp VALUES ('t-ka-kat12', 1, 'Name', 'Netzwerk');
-- INSERT INTO TopicProp VALUES ('t-ka-kat12', 1, 'Icon', 'ka-N.gif');

INSERT INTO Topic VALUES ('tt-ka-kategorie', 1, 1, 't-ka-kat13', 'Not');
INSERT INTO TopicProp VALUES ('t-ka-kat13', 1, 'Name', 'Not');
INSERT INTO TopicProp VALUES ('t-ka-kat13', 1, 'Icon', 'ka-K.gif');

INSERT INTO Topic VALUES ('tt-ka-kategorie', 1, 1, 't-ka-kat14', 'Schule');
INSERT INTO TopicProp VALUES ('t-ka-kat14', 1, 'Name', 'Schule');
INSERT INTO TopicProp VALUES ('t-ka-kat14', 1, 'Icon', 'ka-H.gif');

INSERT INTO Topic VALUES ('tt-ka-kategorie', 1, 1, 't-ka-kat15', 'Sport');
INSERT INTO TopicProp VALUES ('t-ka-kat15', 1, 'Name', 'Sport');
INSERT INTO TopicProp VALUES ('t-ka-kat15', 1, 'Icon', 'ka-C.gif');

INSERT INTO Topic VALUES ('tt-ka-kategorie', 1, 1, 't-ka-kat16', 'Wohnung + Unterkunft');
INSERT INTO TopicProp VALUES ('t-ka-kat16', 1, 'Name', 'Wohnung + Unterkunft');
INSERT INTO TopicProp VALUES ('t-ka-kat16', 1, 'Icon', 'ka-I.gif');

INSERT INTO Topic VALUES ('tt-ka-kategorie', 1, 1, 't-ka-kat17', 'Gewerbe');
INSERT INTO TopicProp VALUES ('t-ka-kat17', 1, 'Name', 'Gewerbe');
INSERT INTO TopicProp VALUES ('t-ka-kat17', 1, 'Icon', 'ka-O.gif');



----------------
--- Angebote ---
----------------



INSERT INTO Topic VALUES ('tt-ka-angebot', 1, 1, 't-ka-ang1', 'Beratung');
INSERT INTO TopicProp VALUES ('t-ka-ang1', 1, 'Name', 'Beratung');

INSERT INTO Topic VALUES ('tt-ka-angebot', 1, 1, 't-ka-ang2', 'Betreuung');
INSERT INTO TopicProp VALUES ('t-ka-ang2', 1, 'Name', 'Betreuung');

INSERT INTO Topic VALUES ('tt-ka-angebot', 1, 1, 't-ka-ang3', 'Essen');
INSERT INTO TopicProp VALUES ('t-ka-ang3', 1, 'Name', 'Essen');

INSERT INTO Topic VALUES ('tt-ka-angebot', 1, 1, 't-ka-ang4', 'Kontakte');
INSERT INTO TopicProp VALUES ('t-ka-ang4', 1, 'Name', 'Kontakte');

INSERT INTO Topic VALUES ('tt-ka-angebot', 1, 1, 't-ka-ang5', 'Medien / Computer');
INSERT INTO TopicProp VALUES ('t-ka-ang5', 1, 'Name', 'Medien / Computer');

INSERT INTO Topic VALUES ('tt-ka-angebot', 1, 1, 't-ka-ang6', 'Qualifizierung / Aus- /Weiterbildung');
INSERT INTO TopicProp VALUES ('t-ka-ang6', 1, 'Name', 'Qualifizierung / Aus- /Weiterbildung');

INSERT INTO Topic VALUES ('tt-ka-angebot', 1, 1, 't-ka-ang7', 'Räume / Freiraumnutzung');
INSERT INTO TopicProp VALUES ('t-ka-ang7', 1, 'Name', 'Räume / Freiraumnutzung');

INSERT INTO Topic VALUES ('tt-ka-angebot', 1, 1, 't-ka-ang8', 'Spielzeug / Sportgeräte / Spiel- /Sportplatz');
INSERT INTO TopicProp VALUES ('t-ka-ang8', 1, 'Name', 'Spielzeug / Sportgeräte / Spiel- /Sportplatz');

INSERT INTO Topic VALUES ('tt-ka-angebot', 1, 1, 't-ka-ang9', 'Technische Geräte / Werkzeuge / Werkstätten');
INSERT INTO TopicProp VALUES ('t-ka-ang9', 1, 'Name', 'Technische Geräte / Werkzeuge / Werkstätten');

INSERT INTO Topic VALUES ('tt-ka-angebot', 1, 1, 't-ka-ang10', 'Transportmöglichkeiten');
INSERT INTO TopicProp VALUES ('t-ka-ang10', 1, 'Name', 'Transportmöglichkeiten');

INSERT INTO Topic VALUES ('tt-ka-angebot', 1, 1, 't-ka-ang11', 'Unterhaltung');
INSERT INTO TopicProp VALUES ('t-ka-ang11', 1, 'Name', 'Unterhaltung');

INSERT INTO Topic VALUES ('tt-ka-angebot', 1, 1, 't-ka-ang12', 'Vermittlung von Jobs / Nachbarschaftshilfe');
INSERT INTO TopicProp VALUES ('t-ka-ang12', 1, 'Name', 'Vermittlung von Jobs / Nachbarschaftshilfe');

INSERT INTO Topic VALUES ('tt-ka-angebot', 1, 1, 't-ka-ang13', 'Sonstige');
INSERT INTO TopicProp VALUES ('t-ka-ang13', 1, 'Name', 'Sonstige');



---------------------
--- Altersgruppen ---
---------------------



INSERT INTO Topic VALUES ('tt-ka-altersgruppe', 1, 1, 't-ka-alt1', 'Familien/Alleinerziehende mit Säuglingen');
INSERT INTO TopicProp VALUES ('t-ka-alt1', 1, 'Name', 'Familien/Alleinerziehende mit Säuglingen');

INSERT INTO Topic VALUES ('tt-ka-altersgruppe', 1, 1, 't-ka-alt2', 'Familien/Alleinerziehende mit Kleinkindern');
INSERT INTO TopicProp VALUES ('t-ka-alt2', 1, 'Name', 'Familien/Alleinerziehende mit Kleinkindern');

INSERT INTO Topic VALUES ('tt-ka-altersgruppe', 1, 1, 't-ka-alt3', 'Familien/Alleinerziehende mit Kindern im Schulalter');
INSERT INTO TopicProp VALUES ('t-ka-alt3', 1, 'Name', 'Familien/Alleinerziehende mit Kindern im Schulalter');

INSERT INTO Topic VALUES ('tt-ka-altersgruppe', 1, 1, 't-ka-alt4', 'Kinder im Schulalter');
INSERT INTO TopicProp VALUES ('t-ka-alt4', 1, 'Name', 'Kinder im Schulalter');

INSERT INTO Topic VALUES ('tt-ka-altersgruppe', 1, 1, 't-ka-alt5', 'Jugendliche/Selbstorganisierte');
INSERT INTO TopicProp VALUES ('t-ka-alt5', 1, 'Name', 'Jugendliche/Selbstorganisierte');

INSERT INTO Topic VALUES ('tt-ka-altersgruppe', 1, 1, 't-ka-alt6', 'Jugendgruppen');
INSERT INTO TopicProp VALUES ('t-ka-alt6', 1, 'Name', 'Jugendgruppen');

INSERT INTO Topic VALUES ('tt-ka-altersgruppe', 1, 1, 't-ka-alt7', 'Auszubildende & junge Erwachsene');
INSERT INTO TopicProp VALUES ('t-ka-alt7', 1, 'Name', 'Auszubildende & junge Erwachsene');

INSERT INTO Topic VALUES ('tt-ka-altersgruppe', 1, 1, 't-ka-alt8', 'Erwachsene');
INSERT INTO TopicProp VALUES ('t-ka-alt8', 1, 'Name', 'Erwachsene');

INSERT INTO Topic VALUES ('tt-ka-altersgruppe', 1, 1, 't-ka-alt9', 'Seniorinnen und Senioren');
INSERT INTO TopicProp VALUES ('t-ka-alt9', 1, 'Name', 'Seniorinnen und Senioren');

INSERT INTO Topic VALUES ('tt-ka-altersgruppe', 1, 1, 't-ka-alt10', 'Alle Altersgruppen');
INSERT INTO TopicProp VALUES ('t-ka-alt10', 1, 'Name', 'Alle Altersgruppen');

INSERT INTO Topic VALUES ('tt-ka-altersgruppe', 1, 1, 't-ka-alt11', 'Sonstige');
INSERT INTO TopicProp VALUES ('t-ka-alt11', 1, 'Name', 'Sonstige');



-----------------
--- Stadtplan ---
-----------------



-- "Schneberg-Nord"
INSERT INTO Topic VALUES ('tt-ka-stadtplan', 1, 1, 't-ka-schoeneberg', 'Schöneberg-Nord');
INSERT INTO TopicProp VALUES ('t-ka-schoeneberg', 1, 'Name', 'Schöneberg-Nord');
INSERT INTO TopicProp VALUES ('t-ka-schoeneberg', 1, 'Background Image', 'kartepur.jpg');
INSERT INTO TopicProp VALUES ('t-ka-schoeneberg', 1, 'Web Alias', 'schoeneberg-nord');
INSERT INTO ViewTopic VALUES ('t-ka-workmap', 1, 't-ka-schoeneberg', 1, 120, 80);
