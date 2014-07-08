
----------------------------
--- Update Email Feature from patch 2.18 and 2.19 ka-1.6.2 which i skipped to install then ---
----------------------------

--- "Recipient" ---
INSERT INTO Topic VALUES ('tt-assoctype', 1, 1, 'at-recipient', 'Recipient');
INSERT INTO TopicProp VALUES ('at-recipient', 1, 'Name', 'Recipient');
INSERT INTO TopicProp VALUES ('at-recipient', 1, 'Plural Name', 'Recipients');
INSERT INTO TopicProp VALUES ('at-recipient', 1, 'Color', '#E14589');

--- "Sender" ---
INSERT INTO Topic VALUES ('tt-assoctype', 1, 1, 'at-sender', 'Sender');
INSERT INTO TopicProp VALUES ('at-sender', 1, 'Name', 'Sender');
INSERT INTO TopicProp VALUES ('at-sender', 1, 'Plural Name', 'Senders');
INSERT INTO TopicProp VALUES ('at-sender', 1, 'Color', '#4589E1');

--- "Attachment" ---
INSERT INTO Topic VALUES ('tt-assoctype', 1, 1, 'at-attachment', 'Attachment');
INSERT INTO TopicProp VALUES ('at-attachment', 1, 'Name', 'Attachment');
INSERT INTO TopicProp VALUES ('at-attachment', 1, 'Plural Name', 'Attachments');
INSERT INTO TopicProp VALUES ('at-attachment', 1, 'Color', '#408000');


--- create topic type "Recipient List" ---

INSERT INTO Topic VALUES ('tt-topictype', 1, 1, 'tt-recipientlist', 'Recipient List');
INSERT INTO TopicProp VALUES ('tt-recipientlist', 1, 'Name', 'Recipient List');
INSERT INTO TopicProp VALUES ('tt-recipientlist', 1, 'Plural Name', 'Recipient Lists');
INSERT INTO TopicProp VALUES ('tt-recipientlist', 1, 'Description', '<html><head></head><body><p>A <i>Recipient List</i> is ...</p></body></html>');
INSERT INTO TopicProp VALUES ('tt-recipientlist', 1, 'Description Query', 'What is a "Recipient List"?');
INSERT INTO TopicProp VALUES ('tt-recipientlist', 1, 'Icon', 'authentificationsource.gif');
-- INSERT INTO TopicProp VALUES ('tt-recipientlist', 1, 'Creation Icon', 'createKompetenzstern.gif');
-- INSERT INTO TopicProp VALUES ('tt-recipientlist', 1, 'Unique Topic Names', 'on');
INSERT INTO TopicProp VALUES ('tt-recipientlist', 1, 'Custom Implementation', 'de.deepamehta.topics.RecipientListTopic');
-- super type
INSERT INTO Association VALUES ('at-derivation', 1, 1, 'a-340', '', 'tt-generic', 1, 'tt-recipientlist', 1);
-- search type
INSERT INTO Topic VALUES ('tt-topictype', 1, 1, 'tt-recipientlist-search', 'Recipient List Search');
INSERT INTO TopicProp VALUES ('tt-recipientlist-search', 1, 'Name', 'Recipient List Search');
-- INSERT INTO TopicProp VALUES ('tt-recipientlist-search', 1, 'Icon', 'event-search.gif');
-- derive search type
INSERT INTO Association VALUES ('at-derivation', 1, 1, 'a-341', '', 'tt-topiccontainer', 1, 'tt-recipientlist-search', 1);
-- assign search type to type
INSERT INTO Association VALUES ('at-aggregation', 1, 1, 'a-342', '', 'tt-recipientlist-search', 1, 'tt-recipientlist', 1);

-- create relation from "Email" to "Recipient List"

INSERT INTO Association VALUES ('at-relation', 1, 1, 'a-343', '', 'tt-email', 1, 'tt-recipientlist', 1);
INSERT INTO AssociationProp VALUES ('a-343', 1, 'Cardinality', 'many');
INSERT INTO AssociationProp VALUES ('a-343', 1, 'Association Type ID', 'at-recipient');
INSERT INTO AssociationProp VALUES ('a-343', 1, 'Web Info', 'Related Topic Name');
INSERT INTO AssociationProp VALUES ('a-343', 1, 'Web Form', 'Related Topic Selector');
INSERT INTO AssociationProp VALUES ('a-343', 1, 'Ordinal Number', '35');

--- create association type: "Attachment" ---

INSERT INTO Topic VALUES ('tt-assoctype', 1, 1, 'at-attachment', 'Attachment');
INSERT INTO TopicProp VALUES ('at-attachment', 1, 'Name', 'Attachment');
INSERT INTO TopicProp VALUES ('at-attachment', 1, 'Plural Name', 'Attachments');
INSERT INTO TopicProp VALUES ('at-attachment', 1, 'Color', '#408000');

-- remove association types "attachement" and "e-mail addressee"

DELETE FROM Topic WHERE ID='at-attachement';
DELETE FROM TopicProp WHERE TopicID='at-attachement';
DELETE FROM ViewTopic WHERE TopicID='at-attachement';
DELETE FROM Association WHERE TypeID='at-attachement';

DELETE FROM Topic WHERE ID='at-emailaddressee';
DELETE FROM TopicProp WHERE TopicID='at-emailaddressee';
DELETE FROM ViewTopic WHERE TopicID='at-emailaddressee';
DELETE FROM Association WHERE TypeID='at-emailaddressee';

--- create association type: "Attachment" ---

--- remove supertype of "Recipient" and "Sender" ---
--- DELETE FROM Association WHERE ID='a-327';
--- DELETE FROM AssociationProp WHERE AssociationID='a-327';
--- DELETE FROM ViewAssociation WHERE AssociationID='a-327';
--- DELETE FROM Association WHERE ID='a-329';
--- DELETE FROM AssociationProp WHERE AssociationID='a-329';
--- DELETE FROM ViewAssociation WHERE AssociationID='a-329';


--- create property "Recipient Type" ---
INSERT INTO Topic VALUES ('tt-property', 1, 1, 'pp-recipienttype', 'Recipient Type');
INSERT INTO TopicProp VALUES ('pp-recipienttype', 1, 'Name', 'Recipient Type');
INSERT INTO TopicProp VALUES ('pp-recipienttype', 1, 'Visualization', 'Options Menu');
INSERT INTO TopicProp VALUES ('pp-recipienttype', 1, 'Default Value', 'To');
-- assign property to "Recipient"
INSERT INTO Association VALUES ('at-composition', 1, 1, 'a-347', '', 'at-recipient', 1, 'pp-recipienttype', 1);
INSERT INTO AssociationProp VALUES ('a-347', 1, 'Ordinal Number', '50');
-- create property values "To", "Cc", "Bcc"
INSERT INTO Topic VALUES ('tt-constant', 1, 1, 't-recipienttype-to', 'To');
INSERT INTO TopicProp VALUES ('t-recipienttype-to', 1, 'Name', 'To');
INSERT INTO Topic VALUES ('tt-constant', 1, 1, 't-recipienttype-cc', 'Cc');
INSERT INTO TopicProp VALUES ('t-recipienttype-cc', 1, 'Name', 'Cc');
INSERT INTO Topic VALUES ('tt-constant', 1, 1, 't-recipienttype-bcc', 'Bcc');
INSERT INTO TopicProp VALUES ('t-recipienttype-bcc', 1, 'Name', 'Bcc');
-- assign property values to "Recipient Type"
INSERT INTO Association VALUES ('at-composition', 1, 1, 'a-344', '', 'pp-recipienttype', 1, 't-recipienttype-to', 1);
INSERT INTO Association VALUES ('at-composition', 1, 1, 'a-345', '', 'pp-recipienttype', 1, 't-recipienttype-cc', 1);
INSERT INTO Association VALUES ('at-composition', 1, 1, 'a-346', '', 'pp-recipienttype', 1, 't-recipienttype-bcc', 1);
INSERT INTO AssociationProp VALUES ('a-344', 1, 'Ordinal Number', '1');
INSERT INTO AssociationProp VALUES ('a-345', 1, 'Ordinal Number', '2');
INSERT INTO AssociationProp VALUES ('a-346', 1, 'Ordinal Number', '3');


--- set custom implementation for "Person Search", "Institution", and "Institution Search" ---
INSERT INTO TopicProp VALUES ('tt-personcontainer', 1, 'Custom Implementation', 'de.deepamehta.topics.PersonSearchTopic');
INSERT INTO TopicProp VALUES ('tt-institution', 1, 'Custom Implementation', 'de.deepamehta.topics.InstitutionTopic');
INSERT INTO TopicProp VALUES ('tt-institutioncontainer', 1, 'Custom Implementation', 'de.deepamehta.topics.InstitutionSearchTopic');

