INSERT INTO Topic VALUES ('tt-property', 1, 1, 'pp-ka-stichwort', 'Stichworte');
INSERT INTO TopicProp VALUES ('pp-ka-stichwort', 1, 'Name', 'Stichworte');
INSERT INTO TopicProp VALUES ('pp-ka-stichwort', 1, 'Visualization', 'Input Field');

INSERT INTO Topic VALUES ('tt-property', 1, 1, 'pp-ka-barrierefrei', 'Barrierefrei');
INSERT INTO TopicProp VALUES ('pp-ka-barrierefrei', 1, 'Name', 'Barrierefrei');
INSERT INTO TopicProp VALUES ('pp-ka-barrierefrei', 1, 'Visualization', 'Options Menu');
INSERT INTO TopicProp VALUES ('pp-ka-barrierefrei', 1, 'Default Value', 'Nein');

INSERT INTO Topic VALUES ('tt-constant', 1, 1, 'pp-ka-constant-yes', 'Ja');
INSERT INTO TopicProp VALUES ('pp-ka-constant-yes', 1, 'Name', 'Ja');
INSERT INTO Topic VALUES ('tt-constant', 1, 1, 'pp-ka-constant-constrained', 'Eingeschränkt');
INSERT INTO TopicProp VALUES ('pp-ka-constant-constrained', 1, 'Name', 'Eingeschränkt');
INSERT INTO Topic VALUES ('tt-constant', 1, 1, 'pp-ka-constant-no', 'Nein');
INSERT INTO TopicProp VALUES ('pp-ka-constant-no', 1, 'Name', 'Nein');

INSERT INTO Association VALUES ('at-composition', 1, 1, 'a-ka-134', '', 'pp-ka-barrierefrei', 1, 'pp-ka-constant-yes', 1);
INSERT INTO AssociationProp VALUES ('a-ka-134', 1, 'Ordinal Number', '2');
INSERT INTO Association VALUES ('at-composition', 1, 1, 'a-ka-135', '', 'pp-ka-barrierefrei', 1, 'pp-ka-constant-constrained', 1);
INSERT INTO AssociationProp VALUES ('a-ka-135', 1, 'Ordinal Number', '3');
INSERT INTO Association VALUES ('at-composition', 1, 1, 'a-ka-136', '', 'pp-ka-barrierefrei', 1, 'pp-ka-constant-no', 1);
INSERT INTO AssociationProp VALUES ('a-ka-136', 1, 'Ordinal Number', '1');

INSERT INTO Association VALUES ('at-composition', 1, 1, 'a-ka-137', '', 'tt-ka-geoobject', 1, 'pp-ka-stichwort', 1);

