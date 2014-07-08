
-------------------------------
--- Intro Importer Config Topic
-------------------------------

--- NOTE: "a-ka-133" is the last assocId in use in this patch

--- create topic type "Importer Settings" ---

INSERT INTO Topic VALUES ('tt-topictype', 1, 1, 'tt-ka-importersettings', 'Importer Settings');
INSERT INTO TopicProp VALUES ('tt-ka-importersettings', 1, 'Name', 'Importer Settings');
INSERT INTO TopicProp VALUES ('tt-ka-importersettings', 1, 'Plural Name', 'Importer Settings');
INSERT INTO TopicProp VALUES ('tt-ka-importersettings', 1, 'Description', '<html><head></head><body><p>A <i>Importer Setting</i> is connected to a Kiezatlas Workspace, and provides additional information for the TimedImporter-Implementations</p></body></html>');
INSERT INTO TopicProp VALUES ('tt-ka-importersettings', 1, 'Description Query', 'What are "Importer Settings"?');
INSERT INTO TopicProp VALUES ('tt-ka-importersettings', 1, 'Icon', 'gear.gif');

-- super type
INSERT INTO Association VALUES ('at-derivation', 1, 1, 'a-ka-127', '', 'tt-generic', 1, 'tt-ka-importersettings', 1);

-- search type
INSERT INTO Topic VALUES ('tt-topictype', 1, 1, 'tt-ka-importersettings-search', 'Importer Settings Search');
INSERT INTO TopicProp VALUES ('tt-ka-importersettings-search', 1, 'Name', 'Importer Settings Search');

-- derive search type
INSERT INTO Association VALUES ('at-derivation', 1, 1, 'a-ka-128', '', 'tt-topiccontainer', 1, 'tt-ka-importersettings-search', 1);

-- assign search type to type
INSERT INTO Association VALUES ('at-aggregation', 1, 1, 'a-ka-129', '', 'tt-ka-importersettings-search', 1, 'tt-ka-importersettings', 1);

--- create property "Icons / Categories" ---
INSERT INTO Topic VALUES ('tt-property', 1, 1, 'pp-ka-importersetting-icons', 'Icons / Kategorien');
INSERT INTO TopicProp VALUES ('pp-ka-importersetting-icons', 1, 'Name', 'Icons / Kategorien');
INSERT INTO TopicProp VALUES ('pp-ka-importersetting-icons', 1, 'Visualization', 'Multiline Input Field');

--- create property "Content Report Mailbox" ---
INSERT INTO Topic VALUES ('tt-property', 1, 1, 'pp-ka-importersetting-contentbox', 'Content Report Mailbox');
INSERT INTO TopicProp VALUES ('pp-ka-importersetting-contentbox', 1, 'Name', 'Content Report Mailbox');
INSERT INTO TopicProp VALUES ('pp-ka-importersetting-contentbox', 1, 'Visualization', 'Input Field');

--- create property "Service Report Mailbox" ---
INSERT INTO Topic VALUES ('tt-property', 1, 1, 'pp-ka-importersetting-servicebox', 'Service Report Mailbox');
INSERT INTO TopicProp VALUES ('pp-ka-importersetting-servicebox', 1, 'Name', 'Service Report Mailbox');
INSERT INTO TopicProp VALUES ('pp-ka-importersetting-servicebox', 1, 'Visualization', 'Input Field');

--- assign properties" to "Import Settings"
INSERT INTO Association VALUES ('at-composition', 1, 1, 'a-ka-130', '', 'tt-ka-importersettings', 1, 'pp-ka-importersetting-icons', 1);
INSERT INTO AssociationProp VALUES ('a-ka-130', 1, 'Ordinal Number', '133');
INSERT INTO Association VALUES ('at-composition', 1, 1, 'a-ka-131', '', 'tt-ka-importersettings', 1, 'pp-ka-importersetting-contentbox', 1);
INSERT INTO AssociationProp VALUES ('a-ka-131', 1, 'Ordinal Number', '131');
INSERT INTO Association VALUES ('at-composition', 1, 1, 'a-ka-132', '', 'tt-ka-importersettings', 1, 'pp-ka-importersetting-servicebox', 1);
INSERT INTO AssociationProp VALUES ('a-ka-132', 1, 'Ordinal Number', '132');

--- assign "Importer Settings" Topic to Adminstration Workgroup
INSERT INTO Association VALUES ('at-uses', 1, 1, 'a-ka-133', '', 't-administrationgroup', 1, 'tt-ka-importersettings', 1);
INSERT INTO AssociationProp VALUES ('a-ka-133', 1, 'Access Permission', 'create');
