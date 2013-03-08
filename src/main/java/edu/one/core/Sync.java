package edu.one.core;

import edu.one.core.infra.Neo;
import edu.one.core.sync.aaf.Constantes;
import edu.one.core.sync.aaf.Operation;
import edu.one.core.sync.aaf.SaxContentHandler;
import java.io.FileReader;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

public class Sync extends Controller {

	@Override
	public void start() throws Exception {
		log = container.getLogger();
		config = container.getConfig();

		XMLReader xr = XMLReaderFactory.createXMLReader();
		xr.setContentHandler(new SaxContentHandler(log, vertx.eventBus()));
		
		for (String filter : Constantes.AAF_FILTERS) {
			String [] files = vertx.fileSystem().readDirSync(
					container.getConfig().getString("input-files-folder"), filter);
			for (String filePath : files) {
				xr.parse(new InputSource(new FileReader(filePath)));
			}
		}
		
		// envoi en masse de requêtes par paquets de LIMITE_OP opérations
		// TODO : problème après envoi de la 232ème requête
		// désactiver l'envoi unitaire de requêtes : ligne 104 de la classe SaxContentHandler
//		Neo neo = new Neo(vertx.eventBus(), log);
//		int nbRequetes = 0;
//		int nbOp = 0;
//		final int LIMITE_OP = 100;
//		Buffer requeteBatch = new Buffer("");
//		for (Operation operation : SaxContentHandler.operations) {
//			if (nbOp == 0) {
//				requeteBatch.appendString(operation.requeteCreation("CREATE \n"));
//			} else {
//				requeteBatch.appendString(operation.requeteCreation(",\n"));
//			}
//			nbOp++;
//			if (nbOp == LIMITE_OP) {
//				nbRequetes++;
//				log.info("Envoi requete " + nbRequetes);
//				neo.send(requeteBatch.toString());
//				nbOp = 0;
//				requeteBatch = new Buffer("");
//			}
//		}
//		if (!requeteBatch.toString().isEmpty()) {
//			neo.send(requeteBatch.toString());
//		}
	}
}
