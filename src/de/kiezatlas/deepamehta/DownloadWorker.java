/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.kiezatlas.deepamehta;

import de.deepamehta.BaseTopic;
import de.deepamehta.DeepaMehtaConstants;
import de.deepamehta.service.ApplicationService;
import de.deepamehta.service.CorporateMemory;
import de.deepamehta.service.TopicBean;
import de.deepamehta.service.TopicBeanField;
import de.kiezatlas.deepamehta.topics.CityMapTopic;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.Vector;

public class DownloadWorker extends Thread implements Runnable {

    private ApplicationService as = null;
    private CorporateMemory cm = null;
    private BaseTopic map = null;
    private String mapAlias = "";


    public DownloadWorker(ApplicationService as, CorporateMemory cm, BaseTopic map, String mapAlias) {

        this.cm = cm;
        this.as = as;
        this.map = map;
        this.mapAlias = mapAlias;

    }

    public void run() {
        System.out.println("    >> DownloadWorker was initalized to run for "+map.getName());
        String filePath = mapAlias; // DeepaMehtaConstants.FILESERVER_DOCUMENTS_PATH + 
        // string wille be an UTF-8 Java String filled from within through a Latin 1 MYSQL Connector
        String content = getFileContent(map);
        // write tmp file
        exportFile(filePath, content);
        // now copy tmp file and replace the original file
        // worst case is; former file deleted and .tmp file could not be copied
        try {
            FileInputStream fis = new FileInputStream(filePath+".tmp"); // read input
            InputStreamReader isr = new InputStreamReader(fis, "ISO-8859-1");
            File fileToWrite = new File(filePath);
            fileToWrite.delete(); // delete the former file and start to write the new one
            System.out.println("DownloadWorker.fileToWrite: \""+fileToWrite.getAbsolutePath()+"\" still exists: " + fileToWrite.exists());
            FileOutputStream fout = new FileOutputStream(fileToWrite.getAbsolutePath(), false);
            OutputStreamWriter out = new OutputStreamWriter(fout, "ISO-8859-1");
            while(isr.ready()) {
                out.write(isr.read());
            }
            out.close();
            System.out.println("    >> DownloadWorker sucessfully replaced CityMapData in " + mapAlias);
        } catch (SecurityException sex) {
            // Former File could not be deleted
            System.out.println("*** ListServlet.DownloadWorker.run() : " + sex.getMessage());
        } catch (FileNotFoundException p) {
            // tmp file could not be opened
            System.out.println("*** ListServlet.DownloadWorker.run() :" + p.getMessage());
        } catch (UnsupportedEncodingException uio) {
            // outputStreamWriter does not support encoding
            System.out.println("*** ListServlet.DownloadWorker.run() :" + uio.getMessage());
        } catch (IOException iox) {
            // copying failed
            System.out.println("*** ListServlet.DownloadWorker.run() :" + iox.getMessage());
        } finally {
            // delete the tmp file, too
            File f = new File(filePath+".tmp");
            f.delete(); // cleanup tmp file
        }
    }

    /**
     * should never get a map without topics as a parameter
     * @param map
     * @return
     */
    String getFileContent(BaseTopic map) {
        StringBuffer result = new StringBuffer();
        CityMapTopic mapTopic = (CityMapTopic) as.getLiveTopic(map);
        Vector allElements = cm.getViewTopics(map.getID(), 1, mapTopic.getInstitutionType().getID());
        System.out.println(">>>> collectMapTopics counted "+allElements.size()+" objects of type " + mapTopic.getInstitutionType().getID());
        // create header of csv file
        TopicBean tmp = as.createTopicBean(((BaseTopic)allElements.get(0)).getID(), 1);
        StringBuffer headline = new StringBuffer();
        // Removing 15 Fields
        tmp.removeField(DeepaMehtaConstants.PROPERTY_PASSWORD);
        tmp.removeField(DeepaMehtaConstants.PROPERTY_WEB_ALIAS);
        tmp.removeFieldsContaining(DeepaMehtaConstants.PROPERTY_OWNER_ID);
        tmp.removeFieldsContaining(DeepaMehtaConstants.PROPERTY_LOCKED_GEOMETRY);
        tmp.removeFieldsContaining("Image");
        tmp.removeFieldsContaining("Forum");
        tmp.removeFieldsContaining("Description");
        for (int i = 0; i < tmp.fields.size(); i++) {
            TopicBeanField field = (TopicBeanField) tmp.fields.get(i);
            headline.append(field.label);
            headline.append(createTab());
            // System.out.println("    > Field: " + field.label + " (" + field.type + ") ");
        }
        // System.out.println("> Headline is " + headline.toString());
        result.append(headline);
		//
		Enumeration e = allElements.elements();
		while (e.hasMoreElements()) {
            result.append("\n");
            String topicId = ((BaseTopic) e.nextElement() ).getID();
            TopicBean bean = as.createTopicBean(topicId, 1);
            // Removing 15 Fields
            bean.removeField(DeepaMehtaConstants.PROPERTY_PASSWORD);
            bean.removeField(DeepaMehtaConstants.PROPERTY_WEB_ALIAS);
            bean.removeFieldsContaining(DeepaMehtaConstants.PROPERTY_OWNER_ID);
            bean.removeFieldsContaining(DeepaMehtaConstants.PROPERTY_LOCKED_GEOMETRY);
            bean.removeFieldsContaining("Image");
            bean.removeFieldsContaining("Forum");
            bean.removeFieldsContaining("Description");
            for (int i = 0; i < bean.fields.size(); i++) {
                TopicBeanField field = (TopicBeanField) bean.fields.get(i);
                if (field.type == TopicBeanField.TYPE_SINGLE) {
                    String toAppend = cleanFieldForExport(field.value);
                    result.append(toAppend);
                } else {
                    for (int a = 0; a < field.values.size(); a++) {
                        BaseTopic fieldTopic = (BaseTopic) field.values.get(a);
                        String toAppend = cleanFieldForExport(fieldTopic.getName());
                        result.append(toAppend);
                        result.append(" ");
                    }
                }
                result.append(createTab());
            }
        }
        return result.toString();
    }

    private String createTab() {
		return "\t";
	}

    private String cleanFieldForExport(String content) {
        //
        content = content.replaceAll("\t", " "); // tab
        content = content.replaceAll("\n", " "); // newline
        content = content.replaceAll("\r", " "); // carriage return
        //
        return content;
    }

    void exportFile(String filePath, String content) {
            try {
                // System.out.println(">>>> DownloadWorker.exportFile(): " + filePath);
                File fileToWrite = new File(filePath+".tmp");
                FileOutputStream fout = new FileOutputStream(fileToWrite, true);
                OutputStreamWriter out = new OutputStreamWriter(fout, "ISO-8859-1");
                // OutputStreamWriter out = new OutputStreamWriter(fout ,"UTF-8");
                out.write(content);
                out.close();
                System.out.println("  > temporary file \"" + fileToWrite + "\" successfully written");
            } catch (FileNotFoundException e) {
                System.out.println("*** ListServlet.DownloadWorker.exportFile(): Trying Again " + e.toString());
                // FileWriter fw = new FileWriter();
            } catch (Exception e) {
                System.out.println("*** ListServlet.DownloadWorker.exportFile(): " + e);
            }
        // return fileTo;
    }
    
}
