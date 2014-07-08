INSERT INTO Topic VALUES ('tt-property', 1, 1, 'pp-ka-mobile-citymap', 'Mobiler Stadtplan');
INSERT INTO TopicProp VALUES ('pp-ka-mobile-citymap', 1, 'Name', 'Mobiler Stadtplan');
INSERT INTO TopicProp VALUES ('pp-ka-mobile-citymap', 1, 'Visualization', 'Switch');

INSERT INTO Association VALUES ('at-composition', 1, 1, 'a-ka-139', '', 'tt-ka-stadtplan', 1, 'pp-ka-mobile-citymap', 1);
INSERT INTO AssociationProp VALUES ('a-ka-139', 1, 'Ordinal Number', '349');
