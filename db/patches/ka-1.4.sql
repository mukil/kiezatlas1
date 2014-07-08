----------------------
--- New Properties ---
----------------------

--- "Administrator Infos" ---
INSERT INTO Topic VALUES ('tt-property', 1, 1, 'pp-ka-administrator-info', 'Administrator Infos');
INSERT INTO TopicProp VALUES ('pp-ka-administrator-info', 1, 'Name', 'Administrator Infos');
INSERT INTO TopicProp VALUES ('pp-ka-administrator-info', 1, 'Visualization', 'Multiline Input Field');
-- assign property to "Einrichtung"
INSERT INTO Association VALUES ('at-composition', 1, 1, 'a-ka-82', '', 'tt-ka-einrichtung', 1, 'pp-ka-administrator-info', 1);
INSERT INTO AssociationProp VALUES ('a-ka-82', 1, 'Ordinal Number', '255');

--- "Target Web Alias" ---
INSERT INTO Topic VALUES ('tt-property', 1, 1, 'pp-ka-target-webalias', 'Target Web Alias');
INSERT INTO TopicProp VALUES ('pp-ka-target-webalias', 1, 'Name', 'Target Web Alias');
INSERT INTO TopicProp VALUES ('pp-ka-target-webalias', 1, 'Visualization', 'Input Field');
-- assign property to "Fläche"
INSERT INTO Association VALUES ('at-composition', 1, 1, 'a-ka-83', '', 'tt-ka-shape', 1, 'pp-ka-target-webalias', 1);
INSERT INTO AssociationProp VALUES ('a-ka-83', 1, 'Ordinal Number', '150');
