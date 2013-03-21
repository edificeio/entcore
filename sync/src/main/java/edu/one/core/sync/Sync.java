package edu.one.core.sync;

import edu.one.core.infra.Controller;
import edu.one.core.sync.aaf.Constantes;
import edu.one.core.sync.aaf.SaxContentHandler;
import java.io.FileReader;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

public class Sync extends Controller {

	XMLReader xr;
	SaxContentHandler aafSaxHandler;

	@Override
	public void start() throws Exception {
		super.start();
		aafSaxHandler = new SaxContentHandler(log, vertx.eventBus());
		xr = XMLReaderFactory.createXMLReader();
		xr.setContentHandler(aafSaxHandler);

		rm.get("/sync/admin", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				renderView(request);
			}
		});

		rm.get("/sync/admin/aaf/test", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				try {
					long startTest = System.currentTimeMillis();
					test();
					long endTest = System.currentTimeMillis();

					JsonObject jo = new JsonObject().putObject("result",
						new JsonObject()
							.putNumber("temps", (endTest - startTest) / 1000)
							.putNumber("operations", aafSaxHandler.nbOp)
					);
					renderJson(request, jo);
				} catch (Exception ex) {
					renderError(request);
				}
			}
		});
	}


	public void test() throws Exception {

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
