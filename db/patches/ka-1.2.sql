----------------------
--- New Topic Type ---
----------------------

--- "YADE Referenzpunkt" ---
INSERT INTO Topic VALUES ('tt-topictype', 1, 1, 'tt-ka-yadepoint', 'YADE Referenzpunkt');
INSERT INTO TopicProp VALUES ('tt-ka-yadepoint', 1, 'Name', 'YADE Referenzpunkt');
INSERT INTO TopicProp VALUES ('tt-ka-yadepoint', 1, 'Plural Name', 'YADE Referenzpunkte');
INSERT INTO TopicProp VALUES ('tt-ka-yadepoint', 1, 'Description', '<html><body><p>Ein <i>YADE Referenzpunkt</i> ist ...</p></body></html>');
INSERT INTO TopicProp VALUES ('tt-ka-yadepoint', 1, 'Description Query', 'Was ist ein YADE Referenzpunkt?');
INSERT INTO TopicProp VALUES ('tt-ka-yadepoint', 1, 'Icon', 'crosshair.gif');
-- INSERT INTO TopicProp VALUES ('tt-ka-yadepoint', 1, 'Creation Icon', 'createKompetenzstern.gif');
-- INSERT INTO TopicProp VALUES ('tt-ka-yadepoint', 1, 'Unique Topic Names', 'on');
INSERT INTO TopicProp VALUES ('tt-ka-yadepoint', 1, 'Custom Implementation', 'de.kiezatlas.deepamehta.topics.YADEPointTopic');
-- super type
INSERT INTO Association VALUES ('at-derivation', 1, 1, 'a-ka-50', '', 'tt-generic', 1, 'tt-ka-yadepoint', 1);
-- search type
INSERT INTO Topic VALUES ('tt-topictype', 1, 1, 'tt-ka-yadepoint-search', 'YADE Referenzpunkt-Suche');
INSERT INTO TopicProp VALUES ('tt-ka-yadepoint-search', 1, 'Name', 'YADE Referenzpunkt-Suche');
-- INSERT INTO TopicProp VALUES ('tt-ka-yadepoint-search', 1, 'Icon', 'KompetenzsternContainer.gif');
-- derive search type
INSERT INTO Association VALUES ('at-derivation', 1, 1, 'a-ka-51', '', 'tt-topiccontainer', 1, 'tt-ka-yadepoint-search', 1);
-- assign type to search type
INSERT INTO Association VALUES ('at-aggregation', 1, 1, 'a-ka-52', '', 'tt-ka-yadepoint-search', 1, 'tt-ka-yadepoint', 1);



----------------------
--- New Properties ---
----------------------

--- "YADE x" (Einrichtung, YADE Referenzpunkt) ---
INSERT INTO Topic VALUES ('tt-property', 1, 1, 'pp-ka-yade-x', 'YADE x');
INSERT INTO TopicProp VALUES ('pp-ka-yade-x', 1, 'Name', 'YADE x');
INSERT INTO TopicProp VALUES ('pp-ka-yade-x', 1, 'Visualization', 'Input Field');
--- "YADE y" (Einrichtung, YADE Referenzpunkt) ---
INSERT INTO Topic VALUES ('tt-property', 1, 1, 'pp-ka-yade-y', 'YADE y');
INSERT INTO TopicProp VALUES ('pp-ka-yade-y', 1, 'Name', 'YADE y');
INSERT INTO TopicProp VALUES ('pp-ka-yade-y', 1, 'Visualization', 'Input Field');



--------------------------
--- Update Topic Types ---
--------------------------

-- assign properties to "Einrichtung"
INSERT INTO Association VALUES ('at-composition', 1, 1, 'a-ka-53', '', 'tt-ka-einrichtung', 1, 'pp-ka-yade-x', 1);
INSERT INTO Association VALUES ('at-composition', 1, 1, 'a-ka-54', '', 'tt-ka-einrichtung', 1, 'pp-ka-yade-y', 1);
INSERT INTO AssociationProp VALUES ('a-ka-53', 1, 'Ordinal Number', '370');
INSERT INTO AssociationProp VALUES ('a-ka-54', 1, 'Ordinal Number', '372');

-- assign properties to "YADE Referenzpunkt"
INSERT INTO Association VALUES ('at-composition', 1, 1, 'a-ka-55', '', 'tt-ka-yadepoint', 1, 'pp-ka-yade-x', 1);
INSERT INTO Association VALUES ('at-composition', 1, 1, 'a-ka-56', '', 'tt-ka-yadepoint', 1, 'pp-ka-yade-y', 1);
INSERT INTO AssociationProp VALUES ('a-ka-55', 1, 'Ordinal Number', '310');
INSERT INTO AssociationProp VALUES ('a-ka-56', 1, 'Ordinal Number', '312');



------------------------------------------------------------------------
--- Assign Topic Type "YADE Referenzpunkt" to workspace "Kiez-Atlas" ---
------------------------------------------------------------------------

INSERT INTO Association VALUES ('at-uses', 1, 1, 'a-ka-57', '', 't-ka-workspace', 1, 'tt-ka-yadepoint', 1);
INSERT INTO AssociationProp VALUES ('a-ka-57', 1, 'Access Permission', 'create');
