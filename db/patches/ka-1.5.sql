--- "Geo Objekt" ---
INSERT INTO Topic VALUES ('tt-topictype', 1, 1, 'tt-ka-geoobject', 'Geo Objekt');

INSERT INTO TopicProp VALUES ('tt-ka-geoobject', 1, 'Name', 'Geo Objekt');
INSERT INTO TopicProp VALUES ('tt-ka-geoobject', 1, 'Plural Name', 'Geo Objekte');
INSERT INTO TopicProp VALUES ('tt-ka-geoobject', 1, 'Description', '<html><body><p>Ein <i>Geo Objekt</i> ist ...</p></body></html>');
INSERT INTO TopicProp VALUES ('tt-ka-geoobject', 1, 'Description Query', 'Was ist ein Geo Objekt?');
INSERT INTO TopicProp VALUES ('tt-ka-geoobject', 1, 'Icon', 'redball.png');
-- INSERT INTO TopicProp VALUES ('tt-ka-geoobject', 1, 'Creation Icon', 'createKompetenzstern.gif');
INSERT INTO TopicProp VALUES ('tt-ka-geoobject', 1, 'Hidden Topic Names', 'on');
INSERT INTO TopicProp VALUES ('tt-ka-geoobject', 1, 'Unique Topic Names', 'on');
INSERT INTO TopicProp VALUES ('tt-ka-geoobject', 1, 'Custom Implementation', 'de.kiezatlas.deepamehta.topics.GeoObjectTopic');


--- "create association to properties" ---

INSERT INTO Association VALUES ('at-composition', 1, 1, 'a-ka-90', '', 'tt-ka-geoobject', 1, 'pp-webalias', 1);
INSERT INTO Association VALUES ('at-composition', 1, 1, 'a-ka-91', '', 'tt-ka-geoobject', 1, 'pp-ka-password', 1);
INSERT INTO Association VALUES ('at-composition', 1, 1, 'a-ka-92', '', 'tt-ka-geoobject', 1, 'pp-ka-yade-x', 1);
INSERT INTO Association VALUES ('at-composition', 1, 1, 'a-ka-93', '', 'tt-ka-geoobject', 1, 'pp-ka-yade-y', 1);

--- "assign properties need to be updated, in my instance but inserted into the existing instance" ---

INSERT INTO AssociationProp VALUES ('a-ka-90', 1, 'Ordinal Number', '350');
INSERT INTO AssociationProp VALUES ('a-ka-91', 1, 'Ordinal Number', '360');
INSERT INTO AssociationProp VALUES ('a-ka-92', 1, 'Ordinal Number', '370');
INSERT INTO AssociationProp VALUES ('a-ka-93', 1, 'Ordinal Number', '372');

-- super type from geoobject
INSERT INTO Association VALUES ('at-derivation', 1, 1, 'a-ka-94', '', 'tt-generic', 1, 'tt-ka-geoobject', 1);

-- search type
INSERT INTO Topic VALUES ('tt-topictype', 1, 1, 'tt-ka-geoobject-search', 'Geo Objekt Suche');
INSERT INTO TopicProp VALUES ('tt-ka-geoobject-search', 1, 'Name', 'Geo Objekt Suche');
-- INSERT INTO TopicProp VALUES ('tt-ka-einrichtungsuche', 1, 'Icon', 'KompetenzsternContainer.gif');

-- assign properties
INSERT INTO Association VALUES ('at-composition', 1, 1, 'a-ka-95', '', 'tt-ka-geoobject-search', 1, 'pp-webalias', 1);
INSERT INTO AssociationProp VALUES ('a-ka-95', 1, 'Ordinal Number', '150');

-- derive search type
INSERT INTO Association VALUES ('at-derivation', 1, 1, 'a-ka-96', '', 'tt-topiccontainer', 1, 'tt-ka-geoobject-search', 1);

-- assign type to search type
INSERT INTO Association VALUES ('at-aggregation', 1, 1, 'a-ka-97', '', 'tt-ka-geoobject-search', 1, 'tt-ka-geoobject', 1);



-----------------------
--- Conversion Part ---
-----------------------



--- delete "Einrichtung"s derivation from "Institution"

DELETE FROM Association WHERE ID = 'a-ka-11';
DELETE from AssociationProp WHERE AssociationID = 'a-ka-11';
DELETE FROM ViewAssociation WHERE AssociationID = 'a-ka-11';

--- delete assignments of "Einrichtung"s properties which are now assigned to supertype "Geo Objekt"
--- "Web Alias", "Password", "YADE x", and "YADE y"

DELETE FROM Association WHERE ID = 'a-ka-18';
DELETE FROM Association WHERE ID = 'a-ka-58';
DELETE FROM Association WHERE ID = 'a-ka-53';
DELETE FROM Association WHERE ID = 'a-ka-54';

DELETE FROM AssociationProp WHERE AssociationID = 'a-ka-18';
DELETE FROM AssociationProp WHERE AssociationID = 'a-ka-58';
DELETE FROM AssociationProp WHERE AssociationID = 'a-ka-53';
DELETE FROM AssociationProp WHERE AssociationID = 'a-ka-54';

DELETE FROM ViewAssociation WHERE AssociationID = 'a-ka-18';
DELETE FROM ViewAssociation WHERE AssociationID = 'a-ka-58';
DELETE FROM ViewAssociation WHERE AssociationID = 'a-ka-53';
DELETE FROM ViewAssociation WHERE AssociationID = 'a-ka-54';

--- delete "Einrichtung"s relation to "Image"

DELETE FROM Association WHERE ID = 'a-ka-38';
DELETE FROM AssociationProp WHERE AssociationID = 'a-ka-38';
DELETE FROM ViewAssociation WHERE AssociationID = 'a-ka-38';

--- delete "Custom Implementation" from "Einrichtung", it's now derived from "Geo Objekt"
DELETE FROM TopicProp WHERE TopicID='tt-ka-einrichtung' AND PropName = 'Custom Implementation';
DELETE FROM TopicProp WHERE TopicID='tt-ka-color' AND PropName = 'Custom Implementation';

--- derive "Einrichtung" from "Geo Objekt"
INSERT INTO Association VALUES ('at-derivation', 1, 1, 'a-ka-98', '', 'tt-ka-geoobject', 1, 'tt-ka-einrichtung', 1);

--- relate "Einrichtung" to "Webpage", "Phone Number", "Fax Number", "Email Address", and "Address"
-- (these relations where formerly derived from the "Institution" type)

INSERT INTO Association VALUES ('at-relation', 1, 1, 'a-ka-99', 'Website', 'tt-ka-einrichtung', 1, 'tt-webpage', 1);
INSERT INTO AssociationProp VALUES ('a-ka-99', 1, 'Name', 'Website');
INSERT INTO AssociationProp VALUES ('a-ka-99', 1, 'Cardinality', 'many');
INSERT INTO AssociationProp VALUES ('a-ka-99', 1, 'Association Type ID', 'at-association');
INSERT INTO AssociationProp VALUES ('a-ka-99', 1, 'Web Info', 'Related Topic Name');
INSERT INTO AssociationProp VALUES ('a-ka-99', 1, 'Web Form', 'Related Form');
INSERT INTO AssociationProp VALUES ('a-ka-99', 1, 'Ordinal Number', '185');

INSERT INTO Association VALUES ('at-relation', 1, 1, 'a-ka-100', 'Telefon', 'tt-ka-einrichtung', 1, 'tt-phonenumber', 1);
INSERT INTO AssociationProp VALUES ('a-ka-100', 1, 'Name', 'Telefon');
INSERT INTO AssociationProp VALUES ('a-ka-100', 1, 'Cardinality', 'many');
INSERT INTO AssociationProp VALUES ('a-ka-100', 1, 'Association Type ID', 'at-association');
INSERT INTO AssociationProp VALUES ('a-ka-100', 1, 'Web Info', 'Related Topic Name');
INSERT INTO AssociationProp VALUES ('a-ka-100', 1, 'Web Form', 'Related Form');
INSERT INTO AssociationProp VALUES ('a-ka-100', 1, 'Ordinal Number', '160');

INSERT INTO Association VALUES ('at-relation', 1, 1, 'a-ka-101', 'Fax', 'tt-ka-einrichtung', 1, 'tt-faxnumber', 1);
INSERT INTO AssociationProp VALUES ('a-ka-101', 1, 'Name', 'Fax');
INSERT INTO AssociationProp VALUES ('a-ka-101', 1, 'Cardinality', 'many');
INSERT INTO AssociationProp VALUES ('a-ka-101', 1, 'Association Type ID', 'at-association');
INSERT INTO AssociationProp VALUES ('a-ka-101', 1, 'Web Info', 'Related Topic Name');
INSERT INTO AssociationProp VALUES ('a-ka-101', 1, 'Web Form', 'Related Form');
INSERT INTO AssociationProp VALUES ('a-ka-101', 1, 'Ordinal Number', '165');

INSERT INTO Association VALUES ('at-relation', 1, 1, 'a-ka-102', 'Email', 'tt-ka-einrichtung', 1, 'tt-emailaddress', 1);
INSERT INTO AssociationProp VALUES ('a-ka-102', 1, 'Name', 'Email');
INSERT INTO AssociationProp VALUES ('a-ka-102', 1, 'Cardinality', 'many');
INSERT INTO AssociationProp VALUES ('a-ka-102', 1, 'Association Type ID', 'at-association');
INSERT INTO AssociationProp VALUES ('a-ka-102', 1, 'Web Info', 'Related Topic Name');
INSERT INTO AssociationProp VALUES ('a-ka-102', 1, 'Web Form', 'Related Form');
INSERT INTO AssociationProp VALUES ('a-ka-102', 1, 'Ordinal Number', '175');

INSERT INTO Association VALUES ('at-relation', 1, 1, 'a-ka-103', 'Adresse', 'tt-ka-einrichtung', 1, 'tt-address', 1);
INSERT INTO AssociationProp VALUES ('a-ka-103', 1, 'Name', 'Adresse');
INSERT INTO AssociationProp VALUES ('a-ka-103', 1, 'Cardinality', 'one');
INSERT INTO AssociationProp VALUES ('a-ka-103', 1, 'Association Type ID', 'at-association');
--- guess it is not deeply related info, cause of unused city relation to adress in the tt-ka-einrichtung, cause of own property stadt
INSERT INTO AssociationProp VALUES ('a-ka-103', 1, 'Web Info', 'Related Info');
INSERT INTO AssociationProp VALUES ('a-ka-103', 1, 'Web Form', 'Related Form');
INSERT INTO AssociationProp VALUES ('a-ka-103', 1, 'Ordinal Number', '140');

--- relate "Geo Objekt" to "Image"
INSERT INTO Association VALUES ('at-relation', 1, 1, 'a-ka-104', 'Image', 'tt-ka-geoobject', 1, 'tt-image', 1);
INSERT INTO AssociationProp VALUES ('a-ka-104', 1, 'Name', 'Image');
INSERT INTO AssociationProp VALUES ('a-ka-104', 1, 'Cardinality', 'one');
INSERT INTO AssociationProp VALUES ('a-ka-104', 1, 'Association Type ID', 'at-association');
INSERT INTO AssociationProp VALUES ('a-ka-104', 1, 'Web Info', 'Related Info');
INSERT INTO AssociationProp VALUES ('a-ka-104', 1, 'Web Form', 'Related Form');
INSERT INTO AssociationProp VALUES ('a-ka-104', 1, 'Ordinal Number', '109');

--- Update ordinal numbers for 3 types/properties:
-- "Ansprechpartner" (relation from "Einrichtung" to "Person")
-- "Öffnungszeiten" (property of "Einrichtung")
-- "Träger" (relation from "Soziale Einrichtung" to "Träger")
UPDATE AssociationProp SET PropValue='170' WHERE AssociationID='a-ka-34' AND PropName='Ordinal Number';
UPDATE AssociationProp SET PropValue='155' WHERE AssociationID='a-ka-8' AND PropName='Ordinal Number';
UPDATE AssociationProp SET PropValue='198' WHERE AssociationID='a-ka-19' AND PropName='Ordinal Number';


--- Version Change
--UPDATE TopicProp SET PropValue='Kiezatlas--Test'         WHERE TopicID='t-deepamehtainstallation' AND PropName='Client Name';
--UPDATE TopicProp SET PropValue='DeepaMehtaServer 2.0b8'   WHERE TopicID='t-deepamehtainstallation' AND PropName='Server Name';

--- assure compatibility of the old kiezatlas publishing scheme
-- set 'Publisher' property for all existing memberships
INSERT AssociationProp (AssociationID, AssociationVersion, PropName, PropValue) SELECT ID, 1, 'Publisher', 'on' FROM Association WHERE TypeID = 'at-membership';
