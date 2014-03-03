package org.entcore.feeder.aaf;

import org.entcore.feeder.Feed;
import org.entcore.feeder.dictionary.structures.Importer;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

public class AafFeeder implements Feed {

	private static final Logger log = LoggerFactory.getLogger(AafFeeder.class);
	private final Vertx vertx;
	private final String path;

	public AafFeeder(Vertx vertx, String path) {
		this.vertx = vertx;
		this.path = path;
	}

	@Override
	public void launch(final Importer importer, final Handler<Message<JsonObject>> handler) throws Exception {
		new StructureImportProcessing(path,vertx).start(handler);
	}

}
