/*
 * Copyright. Tous droits réservés. WebServices pour l’Education.
 */

package org.entcore.feeder.aaf;

import org.entcore.feeder.dictionary.structures.Importer;
import org.entcore.feeder.dictionary.structures.Structure;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

import static org.entcore.feeder.dictionary.structures.DefaultProfiles.*;

public class StructureImportProcessing extends BaseImportProcessing {

	private final Importer importer = Importer.getInstance();

	protected StructureImportProcessing(String path, Vertx vertx) {
		super(path, vertx);
	}

	@Override
	public void start(final Handler<Message<JsonObject>> handler) {
		if (importer.isFirstImport()) {
			importer.structureConstraints();
			importer.profileConstraints();
			importer.functionConstraints();
			importer.persist(new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> message) {
					if ("ok".equals(message.body().getString("status"))) {
						createOrUpdateProfiles();
						parse(handler, new FieldOfStudyImportProcessing(path, vertx));
					} else {
						error(message, handler);
					}
				}
			});
		} else {
			createOrUpdateProfiles();
			parse(handler, new FieldOfStudyImportProcessing(path, vertx));
		}
	}

	private void createOrUpdateProfiles() {
		importer.createOrUpdateProfile(STUDENT_PROFILE);
		importer.createOrUpdateProfile(RELATIVE_PROFILE);
		importer.createOrUpdateProfile(PERSONNEL_PROFILE);
		importer.createOrUpdateProfile(TEACHER_PROFILE);
	}


	@Override
	public String getMappingResource() {
		return "dictionary/mapping/aaf/EtabEducNat.json";
	}

	@Override
	public void process(JsonObject object) {
		Structure structure = importer.createOrUpdateStructure(object);
		if (structure != null) {
			structure.addAttachment();
		}
	}

	@Override
	protected String getFileRegex() {
		return ".*?EtabEducNat_[0-9]{4}\\.xml";
	}

}
