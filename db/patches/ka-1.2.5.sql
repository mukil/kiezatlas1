--- "Password" (Einrichtung) ---
INSERT INTO Topic VALUES ('tt-property', 1, 1, 'pp-ka-password', 'Password');
INSERT INTO TopicProp VALUES ('pp-ka-password', 1, 'Name', 'Password');
INSERT INTO TopicProp VALUES ('pp-ka-password', 1, 'Visualization', 'Input Field');

DELETE FROM Association WHERE ID='a-ka-41';
DELETE FROM ViewAssociation WHERE AssociationID='a-ka-41';
DELETE FROM AssociationProp WHERE AssociationID='a-ka-41';

INSERT INTO Association VALUES ('at-composition', 1, 1, 'a-ka-58', '', 'tt-ka-einrichtung', 1, 'pp-ka-password', 1);
INSERT INTO AssociationProp VALUES ('a-ka-58', 1, 'Ordinal Number', '360');
