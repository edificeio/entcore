package edu.one.core;

import edu.one.core.infra.Controller;
import edu.one.core.sync.RelativeFactory;
import edu.one.core.sync.SchoolFactory;
import edu.one.core.sync.StaffFactory;
import edu.one.core.sync.StudentFactory;
import edu.one.core.sync.constants.SyncConstants;
import edu.one.core.sync.utils.FileUtils;
import java.io.File;
import java.util.List;
import java.util.ListIterator;
import org.jdom2.Document;
import org.jdom2.Element;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

public class Sync extends Controller {

	@Override
	public void start() throws Exception {
		log = container.getLogger();
		log.info(container.getConfig().getString("test"));
		config = container.getConfig();

		// Get the files from the input folder
		String rootFolderPathStr = container.getConfig().getString("input-files-folder");
		log.info(rootFolderPathStr);
		String [] files = FileUtils.getFiles(rootFolderPathStr);
		
		// Schools files
		ListIterator < File > schoolsFilesListIterator = FileUtils.getFilesByType(
			   files, SyncConstants.SCHOOL_FILTER, rootFolderPathStr).listIterator();
		
		// Processing files
		while (schoolsFilesListIterator.hasNext()) {
		    File currentSchoolsFile = schoolsFilesListIterator.next();
		    log.info("Current file : " + currentSchoolsFile.getName());
		    
		    // Turn file into jdom xml document
		    Document document = FileUtils.getXmlDocument(currentSchoolsFile);
		    List < Element > elementsList = (List < Element >) document.getRootElement().getChildren();
		    log.info("Number of elements to process : " + elementsList.size());
		    
		    // Processing elements
		    for (Element element : elementsList) {
			   String processingStr = element.getName();
			   if (processingStr != null) {
				  switch (processingStr) {
				  	 case SyncConstants.ADD_TAG:
						SchoolFactory.addSchool(element);
						break;
				  	 case SyncConstants.UPDATE_TAG:
						SchoolFactory.updateSchool(element);
						break;
				  	 case SyncConstants.DELETE_TAG:
						SchoolFactory.deleteSchool(element);
						break;
				  	 default:
						log.warn("Unkown processing type.");
						break;
				  }
			   } else {
				  log.warn("Empty element.");
			   }
		    }
		}
		
		// Relatives files
		ListIterator < File > relativesFilesListIterator = FileUtils.getFilesByType(
			   files, SyncConstants.RELATIVES_FILTER, rootFolderPathStr).listIterator();
		
		// Processing files
		while (relativesFilesListIterator.hasNext()) {
		    File currentRelativesFile = relativesFilesListIterator.next();
		    log.info("Current file : " + currentRelativesFile.getName());
		    
		    // Turn file into jdom xml document
		    Document document = FileUtils.getXmlDocument(currentRelativesFile);
		    List < Element > elementsList = (List < Element >) document.getRootElement().getChildren();
		    log.info("Number of elements to process : " + elementsList.size());
		    
		    // Processing elements
		    for (Element element : elementsList) {
			   String processingStr = element.getName();
			   if (processingStr != null) {
				  switch (processingStr) {
				  	 case SyncConstants.ADD_TAG:
						RelativeFactory.addRelative(element);
						break;
				  	 case SyncConstants.UPDATE_TAG:
						RelativeFactory.updateRelative(element);
						break;
				  	 case SyncConstants.DELETE_TAG:
						RelativeFactory.deleteRelative(element);
						break;
				  	 default:
						log.warn("Unkown processing type.");
						break;
				  }
			   } else {
				  log.warn("Empty element.");
			   }
		    }
		}
		
		// Students files
		ListIterator < File > studentsFilesListIterator = FileUtils.getFilesByType(
			   files, SyncConstants.STUDENT_FILTER, rootFolderPathStr).listIterator();
		
		// Processing files
		while (studentsFilesListIterator.hasNext()) {
		    File currentStudentsFile = studentsFilesListIterator.next();
		    log.info("Current file : " + currentStudentsFile.getName());
		    
		    // Turn file into jdom xml document
		    Document document = FileUtils.getXmlDocument(currentStudentsFile);
		    List < Element > elementsList = (List < Element >) document.getRootElement().getChildren();
		    log.info("Number of elements to process : " + elementsList.size());
		    
		    // Processing elements
		    for (Element element : elementsList) {
			   String processingStr = element.getName();
			   if (processingStr != null) {
				  switch (processingStr) {
				  	 case SyncConstants.ADD_TAG:
						StudentFactory.addStudent(element);
						break;
				  	 case SyncConstants.UPDATE_TAG:
						StudentFactory.updateStudent(element);
						break;
				  	 case SyncConstants.DELETE_TAG:
						StudentFactory.deleteStudent(element);
						break;
				  	 default:
						log.warn("Unkown processing type.");
						break;
				  }
			   } else {
				  log.warn("Empty element.");
			   }
		    }
		}
		
		// Staff files
		ListIterator < File > staffFilesListIterator = FileUtils.getFilesByType(
			   files, SyncConstants.STAFF_FILTER, rootFolderPathStr).listIterator();
		
		// Processing files
		while (staffFilesListIterator.hasNext()) {
		    File currentStaffFile = staffFilesListIterator.next();
		    log.info("Current file : " + currentStaffFile.getName());
		    
		    // Turn file into jdom xml document
		    Document document = FileUtils.getXmlDocument(currentStaffFile);
		    List < Element > elementsList = (List < Element >) document.getRootElement().getChildren();
		    log.info("Number of elements to process : " + elementsList.size());
		    
		    // Processing elements
		    for (Element element : elementsList) {
			   String processingStr = element.getName();
			   if (processingStr != null) {
				  switch (processingStr) {
				  	 case SyncConstants.ADD_TAG:
						StaffFactory.addStaffMember(element);
						break;
				  	 case SyncConstants.UPDATE_TAG:
						StaffFactory.updateStaffMember(element);
						break;
				  	 case SyncConstants.DELETE_TAG:
						StaffFactory.deleteStaffMember(element);
						break;
				  	 default:
						log.warn("Unkown processing type.");
						break;
				  }
			   } else {
				  log.warn("Empty element.");
			   }
		    }
		}
		
//		vertx.eventBus().send("wse.neo4j.persistor"
//			, new JsonObject().putString("action", "query").putString("query", "START n=node(1) RETURN n"));

		RouteMatcher rm = new RouteMatcher();
		rm.get("sync/data/dev", new Handler<HttpServerRequest> () {
			public void handle(HttpServerRequest req) {

				Buffer setupData = null;
				try {
					setupData = vertx.fileSystem().readFileSync(config.getString("dev-data-file"));
				} catch (Exception ex) {
					log.error("dev-data-file not loaded");
					req.response.statusCode = 500;
					req.response.end(new JsonObject().putString("error", "dev-data-file not loaded").encode());
				}
				req.response.end();
			}
		});

		vertx.createHttpServer().requestHandler(rm).listen(config.getInteger("port"));
	}
}
