/* Copyright © WebServices pour l'Éducation, 2014
 *
 * This file is part of ENT Core. ENT Core is a versatile ENT engine based on the JVM.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 * For the sake of explanation, any module that communicate over native
 * Web protocols, such as HTTP, with ENT Core is outside the scope of this
 * license and could be license under its own terms. This is merely considered
 * normal use of ENT Core, and does not fall under the heading of "covered work".
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 */

package org.entcore.feeder.aaf;

import org.entcore.feeder.dictionary.structures.DefaultFunctions;
import org.entcore.feeder.dictionary.structures.Importer;
import org.entcore.feeder.dictionary.structures.Structure;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

import static org.entcore.feeder.dictionary.structures.DefaultProfiles.*;

public class StructureImportProcessing extends BaseImportProcessing {

	private final Importer importer = Importer.getInstance();

	protected StructureImportProcessing(String path, Vertx vertx) {
		super(path, vertx);
	}

	@Override
	public void start(final Handler<Message<JsonObject>> handler) {
		createOrUpdateProfiles();
		DefaultFunctions.createOrUpdateFunctions(importer);
		parse(handler, getNextImportProcessing());
	}

	protected ImportProcessing getNextImportProcessing() {
		return new FieldOfStudyImportProcessing(path, vertx);
	}

	private void createOrUpdateProfiles() {
		importer.createOrUpdateProfile(STUDENT_PROFILE);
		importer.createOrUpdateProfile(RELATIVE_PROFILE);
		importer.createOrUpdateProfile(PERSONNEL_PROFILE);
		importer.createOrUpdateProfile(TEACHER_PROFILE);
		importer.createOrUpdateProfile(GUEST_PROFILE);
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
