package edu.one.core.sync;

import edu.one.core.infra.Controller;
import edu.one.core.sync.aaf.AafConstantes;
import edu.one.core.sync.aaf.AafGeoffHelper;
import edu.one.core.sync.aaf.AafSaxContentHandler;
import java.io.FileReader;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

public class Sync extends Controller {

	XMLReader xr;
	AafSaxContentHandler aafSaxHandler;
	AafGeoffHelper aafGeoffHelper;

	@Override
	public void start() throws Exception {
		super.start();
		aafSaxHandler = new AafSaxContentHandler(log);
		aafGeoffHelper = new AafGeoffHelper(log, vertx.eventBus());
		xr = XMLReaderFactory.createXMLReader();
		xr.setContentHandler(aafSaxHandler);

		rm.get("/admin", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				renderView(request);
			}
		});

		rm.get("/admin/aaf/test", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				try {
					long startTest = System.currentTimeMillis();
					int nbOps = test();
					long endTest = System.currentTimeMillis();

					JsonObject jo = new JsonObject().putObject("result",
						new JsonObject()
							.putString("temps", (endTest - startTest) + " ms")
							.putNumber("operations", nbOps)
					);
					renderJson(request, jo);
				} catch (Exception ex) {
					ex.printStackTrace();
					renderError(request);
				}
			}
		});
	}

	public int test() throws Exception {
		// Parsing of xml aaf files
		for (String filter : AafConstantes.AAF_FILTERS) {
		String [] files = vertx.fileSystem().readDirSync(
				config.getString("input-files-folder"), filter);
			for (String filePath : files) {
				xr.parse(new InputSource(new FileReader(filePath)));
			}
		}
		// Build and send geoff request
		aafGeoffHelper.sendRequest(aafSaxHandler.operations);
		
		// reset objects
		aafSaxHandler.reset();
		return aafGeoffHelper.reset();
	}
}