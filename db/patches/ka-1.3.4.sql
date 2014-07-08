-----------------------
--- New Topic Types ---
-----------------------

--- "Fläche" ---
INSERT INTO Topic VALUES ('tt-topictype', 1, 1, 'tt-ka-shape', 'Fläche');
INSERT INTO TopicProp VALUES ('tt-ka-shape', 1, 'Name', 'Fläche');
INSERT INTO TopicProp VALUES ('tt-ka-shape', 1, 'Plural Name', 'Flächen');
INSERT INTO TopicProp VALUES ('tt-ka-shape', 1, 'Description', '<html><body><p>Eine <i>Fl&auml;che</i> ist ...</p></body></html>');
INSERT INTO TopicProp VALUES ('tt-ka-shape', 1, 'Description Query', 'Was ist eine Fläche?');
-- INSERT INTO TopicProp VALUES ('tt-ka-shape', 1, 'Icon', 'shape.gif');
-- INSERT INTO TopicProp VALUES ('tt-ka-shape', 1, 'Creation Icon', 'createKompetenzstern.gif');
-- INSERT INTO TopicProp VALUES ('tt-ka-shape', 1, 'Unique Topic Names', 'on');
-- INSERT INTO TopicProp VALUES ('tt-ka-shape', 1, 'Custom Implementation', 'de.kiezatlas.deepamehta.topics.ColorTopic');
-- assign properties
-- INSERT INTO Association VALUES ('at-composition', 1, 1, 'a-ka-73', '', 'tt-ka-shape', 1, 'pp-color', 1);
-- INSERT INTO AssociationProp VALUES ('a-ka-73', 1, 'Ordinal Number', '50');
-- super type
INSERT INTO Association VALUES ('at-derivation', 1, 1, 'a-ka-79', '', 'tt-generic', 1, 'tt-ka-shape', 1);
-- search type
INSERT INTO Topic VALUES ('tt-topictype', 1, 1, 'tt-ka-shape-search', 'Flächen Suche');
INSERT INTO TopicProp VALUES ('tt-ka-shape-search', 1, 'Name', 'Flächen Suche');
-- INSERT INTO TopicProp VALUES ('tt-ka-shape-search', 1, 'Icon', 'KompetenzsternContainer.gif');
-- derive search type
INSERT INTO Association VALUES ('at-derivation', 1, 1, 'a-ka-80', '', 'tt-topiccontainer', 1, 'tt-ka-shape-search', 1);
-- assign search type to type
INSERT INTO Association VALUES ('at-aggregation', 1, 1, 'a-ka-81', '', 'tt-ka-shape-search', 1, 'tt-ka-shape', 1);



--------------------------------
--- Type "Farbe" is obsolete ---
--------------------------------

-- Note: we could delete the type completely but remove just the assignment ---
-- (see ka-1.3.3.sql)

DELETE FROM Association WHERE ID='a-ka-77';
DELETE FROM AssociationProp WHERE AssociationID='a-ka-77';
DELETE FROM ViewAssociation WHERE AssociationID='a-ka-77';



---------------------------------------------------
--- Remove Property "Original Background Image" ---
---------------------------------------------------

-- (see ka-1.3.3.sql)

DELETE FROM Topic WHERE ID='pp-ka-original-bgimage';
DELETE FROM TopicProp WHERE TopicID='pp-ka-original-bgimage';
DELETE FROM ViewTopic WHERE TopicID='pp-ka-original-bgimage';

DELETE FROM Association WHERE ID='a-ka-78';
DELETE FROM AssociationProp WHERE AssociationID='a-ka-78';
DELETE FROM ViewAssociation WHERE AssociationID='a-ka-78';

