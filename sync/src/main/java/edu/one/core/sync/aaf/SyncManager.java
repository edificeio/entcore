/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.one.core.sync.aaf;

import java.io.FileReader;
import java.util.List;

import org.vertx.java.core.Vertx;
import org.vertx.java.platform.Container;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import edu.one.core.datadictionary.dictionary.DefaultDictionary;
import edu.one.core.infra.Server;
import edu.one.core.infra.TracerHelper;

/**
 *
 * @author bperez
 */
public class SyncManager {
	private XMLReader xr;
	private AafSaxContentHandler aafSaxHandler;
	private AafGeoffHelper aafGeoffHelper;
	private WordpressHelper wordpressHelper;
	private Vertx vertx;

	public SyncManager (TracerHelper trace, Vertx vertx, Container container) {
		this.vertx = vertx;
		aafSaxHandler = new AafSaxContentHandler(trace, new DefaultDictionary(
				vertx, container, "../edu.one.core~dataDictionary~1.1-SNAPSHOT/aaf-dictionary.json"));
		wordpressHelper = new WordpressHelper(trace, Server.getEventBus(vertx));
		aafGeoffHelper = new AafGeoffHelper(trace, Server.getEventBus(vertx), wordpressHelper);
		try {
			xr = XMLReaderFactory.createXMLReader();
		} catch (SAXException ex) {
			trace.error(ex.getMessage());
		}
		xr.setContentHandler(aafSaxHandler);

	}

	public int[] syncAaf(String inputFolder) throws Exception {
		// parsing all xml aaf files in the specified input folder
		for (String filter : AafConstantes.AAF_FILTERS) {
		String [] files = vertx.fileSystem().readDirSync(
				inputFolder, filter);
			for (String filePath : files) {
				xr.parse(new InputSource(new FileReader(filePath)));
			}
		}

		// parse all operations for the current import
		for (Operation operation : aafSaxHandler.operations) {
			// create node in neo4j for current operation
			aafGeoffHelper.addOperation(operation);
			// create objects in Wordpress for current operation
			wordpressHelper.addEntity(operation);
			// create relationships with other objects
			addRelationships(operation);
		}

		// send geoff request
		aafGeoffHelper.sendRequest();
		// send WP requests
		wordpressHelper.send();

		// reset objects
		int[] cr = {aafGeoffHelper.reset(),aafSaxHandler.invalidOperations.size()};
		aafSaxHandler.reset();
		wordpressHelper.reset();
		return cr;
	}

	public void addRelationships(Operation op) {
		// search and create relationships (groups, classes, schools, ...)
		// depending on entity type and potential links
		// TODO : refactorer?
		if (op.typeEntite == Operation.TypeEntite.ELEVE
				|| op.typeEntite == Operation.TypeEntite.PERSEDUCNAT) {
			if (op.attributs.containsKey(AafConstantes.CLASSES_ATTR)) {
				// create classes and links in neo4j and Wordpress
				addGroups(op,Operation.TypeEntite.CLASSE,AafConstantes.CLASSES_ATTR);
			}
			if (op.attributs.containsKey(AafConstantes.GROUPES_ATTR)) {
				// create groups and links in neo4j and Wordpress
				addGroups(op,Operation.TypeEntite.GROUPE,AafConstantes.GROUPES_ATTR);
			}
			if (op.attributs.containsKey(AafConstantes.ECOLE_ATTR)) {
				// create school link in neo4j
				aafGeoffHelper.relEcoles(op);
			}
			if (op.typeEntite == Operation.TypeEntite.ELEVE &&
					op.attributs.containsKey(AafConstantes.PARENTS_ATTR)) {
				// create parents links in neo4j
				aafGeoffHelper.relParents(op);
			}
		}
	}

	public void addGroups(Operation userOp, Operation.TypeEntite grpType, String grpAttr) {
		// create groups and links with user in neo4j, and get list of created groups
		List<Operation> groupsOperations = aafGeoffHelper.relGroupements(userOp, grpType, grpAttr);
		// parse created groups to create the in Wordpress
		for (Operation opGr : groupsOperations) {
			wordpressHelper.addEntity(opGr);
		}
	}
}
