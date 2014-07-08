----------------------
--- New Topic Type ---
----------------------

--- "Kriterium" ---
INSERT INTO Topic VALUES ('tt-topictype', 1, 1, 'tt-ka-kriterium', 'Kriterium');
INSERT INTO TopicProp VALUES ('tt-ka-kriterium', 1, 'Name', 'Kriterium');
INSERT INTO TopicProp VALUES ('tt-ka-kriterium', 1, 'Plural Name', 'Kriterien');
INSERT INTO TopicProp VALUES ('tt-ka-kriterium', 1, 'Description', '<html><body><p>Ein <i>Kriterium</i> ist ...</p></body></html>');
INSERT INTO TopicProp VALUES ('tt-ka-kriterium', 1, 'Description Query', 'Was ist ein Kriterium?');
INSERT INTO TopicProp VALUES ('tt-ka-kriterium', 1, 'Icon', 'blackdot.gif');
-- INSERT INTO TopicProp VALUES ('tt-ka-kriterium', 1, 'Creation Icon', 'createKompetenzstern.gif');
INSERT INTO TopicProp VALUES ('tt-ka-kriterium', 1, 'Unique Topic Names', 'on');
-- INSERT INTO TopicProp VALUES ('tt-ka-kriterium', 1, 'Custom Implementation', 'de.kiezatlas.deepamehta.topics.CategoryTopic');
-- super type
INSERT INTO Association VALUES ('at-derivation', 1, 1, 'a-ka-43', '', 'tt-generic', 1, 'tt-ka-kriterium', 1);
-- container type
INSERT INTO Topic VALUES ('tt-topictype', 1, 1, 'tt-ka-kriteriumsuche', 'Kriterium-Suche');
INSERT INTO TopicProp VALUES ('tt-ka-kriteriumsuche', 1, 'Name', 'Kriterium-Suche');
-- INSERT INTO TopicProp VALUES ('tt-ka-kriteriumsuche', 1, 'Icon', 'KompetenzsternContainer.gif');
-- derive container type
INSERT INTO Association VALUES ('at-derivation', 1, 1, 'a-ka-44', '', 'tt-topiccontainer', 1, 'tt-ka-kriteriumsuche', 1);
-- assign type to container type
INSERT INTO Association VALUES ('at-aggregation', 1, 1, 'a-ka-45', '', 'tt-ka-kriteriumsuche', 1, 'tt-ka-kriterium', 1);

--- "Soziale Einrichtung" ---
INSERT INTO Topic VALUES ('tt-topictype', 1, 1, 'tt-ka-sozialeinrichtung', 'Soziale Einrichtung');
INSERT INTO TopicProp VALUES ('tt-ka-sozialeinrichtung', 1, 'Name', 'Soziale Einrichtung');
INSERT INTO TopicProp VALUES ('tt-ka-sozialeinrichtung', 1, 'Plural Name', 'Soziale Einrichtungen');
INSERT INTO TopicProp VALUES ('tt-ka-sozialeinrichtung', 1, 'Description', '<html><body><p>Eine <i>soziale Einrichtung</i> ist ...</p></body></html>');
INSERT INTO TopicProp VALUES ('tt-ka-sozialeinrichtung', 1, 'Description Query', 'Was ist eine soziale Einrichtung?');
-- INSERT INTO TopicProp VALUES ('tt-ka-sozialeinrichtung', 1, 'Icon', 'redball.png');
-- INSERT INTO TopicProp VALUES ('tt-ka-sozialeinrichtung', 1, 'Creation Icon', 'createKompetenzstern.gif');
INSERT INTO TopicProp VALUES ('tt-ka-sozialeinrichtung', 1, 'Hidden Topic Names', 'on');
INSERT INTO TopicProp VALUES ('tt-ka-sozialeinrichtung', 1, 'Unique Topic Names', 'on');
-- INSERT INTO TopicProp VALUES ('tt-ka-sozialeinrichtung', 1, 'Custom Implementation', 'de.kiezatlas.deepamehta.topics.InstitutionTopic');
-- super type
INSERT INTO Association VALUES ('at-derivation', 1, 1, 'a-ka-46', '', 'tt-ka-einrichtung', 1, 'tt-ka-sozialeinrichtung', 1);
-- container type
INSERT INTO Topic VALUES ('tt-topictype', 1, 1, 'tt-ka-sozialeinrichtungsuche', 'Sozialeinrichtungs-Suche');
INSERT INTO TopicProp VALUES ('tt-ka-sozialeinrichtungsuche', 1, 'Name', 'Sozialeinrichtungs-Suche');
-- INSERT INTO TopicProp VALUES ('tt-ka-sozialeinrichtungsuche', 1, 'Icon', 'KompetenzsternContainer.gif');
-- assign properties
-- INSERT INTO Association VALUES ('at-composition', 1, 1, 'a-ka-47', '', 'tt-ka-sozialeinrichtungsuche', 1, 'pp-webalias', 1);
-- INSERT INTO AssociationProp VALUES ('a-ka-47', 1, 'Ordinal Number', '150');
-- derive container type
INSERT INTO Association VALUES ('at-derivation', 1, 1, 'a-ka-48', '', 'tt-topiccontainer', 1, 'tt-ka-sozialeinrichtungsuche', 1);
-- assign type to container type
INSERT INTO Association VALUES ('at-aggregation', 1, 1, 'a-ka-49', '', 'tt-ka-sozialeinrichtungsuche', 1, 'tt-ka-sozialeinrichtung', 1);



--------------------------
--- Update Topic Types ---
--------------------------

-- remove custom implementation from "Kategorie", "Angebot" and "Altersgruppe"
DELETE FROM TopicProp WHERE TopicID='tt-ka-kategorie' AND PropName='Custom Implementation';
DELETE FROM TopicProp WHERE TopicID='tt-ka-angebot' AND PropName='Custom Implementation';
DELETE FROM TopicProp WHERE TopicID='tt-ka-altersgruppe' AND PropName='Custom Implementation';
-- remove icons from "Kategorie", "Angebot" and "Altersgruppe"
DELETE FROM TopicProp WHERE TopicID='tt-ka-kategorie' AND PropName='Icon';
DELETE FROM TopicProp WHERE TopicID='tt-ka-angebot' AND PropName='Icon';
DELETE FROM TopicProp WHERE TopicID='tt-ka-altersgruppe' AND PropName='Icon';

-- set "Kriterium" as new supertype of "Kategorie", "Angebot" and "Altersgruppe"
DELETE FROM ViewAssociation WHERE AssociationID='a-ka-10';
DELETE FROM ViewAssociation WHERE AssociationID='a-ka-20';
DELETE FROM ViewAssociation WHERE AssociationID='a-ka-27';
UPDATE Association SET TopicID1='tt-ka-kriterium' WHERE ID='a-ka-10';
UPDATE Association SET TopicID1='tt-ka-kriterium' WHERE ID='a-ka-20';
UPDATE Association SET TopicID1='tt-ka-kriterium' WHERE ID='a-ka-27';

-- set "Soziale Einrichtung" as new origin of relations to "Kategorie", "Angebot", "Altersgruppe" und "Träger"
DELETE FROM ViewAssociation WHERE AssociationID='a-ka-23';
DELETE FROM ViewAssociation WHERE AssociationID='a-ka-32';
DELETE FROM ViewAssociation WHERE AssociationID='a-ka-33';
DELETE FROM ViewAssociation WHERE AssociationID='a-ka-19';
UPDATE Association SET TopicID1='tt-ka-sozialeinrichtung' WHERE ID='a-ka-23';
UPDATE Association SET TopicID1='tt-ka-sozialeinrichtung' WHERE ID='a-ka-32';
UPDATE Association SET TopicID1='tt-ka-sozialeinrichtung' WHERE ID='a-ka-33';
UPDATE Association SET TopicID1='tt-ka-sozialeinrichtung' WHERE ID='a-ka-19';

-- let "Kiez-Atlas" members create "Soziale Einrichtung" instead of "Einrichtung"
DELETE FROM ViewAssociation WHERE AssociationID='a-ka-3';
UPDATE Association SET TopicID2='tt-ka-sozialeinrichtung' WHERE ID='a-ka-3';

-- switch default "Access Permission" to "create" (was "view")
UPDATE TopicProp SET PropValue='create' WHERE TopicID='pp-createpermission' AND PropName='Default Value';



----------------------
--- Update Content ---
----------------------

-- retype all topics of type "Einrichtung" to "Soziale Einrichtung"
UPDATE Topic SET TypeID='tt-ka-sozialeinrichtung' WHERE TypeID='tt-ka-einrichtung';
