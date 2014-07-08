--- "New Membershop Role, Affiliated" ---

INSERT INTO Topic VALUES ('tt-assoctype', 1, 1, 'at-ka-affiliated', 'Affiliated');

INSERT INTO TopicProp VALUES ('at-ka-affiliated', 1, 'Name', 'Affiliated');
INSERT INTO TopicProp VALUES ('at-ka-affiliated', 1, 'Plural Name', 'Affiliated');
INSERT INTO TopicProp VALUES ('at-ka-affiliated', 1, 'Description', '<html><body><p>Ein <i>Affiliated User</i> ist ein Kiez-Mitarbeiter der nur im Web Interface lesen darf.</p></body></html>');
INSERT INTO TopicProp VALUES ('at-ka-affiliated', 1, 'Description Query', 'Was ist ein Affiliated User?');
INSERT INTO TopicProp VALUES ('at-ka-affiliated', 1, 'Color', '#99ff99');

-- super type from Affiliated

INSERT INTO Association VALUES ('at-derivation', 1, 1, 'a-ka-affiliated-link', '', 'at-membership', 1, 'at-ka-affiliated', 1);

-- assign Affiliated AssocType to Administration Workspace

INSERT INTO Association VALUES ('at-uses', 1, 1, 'a-ka-affiliated-use', '', 't-administrationgroup', 1, 'at-ka-affiliated', 1);
INSERT INTO AssociationProp VALUES ('a-ka-affiliated-use', 1, 'Access Permission', 'create');

