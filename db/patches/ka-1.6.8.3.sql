INSERT INTO Topic VALUES ('tt-assoctype', 1, 1, 'at-ka-submitter', 'Submitter');

INSERT INTO TopicProp VALUES ('at-ka-submitter', 1, 'Name', 'Submitter');
INSERT INTO TopicProp VALUES ('at-ka-submitter', 1, 'Plural Name', 'Submitter');
INSERT INTO TopicProp VALUES ('at-ka-submitter', 1, 'Description', '<html><body><p>Ein <i>Submitter</i> ist ein externer der mittels seiner Zugangsdaten im Web Interface neue Datensätze in einen Workspace eintragen kann.</p></body></html>');
INSERT INTO TopicProp VALUES ('at-ka-submitter', 1, 'Description Query', 'Was ist ein Submitter User?');
INSERT INTO TopicProp VALUES ('at-ka-submitter', 1, 'Color', '#009999');

INSERT INTO Association VALUES ('at-derivation', 1, 1, 'a-ka-submitter-link', '', 'at-membership', 1, 'at-ka-submitter', 1);

INSERT INTO Association VALUES ('at-uses', 1, 1, 'a-ka-submitter-use', '', 't-administrationgroup', 1, 'at-ka-submitter', 1);
INSERT INTO AssociationProp VALUES ('a-ka-submitter-use', 1, 'Access Permission', 'create');

INSERT INTO Topic VALUES ('tt-property', 1, 1, 'pp-ka-workspace-alias', 'Workspace Web Alias');
INSERT INTO TopicProp VALUES ('pp-ka-workspace-alias', 1, 'Name', 'Workspace Web Alias');
INSERT INTO TopicProp VALUES ('pp-ka-workspace-alias', 1, 'Visualization', 'Input Field');

INSERT INTO Association VALUES ('at-composition', 1, 1, 'a-ka-138', '', 'tt-workspace', 1, 'pp-ka-workspace-alias', 1);
INSERT INTO AssociationProp VALUES ('a-ka-138', 1, 'Ordinal Number', '109');

