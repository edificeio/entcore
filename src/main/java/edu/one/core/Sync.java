package edu.one.core;

import edu.one.core.sync.aaf.Constantes;
import edu.one.core.sync.aaf.SaxContentHandler;
import java.io.FileReader;
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
		xr.setContentHandler(new SaxContentHandler(log));
		
		for (String filter : Constantes.AAF_FILTERS) {
			String [] files = vertx.fileSystem().readDirSync(
					container.getConfig().getString("input-files-folder"), filter);
			for (String filePath : files) {
				xr.parse(new InputSource(new FileReader(filePath)));
			}
		}
	}
}
