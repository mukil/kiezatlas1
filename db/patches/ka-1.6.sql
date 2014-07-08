
--------------------
--- New Property ---
--------------------

--- "Zuletzt geändert" ---
INSERT INTO Topic VALUES ('tt-property', 1, 1, 'pp-ka-last-modified', 'Zuletzt geändert');
INSERT INTO TopicProp VALUES ('pp-ka-last-modified', 1, 'Name', 'Zuletzt geändert');
INSERT INTO TopicProp VALUES ('pp-ka-last-modified', 1, 'Visualization', 'Date Chooser');
-- assign property to "Geo Objekt"
INSERT INTO Association VALUES ('at-composition', 1, 1, 'a-ka-105', '', 'tt-ka-geoobject', 1, 'pp-ka-last-modified', 1);
INSERT INTO AssociationProp VALUES ('a-ka-105', 1, 'Ordinal Number', '380');



------------------------
--- Reorder Property ---
------------------------

-- reorder "Image", was 109
UPDATE AssociationProp SET PropValue='187' WHERE AssociationID='a-ka-104' AND PropName='Ordinal Number';
