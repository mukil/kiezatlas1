
----------------------------
--- Intro GPS Converter ---
----------------------------

--- create topic type "GPS Converter" ---

INSERT INTO Topic VALUES ('tt-topictype', 1, 1, 'tt-ka-gpsconverter', 'GPS Converter');
INSERT INTO TopicProp VALUES ('tt-ka-gpsconverter', 1, 'Name', 'GPS Converter');
INSERT INTO TopicProp VALUES ('tt-ka-gpsconverter', 1, 'Plural Name', 'GPS Converters');
INSERT INTO TopicProp VALUES ('tt-ka-gpsconverter', 1, 'Description', '<html><head></head><body><p>A <i>GPS Converter</i> connected to a Kiezatlas Workspace, transforms all YADEX and YADEY Coordinates of GeoObject subtypes, into additonal LONG and LAT world coordinates...</p></body></html>');
INSERT INTO TopicProp VALUES ('tt-ka-gpsconverter', 1, 'Description Query', 'What is a "GPS Converter"?');
INSERT INTO TopicProp VALUES ('tt-ka-gpsconverter', 1, 'Icon', 'location.png');
-- INSERT INTO TopicProp VALUES ('tt-recipientlist', 1, 'Creation Icon', 'createKompetenzstern.gif');
-- INSERT INTO TopicProp VALUES ('tt-recipientlist', 1, 'Unique Topic Names', 'on');
INSERT INTO TopicProp VALUES ('tt-ka-gpsconverter', 1, 'Custom Implementation', 'de.kiezatlas.deepamehta.topics.GPSConverterTopic');
-- super type
INSERT INTO Association VALUES ('at-derivation', 1, 1, 'a-ka-120', '', 'tt-generic', 1, 'tt-ka-gpsconverter', 1);
-- search type
INSERT INTO Topic VALUES ('tt-topictype', 1, 1, 'tt-ka-gpsconverter-search', 'GPS Converter Search');
INSERT INTO TopicProp VALUES ('tt-ka-gpsconverter-search', 1, 'Name', 'GPS Converter Search');
-- derive search type
INSERT INTO Association VALUES ('at-derivation', 1, 1, 'a-ka-121', '', 'tt-topiccontainer', 1, 'tt-ka-gpsconverter-search', 1);
-- assign search type to type
INSERT INTO Association VALUES ('at-aggregation', 1, 1, 'a-ka-122', '', 'tt-ka-gpsconverter-search', 1, 'tt-ka-gpsconverter', 1);

--- create property "GPS LONG" ---
INSERT INTO Topic VALUES ('tt-property', 1, 1, 'pp-ka-gpslong', 'LONG');
INSERT INTO TopicProp VALUES ('pp-ka-gpslong', 1, 'Name', 'LONG');
INSERT INTO TopicProp VALUES ('pp-ka-gpslong', 1, 'Visualization', 'Input Field');

--- create property "GPS LAT" ---
INSERT INTO Topic VALUES ('tt-property', 1, 1, 'pp-ka-gpslat', 'Lattitude');
INSERT INTO TopicProp VALUES ('pp-ka-gpslat', 1, 'Name', 'LAT');
INSERT INTO TopicProp VALUES ('pp-ka-gpslat', 1, 'Visualization', 'Input Field');

-- assign GPS properties to "GeoObject"
INSERT INTO Association VALUES ('at-composition', 1, 1, 'a-ka-123', '', 'tt-ka-geoobject', 1, 'pp-ka-gpslong', 1);
INSERT INTO Association VALUES ('at-composition', 1, 1, 'a-ka-124', '', 'tt-ka-geoobject', 1, 'pp-ka-gpslat', 1);

-- assign GPS Converter Topic to Adminstration Workgroup
INSERT INTO Association VALUES ('at-uses', 1, 1, 'a-ka-125', '', 't-administrationgroup', 1, 'tt-ka-gpsconverter', 1);