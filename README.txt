
Kiezatlas 1.6.2                                                  Malte Reißig & Jörg Richter
===============                                                                   21.11.2008


Kiezatlas is an application for the DeepaMehta platform.
Kiezatlas relies on a DeepaMehta installation, at the moment it depends on the revision 357


Install Kiezatlas:

1) unzip kiezatlas-1.6.2.zip
2) apply all database patches from db/, begin with kiezatlas.sql
3) copy 2 .war files to tomcat/webapps
4) copy kiezatlas.jar file to tomcat/shared/libs or to a resp. local web-inf/lib
5) copy content of icons/ to deepamehta/install/client/icons
6) copy content of backgrounds/ to deepamehta/install/client/backgrounds

Run Kiezatlas:

1) restart tomcat
2) restart DeepaMehta server. Note: kiezatlas.jar must be in the classpath.
3) login to DeepaMehta as an administrator and set the "Base URL" property.
4) Join the "Kiez-Atlas" workspace

More Information:

www.kiezatlas.de
www.deepamehta.de/docs/kiezatlas.html