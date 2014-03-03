package org.entcore.feeder.aaf;

import org.entcore.feeder.dictionary.structures.Importer;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

public class UserImportProcessing extends BaseImportProcessing {

	private final Importer importer = Importer.getInstance();

	protected UserImportProcessing(String path, Vertx vertx) {
		super(path, vertx);
	}

	@Override
	public void start(final Handler<Message<JsonObject>> handler) {
		if (importer.isFirstImport()) {
			importer.moduleConstraints();
			importer.persist(new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> message) {
					if ("ok".equals(message.body().getString("status"))) {
						parse(handler, new PersonnelImportProcessing(path, vertx));
					} else {
						error(message, handler);
					}
				}
			});
		} else {
			parse(handler, new PersonnelImportProcessing(path, vertx));
		}
	}

	@Override
	public String getMappingResource() {
		return "dictionary/mapping/aaf/PersRelEleve.json";
	}

	@Override
	public void process(JsonObject object) {
		importer.createOrUpdateUser(object);
	}

	@Override
	protected String getFileRegex() {
		return ".*?PersRelEleve_[0-9]{4}\\.xml";
	}

}
