-----------------------
--- New Topic Types ---
-----------------------

--- "Umrisspunkt" ---
INSERT INTO Topic VALUES ('tt-topictype', 1, 1, 'tt-ka-outlinepoint', 'Umrisspunkt');
INSERT INTO TopicProp VALUES ('tt-ka-outlinepoint', 1, 'Name', 'Umrisspunkt');
INSERT INTO TopicProp VALUES ('tt-ka-outlinepoint', 1, 'Plural Name', 'Umrisspunkte');
INSERT INTO TopicProp VALUES ('tt-ka-outlinepoint', 1, 'Description', '<html><body><p>Ein <i>Umrisspunkt</i> ist ...</p></body></html>');
INSERT INTO TopicProp VALUES ('tt-ka-outlinepoint', 1, 'Description Query', 'Was ist ein Umrisspunkt?');
INSERT INTO TopicProp VALUES ('tt-ka-outlinepoint', 1, 'Icon', 'crosspoint.gif');
-- INSERT INTO TopicProp VALUES ('tt-ka-outlinepoint', 1, 'Creation Icon', 'createKompetenzstern.gif');
-- INSERT INTO TopicProp VALUES ('tt-ka-outlinepoint', 1, 'Unique Topic Names', 'on');
INSERT INTO TopicProp VALUES ('tt-ka-outlinepoint', 1, 'Custom Implementation', 'de.kiezatlas.deepamehta.topics.OutlinePointTopic');
-- assign properties
-- INSERT INTO Association VALUES ('at-composition', 1, 1, 'a-ka-68', '', 'tt-ka-outlinepoint', 1, 'pp-ka-outlinepoint', 1);
-- INSERT INTO AssociationProp VALUES ('a-ka-68', 1, 'Ordinal Number', '10');
-- super type
-- INSERT INTO Association VALUES ('at-derivation', 1, 1, 'a-ka-50', '', 'tt-generic', 1, 'tt-ka-outlinepoint', 1);
-- search type
INSERT INTO Topic VALUES ('tt-topictype', 1, 1, 'tt-ka-outlinepoint-search', 'Umrisspunkt Suche');
INSERT INTO TopicProp VALUES ('tt-ka-outlinepoint-search', 1, 'Name', 'Umrisspunkt Suche');
-- INSERT INTO TopicProp VALUES ('tt-ka-outlinepoint-search', 1, 'Icon', 'KompetenzsternContainer.gif');
-- derive search type
INSERT INTO Association VALUES ('at-derivation', 1, 1, 'a-ka-70', '', 'tt-topiccontainer', 1, 'tt-ka-outlinepoint-search', 1);
-- assign type to search type
INSERT INTO Association VALUES ('at-aggregation', 1, 1, 'a-ka-71', '', 'tt-ka-outlinepoint-search', 1, 'tt-ka-outlinepoint', 1);

--- "Farbe" ---
INSERT INTO Topic VALUES ('tt-topictype', 1, 1, 'tt-ka-color', 'Farbe');
INSERT INTO TopicProp VALUES ('tt-ka-color', 1, 'Name', 'Farbe');
INSERT INTO TopicProp VALUES ('tt-ka-color', 1, 'Plural Name', 'Farben');
INSERT INTO TopicProp VALUES ('tt-ka-color', 1, 'Description', '<html><body><p>Eine <i>Farbe</i> ist ...</p></body></html>');
INSERT INTO TopicProp VALUES ('tt-ka-color', 1, 'Description Query', 'Was ist eine Farbe?');
INSERT INTO TopicProp VALUES ('tt-ka-color', 1, 'Icon', 'color.gif');
-- INSERT INTO TopicProp VALUES ('tt-ka-color', 1, 'Creation Icon', 'createKompetenzstern.gif');
-- INSERT INTO TopicProp VALUES ('tt-ka-color', 1, 'Unique Topic Names', 'on');
INSERT INTO TopicProp VALUES ('tt-ka-color', 1, 'Custom Implementation', 'de.kiezatlas.deepamehta.topics.ColorTopic');
-- assign properties
INSERT INTO Association VALUES ('at-composition', 1, 1, 'a-ka-73', '', 'tt-ka-color', 1, 'pp-color', 1);
INSERT INTO AssociationProp VALUES ('a-ka-73', 1, 'Ordinal Number', '50');
-- super type
INSERT INTO Association VALUES ('at-derivation', 1, 1, 'a-ka-74', '', 'tt-generic', 1, 'tt-ka-color', 1);
-- search type
INSERT INTO Topic VALUES ('tt-topictype', 1, 1, 'tt-ka-color-search', 'Farben Suche');
INSERT INTO TopicProp VALUES ('tt-ka-color-search', 1, 'Name', 'Farben Suche');
-- INSERT INTO TopicProp VALUES ('tt-ka-color-search', 1, 'Icon', 'KompetenzsternContainer.gif');
-- derive search type
INSERT INTO Association VALUES ('at-derivation', 1, 1, 'a-ka-75', '', 'tt-topiccontainer', 1, 'tt-ka-color-search', 1);
-- assign type to search type
INSERT INTO Association VALUES ('at-aggregation', 1, 1, 'a-ka-76', '', 'tt-ka-color-search', 1, 'tt-ka-color', 1);



----------------------------
--- New Association Type ---
----------------------------

--- "Umriss" ---
INSERT INTO Topic VALUES ('tt-assoctype', 1, 1, 'at-ka-outline', 'Umriss');
INSERT INTO TopicProp VALUES ('at-ka-outline', 1, 'Name', 'Umriss');
INSERT INTO TopicProp VALUES ('at-ka-outline', 1, 'Color', '#ff0000');
-- INSERT INTO TopicProp VALUES ('at-ksmembership', 1, 'Custom Implementation', 'de.deepamehta.kompetenzstern.assocs.KompetenzsternMembership');
-- super type
-- INSERT INTO Association VALUES ('at-derivation', 1, 1, 'a-757', '', 'at-membership', 1, 'at-ksmembership', 1);
-- assign association type to workspace
-- INSERT INTO Association VALUES ('at-uses', 1, 1, 'a-756', '', 't-ks-workspace', 1, 'at-ksmembership', 1);



---------------------------------
--- Assign Types to Workspace ---
---------------------------------

--- Assign "Umrisspunkt" to workspace "Kiez-Atlas" ---
INSERT INTO Association VALUES ('at-uses', 1, 1, 'a-ka-72', '', 't-ka-workspace', 1, 'tt-ka-outlinepoint', 1);
INSERT INTO AssociationProp VALUES ('a-ka-72', 1, 'Access Permission', 'create');

--- Assign "Farbe" to workspace "Kiez-Atlas" ---
INSERT INTO Association VALUES ('at-uses', 1, 1, 'a-ka-77', '', 't-ka-workspace', 1, 'tt-ka-color', 1);
INSERT INTO AssociationProp VALUES ('a-ka-77', 1, 'Access Permission', 'create');



--------------------
--- New Property ---
--------------------

--- "Original Background Image" (Stadtplan) ---
INSERT INTO Topic VALUES ('tt-property', 1, 1, 'pp-ka-original-bgimage', 'Original Background Image');
INSERT INTO TopicProp VALUES ('pp-ka-original-bgimage', 1, 'Name', 'Original Background Image');
INSERT INTO TopicProp VALUES ('pp-ka-original-bgimage', 1, 'Visualization', 'hidden');



--------------------------
--- Update Topic Types ---
--------------------------

-- assign property to "Stadtplan"
INSERT INTO Association VALUES ('at-composition', 1, 1, 'a-ka-78', '', 'tt-ka-stadtplan', 1, 'pp-ka-original-bgimage', 1);
INSERT INTO AssociationProp VALUES ('a-ka-78', 1, 'Ordinal Number', '315');



----------------------------------
--- Content: Predefined Colors ---
----------------------------------

INSERT INTO Topic VALUES ('tt-ka-color', 1, 1, 't-ka-color1', '');
INSERT INTO TopicProp VALUES ('t-ka-color1', 1, 'Color', '#ff0000');
INSERT INTO TopicProp VALUES ('t-ka-color1', 1, 'Icon', 'color-ff0000.png');
INSERT INTO Topic VALUES ('tt-ka-color', 1, 1, 't-ka-color2', '');
INSERT INTO TopicProp VALUES ('t-ka-color2', 1, 'Color', '#00ff00');
INSERT INTO TopicProp VALUES ('t-ka-color2', 1, 'Icon', 'color-00ff00.png');
INSERT INTO Topic VALUES ('tt-ka-color', 1, 1, 't-ka-color3', '');
INSERT INTO TopicProp VALUES ('t-ka-color3', 1, 'Color', '#0000ff');
INSERT INTO TopicProp VALUES ('t-ka-color3', 1, 'Icon', 'color-0000ff.png');
INSERT INTO Topic VALUES ('tt-ka-color', 1, 1, 't-ka-color4', '');
INSERT INTO TopicProp VALUES ('t-ka-color4', 1, 'Color', '#ffff00');
INSERT INTO TopicProp VALUES ('t-ka-color4', 1, 'Icon', 'color-ffff00.png');
INSERT INTO Topic VALUES ('tt-ka-color', 1, 1, 't-ka-color5', '');
INSERT INTO TopicProp VALUES ('t-ka-color5', 1, 'Color', '#00ffff');
INSERT INTO TopicProp VALUES ('t-ka-color5', 1, 'Icon', 'color-00ffff.png');
INSERT INTO Topic VALUES ('tt-ka-color', 1, 1, 't-ka-color6', '');
INSERT INTO TopicProp VALUES ('t-ka-color6', 1, 'Color', '#ff00ff');
INSERT INTO TopicProp VALUES ('t-ka-color6', 1, 'Icon', 'color-ff00ff.png');
