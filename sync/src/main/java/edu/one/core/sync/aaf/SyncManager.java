/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.one.core.sync.aaf;

import edu.one.core.datadictionary.dictionary.DefaultDictionary;
import edu.one.core.infra.TracerHelper;
import edu.one.core.sync.aaf.AafConstantes;
import edu.one.core.sync.aaf.AafGeoffHelper;
import edu.one.core.sync.aaf.AafSaxContentHandler;
import edu.one.core.sync.aaf.WordpressHelper;
import java.io.FileReader;
import java.util.List;
import org.vertx.java.core.Vertx;
import org.vertx.java.platform.Container;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

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
				vertx, container, "../edu.one.core~dataDictionary~0.1.0-SNAPSHOT/aaf-dictionary.json"));
		wordpressHelper = new WordpressHelper(trace, vertx.eventBus());
		aafGeoffHelper = new AafGeoffHelper(trace, vertx.eventBus(), wordpressHelper);
		try {
			xr = XMLReaderFactory.createXMLReader();
		} catch (SAXException ex) {
			trace.error(ex.getMessage());
		}
		xr.setContentHandler(aafSaxHandler);

	}

	public int[] syncAaf(String inputFolder) throws Exception {
		// Parsing all xml aaf files in the specified input folder
		for (String filter : AafConstantes.AAF_FILTERS) {
		String [] files = vertx.fileSystem().readDirSync(
				inputFolder, filter);
			for (String filePath : files) {
				xr.parse(new InputSource(new FileReader(filePath)));
			}
		}

		// Parse all operations for the current import
		for (Operation operation : aafSaxHandler.operations) {
			// création du noeud associé à l'opération avec ses attributs
			aafGeoffHelper.addOperation(operation);
			// création des objets dans Wordpress
			wordpressHelper.addEntity(operation);

			// création des relations et des groupements éventuels
			addRelationships(operation);
		}

		// send geoff request
		aafGeoffHelper.sendRequest();
		// Send WP requests
		wordpressHelper.send();

		// reset objects
		int[] cr = {aafGeoffHelper.reset(),aafSaxHandler.operationsInvalides.size()};
		aafSaxHandler.reset();
		wordpressHelper.reset();
		return cr;
	}

	public void addRelationships(Operation op) {
		if (op.typeEntite == Operation.TypeEntite.ELEVE
				|| op.typeEntite == Operation.TypeEntite.PERSEDUCNAT) {
			// TODO : refactorer?
			if (op.attributs.containsKey(AafConstantes.CLASSES_ATTR)) {
				addGroups(op,Operation.TypeEntite.CLASSE,AafConstantes.CLASSES_ATTR);
			}
			if (op.attributs.containsKey(AafConstantes.GROUPES_ATTR)) {
				addGroups(op,Operation.TypeEntite.GROUPE,AafConstantes.GROUPES_ATTR);
			}
			if (op.attributs.containsKey(AafConstantes.ECOLE_ATTR)) {
				aafGeoffHelper.relEcoles(op);
			}
			if (op.typeEntite == Operation.TypeEntite.ELEVE &&
					op.attributs.containsKey(AafConstantes.PARENTS_ATTR)) {
				aafGeoffHelper.relParents(op);
			}
		}
	}

	public void addGroups(Operation userOp, Operation.TypeEntite grpType, String grpAttr) {
		List<Operation> groupsOperations = aafGeoffHelper.relGroupements(userOp, grpType, grpAttr);
		for (Operation opGr : groupsOperations) {
			wordpressHelper.addEntity(opGr);
		}
	}
}
