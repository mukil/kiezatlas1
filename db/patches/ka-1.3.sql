
-------------------------------
--- Create Property "Stadt" ---
-------------------------------

--- "Stadt" (Einrichtung) ---
INSERT INTO Topic VALUES ('tt-property', 1, 1, 'pp-ka-stadt', 'Stadt');
INSERT INTO TopicProp VALUES ('pp-ka-stadt', 1, 'Name', 'Stadt');
INSERT INTO TopicProp VALUES ('pp-ka-stadt', 1, 'Visualization', 'Input Field');

-- assign property to "Einrichtung"
INSERT INTO Association VALUES ('at-composition', 1, 1, 'a-ka-59', '', 'tt-ka-einrichtung', 1, 'pp-ka-stadt', 1);
INSERT INTO AssociationProp VALUES ('a-ka-59', 1, 'Ordinal Number', '145');
