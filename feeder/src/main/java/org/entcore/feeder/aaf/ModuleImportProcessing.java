/*
 * Copyright. Tous droits réservés. WebServices pour l’Education.
 */

package org.entcore.feeder.aaf;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

public class ModuleImportProcessing extends BaseImportProcessing {

	protected ModuleImportProcessing(String path, Vertx vertx) {
		super(path, vertx);
	}

	@Override
	public void start(final Handler<Message<JsonObject>> handler) {
		if (importer.isFirstImport()) {
			importer.fieldOfStudyConstraints();
			importer.persist(new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> message) {
					if ("ok".equals(message.body().getString("status"))) {
						parse(handler, new UserImportProcessing(path, vertx));
					} else {
						error(message, handler);
					}
				}
			});
		} else {
			parse(handler, new UserImportProcessing(path, vertx));
		}
	}

	@Override
	public String getMappingResource() {
		return "dictionary/mapping/aaf/MefEducNat.json";
	}

	@Override
	public void process(JsonObject object) {
		importer.createOrUpdateModule(object);
	}

	@Override
	protected String getFileRegex() {
		return  ".*?MefEducNat_[0-9]{4}\\.xml";
	}

}
