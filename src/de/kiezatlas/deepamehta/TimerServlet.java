package de.kiezatlas.deepamehta;

import de.deepamehta.service.Session;
import de.deepamehta.service.CorporateMemory;
import de.deepamehta.service.ApplicationService;
import de.deepamehta.service.ApplicationServiceHost;
import de.deepamehta.service.CorporateDirectives;
import de.deepamehta.service.PojoApplicationServiceProvider;

import java.io.IOException;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.quartz.Scheduler;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.quartz.TriggerUtils;
import org.quartz.SchedulerFactory;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.SchedulerException;

/**
 * Timer based Ehrenamt Schnittstelle 1.0
 * is a thread based worker which imports single projects and all occuring criterias resp. categories
 * from an xml interface into one kiezatlas workspace - it reuses topics (e.g. addresstopics) known to
 * the cm (by name), triggers a geolocation on each project with nice address
 *
 * @author Malte Reißig (mre@deepamehta.de)
 */
public class TimerServlet implements Servlet, ApplicationServiceHost {

    Scheduler scheduler = null;
    ServletContext sc = null;
    ApplicationService as = null;
    CorporateMemory cm = null;
    //
    PojoApplicationServiceProvider serviceProvider;
    //
    public static final long UPDATE_INTERVAL = 86000000; // approximately 24 hours
    public static final long UPDATE_INTERVAL_TESTING = 3600000; // approximately 60 mins
    //
    public static final String ENGAGEMENT_SERVICE_URL = "http://buerger-aktiv.index.de/kiezatlas/";
    public static final String ENGAGEMENT_WORKSPACE = "t-331306";
    public static final String CITYMAP_TO_PUBLISH = "t-331302";
    //
    public static final String EVENT_SERVICE_URL = "http://www.berlin.de/land/kalender/export_kiezatlas.php";
    public static final String EVENTMENT_WORKSPACE = "t-453282";
    public static final String EVENTMAP_TO_PUBLISH = "t-453286";

    public void init(ServletConfig config) throws ServletException {
        System.out.println("DEBUG: Starting to initialize the TimerServlet...");
        sc = config.getServletContext();
        // --- create application service ---
        String service = sc.getInitParameter("service");
        try {
            // ApplicationServiceInstance instance = ApplicationServiceInstance.lookup(service, "../config/dm.properties");
            // as = ApplicationService.create(this, instance); // throws DME ### servlet is not properly inited
            SchedulerFactory sf = new StdSchedulerFactory();
            serviceProvider = new PojoApplicationServiceProvider();
            serviceProvider.setServiceName(service);
            serviceProvider.startup();
            as = serviceProvider.getApplicationService();
            cm = as.cm;
            scheduler = sf.getScheduler();
            scheduler.start();
            schedule();
        } catch (Exception e){
            System.out.println("ERROR: while initializing the TimerServlet and or Quartz Framework...");
            e.printStackTrace();
        }
        // System.out.println("INFO: Quartz Framework initialized correctly.. for 2 Jobs for instance " + service);
    }

    public void schedule() {
        TimerJobParameterHolder.getInstance().getParameters().put("as", as);
        TimerJobParameterHolder.getInstance().getParameters().put("cm", cm);
        TimerJobParameterHolder.getInstance().getParameters().put("directives", new CorporateDirectives());
        // both jobs use one jobParameterHolder
        TimerJobParameterHolder.getInstance().getParameters().put("engagementWorkspaceId", ENGAGEMENT_WORKSPACE);
        TimerJobParameterHolder.getInstance().getParameters().put("engagementCityMapId", CITYMAP_TO_PUBLISH);
        TimerJobParameterHolder.getInstance().getParameters().put("engagementServiceUrl", ENGAGEMENT_SERVICE_URL);
        TimerJobParameterHolder.getInstance().getParameters().put("eventWorkspaceId", EVENTMENT_WORKSPACE);
        TimerJobParameterHolder.getInstance().getParameters().put("eventCityMapId", EVENTMAP_TO_PUBLISH);
        TimerJobParameterHolder.getInstance().getParameters().put("eventServiceUrl", EVENT_SERVICE_URL);
        //
        JobDetail engagementJob = new JobDetail("engagement", "importer", TimedEngagementImporter.class);
        JobDetail eventJob = new JobDetail("event", "importer", TimedEventImporter.class);
        Trigger indexdeTrigger = TriggerUtils.makeDailyTrigger(1, 45);
        // Trigger indexdeTrigger = TriggerUtils.makeMinutelyTrigger(15, 2);
        Trigger eventTrigger = TriggerUtils.makeDailyTrigger(6, 15);
        // Trigger eventTrigger = TriggerUtils.makeMinutelyTrigger(5, 2);
        indexdeTrigger.setName("engagement");
        indexdeTrigger.setGroup("importer");
        eventTrigger.setName("event");
        eventTrigger.setGroup("importer");
        try {
        scheduler.scheduleJob(engagementJob, indexdeTrigger);
        scheduler.scheduleJob(eventJob, eventTrigger);
        System.out.println("INFO: The Importer Jobs are now scheduled for... 00:45 AM (Project) and 06:15 AM (Event)");
        } catch (SchedulerException ex) {
        ex.printStackTrace();
        }
    }

    public ServletConfig getServletConfig() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void service(ServletRequest sr, ServletResponse sr1) throws ServletException, IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getServletInfo() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void destroy() {
        try {
            // shutdown service and scheduler jobs
            scheduler.shutdown();
            serviceProvider.shutdown();
            TimerJobParameterHolder.getInstance().clearReferences();
            // null references
            serviceProvider = null;
            scheduler = null;
            sc = null;
            as = null;
            cm = null;
            System.out.println("INFO: QuartzTimer and ApplicationService are stopped and destroyed..");
        } catch (Exception ex) {
            System.out.println("ERROR: while stopping the Quartz Framework...");
            ex.printStackTrace();
        }
    }

    public String getCommInfo() {
        return getCommInfo();
    }

    public void sendDirectives(Session sn, CorporateDirectives cd, ApplicationService as, String string, String string1) {
        // do nothing
    }

    public void broadcastChangeNotification(String string) {
        // do nothing
    }

}
