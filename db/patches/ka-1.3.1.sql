----------------------------
--- Create 2 Topic Types ---
----------------------------

--- "Forum" ---
INSERT INTO Topic VALUES ('tt-topictype', 1, 1, 'tt-ka-forum', 'Forum');
INSERT INTO TopicProp VALUES ('tt-ka-forum', 1, 'Name', 'Forum');
INSERT INTO TopicProp VALUES ('tt-ka-forum', 1, 'Plural Name', 'Foren');
INSERT INTO TopicProp VALUES ('tt-ka-forum', 1, 'Description', '<html><body><p>Ein <i>Forum</i> ist ...</p></body></html>');
INSERT INTO TopicProp VALUES ('tt-ka-forum', 1, 'Description Query', 'Was ist ein Forum?');
-- INSERT INTO TopicProp VALUES ('tt-ka-forum', 1, 'Icon', 'crosshair.gif');
-- INSERT INTO TopicProp VALUES ('tt-ka-forum', 1, 'Creation Icon', 'createKompetenzstern.gif');
-- INSERT INTO TopicProp VALUES ('tt-ka-forum', 1, 'Unique Topic Names', 'on');
-- INSERT INTO TopicProp VALUES ('tt-ka-forum', 1, 'Custom Implementation', 'de.kiezatlas.deepamehta.topics.YADEPointTopic');
-- assign properties
INSERT INTO Association VALUES ('at-composition', 1, 1, 'a-ka-68', '', 'tt-ka-forum', 1, 'pp-ka-forum-activition', 1);
INSERT INTO AssociationProp VALUES ('a-ka-68', 1, 'Ordinal Number', '10');
-- super type
-- INSERT INTO Association VALUES ('at-derivation', 1, 1, 'a-ka-50', '', 'tt-generic', 1, 'tt-ka-forum', 1);
-- search type
INSERT INTO Topic VALUES ('tt-topictype', 1, 1, 'tt-ka-forum-search', 'Forum Suche');
INSERT INTO TopicProp VALUES ('tt-ka-forum-search', 1, 'Name', 'Forum Suche');
-- INSERT INTO TopicProp VALUES ('tt-ka-forum-search', 1, 'Icon', 'KompetenzsternContainer.gif');
-- derive search type
INSERT INTO Association VALUES ('at-derivation', 1, 1, 'a-ka-60', '', 'tt-topiccontainer', 1, 'tt-ka-forum-search', 1);
-- assign type to search type
INSERT INTO Association VALUES ('at-aggregation', 1, 1, 'a-ka-61', '', 'tt-ka-forum-search', 1, 'tt-ka-forum', 1);

--- "Kommentar" ---
INSERT INTO Topic VALUES ('tt-topictype', 1, 1, 'tt-ka-kommentar', 'Kommentar');
INSERT INTO TopicProp VALUES ('tt-ka-kommentar', 1, 'Name', 'Kommentar');
INSERT INTO TopicProp VALUES ('tt-ka-kommentar', 1, 'Plural Name', 'Kommentare');
INSERT INTO TopicProp VALUES ('tt-ka-kommentar', 1, 'Description', '<html><body><p>Ein <i>Kommentar</i> ist ...</p></body></html>');
INSERT INTO TopicProp VALUES ('tt-ka-kommentar', 1, 'Description Query', 'Was ist ein Kommentar?');
-- INSERT INTO TopicProp VALUES ('tt-ka-kommentar', 1, 'Icon', 'crosshair.gif');
-- INSERT INTO TopicProp VALUES ('tt-ka-kommentar', 1, 'Creation Icon', 'createKompetenzstern.gif');
-- INSERT INTO TopicProp VALUES ('tt-ka-kommentar', 1, 'Unique Topic Names', 'on');
-- INSERT INTO TopicProp VALUES ('tt-ka-kommentar', 1, 'Custom Implementation', 'de.kiezatlas.deepamehta.topics.YADEPointTopic');
-- assign properties
INSERT INTO Association VALUES ('at-composition', 1, 1, 'a-ka-64', '', 'tt-ka-kommentar', 1, 'pp-text', 1);
INSERT INTO Association VALUES ('at-composition', 1, 1, 'a-ka-65', '', 'tt-ka-kommentar', 1, 'pp-ka-author', 1);
INSERT INTO Association VALUES ('at-composition', 1, 1, 'a-ka-66', '', 'tt-ka-kommentar', 1, 'pp-ka-date', 1);
INSERT INTO Association VALUES ('at-composition', 1, 1, 'a-ka-67', '', 'tt-ka-kommentar', 1, 'pp-ka-time', 1);
INSERT INTO AssociationProp VALUES ('a-ka-64', 1, 'Ordinal Number', '10');
INSERT INTO AssociationProp VALUES ('a-ka-65', 1, 'Ordinal Number', '20');
INSERT INTO AssociationProp VALUES ('a-ka-66', 1, 'Ordinal Number', '30');
INSERT INTO AssociationProp VALUES ('a-ka-67', 1, 'Ordinal Number', '40');
-- super type
-- INSERT INTO Association VALUES ('at-derivation', 1, 1, 'a-ka-50', '', 'tt-generic', 1, 'tt-ka-kommentar', 1);
-- search type
INSERT INTO Topic VALUES ('tt-topictype', 1, 1, 'tt-ka-kommentar-search', 'Kommentar Suche');
INSERT INTO TopicProp VALUES ('tt-ka-kommentar-search', 1, 'Name', 'Kommentar Suche');
-- INSERT INTO TopicProp VALUES ('tt-ka-kommentar-search', 1, 'Icon', 'KompetenzsternContainer.gif');
-- derive search type
INSERT INTO Association VALUES ('at-derivation', 1, 1, 'a-ka-62', '', 'tt-topiccontainer', 1, 'tt-ka-kommentar-search', 1);
-- assign type to search type
INSERT INTO Association VALUES ('at-aggregation', 1, 1, 'a-ka-63', '', 'tt-ka-kommentar-search', 1, 'tt-ka-kommentar', 1);

--- "Forum" Properties ---
INSERT INTO Topic VALUES ('tt-property', 1, 1, 'pp-ka-forum-activition', 'Aktivierung');
INSERT INTO TopicProp VALUES ('pp-ka-forum-activition', 1, 'Name', 'Aktivierung');
INSERT INTO TopicProp VALUES ('pp-ka-forum-activition', 1, 'Visualization', 'Switch');

--- "Kommentar" Properties ---
INSERT INTO Topic VALUES ('tt-property', 1, 1, 'pp-ka-date', 'Datum');
INSERT INTO TopicProp VALUES ('pp-ka-date', 1, 'Name', 'Datum');
INSERT INTO TopicProp VALUES ('pp-ka-date', 1, 'Visualization', 'Date Chooser');
--
INSERT INTO Topic VALUES ('tt-property', 1, 1, 'pp-ka-time', 'Uhrzeit');
INSERT INTO TopicProp VALUES ('pp-ka-time', 1, 'Name', 'Uhrzeit');
INSERT INTO TopicProp VALUES ('pp-ka-time', 1, 'Visualization', 'Time Chooser');
--
INSERT INTO Topic VALUES ('tt-property', 1, 1, 'pp-ka-author', 'Autor');
INSERT INTO TopicProp VALUES ('pp-ka-author', 1, 'Name', 'Autor');
INSERT INTO TopicProp VALUES ('pp-ka-author', 1, 'Visualization', 'Input Field');
