/*
 * Copyright. Tous droits réservés. WebServices pour l’Education.
 */

package org.entcore.feeder.aaf;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

public class PersonnelImportProcessing2 extends PersonnelImportProcessing {

	protected PersonnelImportProcessing2(String path, Vertx vertx) {
		super(path, vertx);
	}

	@Override
	public void start(final Handler<Message<JsonObject>> handler) {
		if (importer.isFirstImport()) {
			importer.userConstraints();
			importer.classConstraints();
			importer.groupConstraints();
			importer.persist(new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> message) {
					if ("ok".equals(message.body().getString("status"))) {
						parse(handler, new StudentImportProcessing2(path, vertx));
					} else {
						error(message, handler);
					}
				}
			});
		} else {
			importer.markMissingUsers(new Handler<Void>() {
				@Override
				public void handle(Void event) {
					parse(handler, new StudentImportProcessing2(path, vertx));
				}
			});
		}
	}

	@Override
	public void process(JsonObject object) {
		String[][] classes = createClasses(object.getArray("classes"));
		String[][] groups = createGroups(object.getArray("groups"));
		importer.createOrUpdatePersonnel(object, detectProfile(object), classes, groups, false, true);
	}

}
