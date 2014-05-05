/*
 * Copyright. Tous droits réservés. WebServices pour l’Education.
 */

package org.entcore.feeder.aaf;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

public class FieldOfStudyImportProcessing extends BaseImportProcessing {


	protected FieldOfStudyImportProcessing(String path, Vertx vertx) {
		super(path, vertx);
	}

	@Override
	public void start(Handler<Message<JsonObject>> handler) {
		parse(handler, new ModuleImportProcessing(path, vertx));
	}

	@Override
	public String getMappingResource() {
		return "dictionary/mapping/aaf/MatEducNat.json";
	}

	@Override
	public void process(JsonObject object) {
		importer.createOrUpdateFieldOfStudy(object);
	}

	@Override
	protected String getFileRegex() {
		return ".*?MatiereEducNat_[0-9]{4}\\.xml";
	}
}
