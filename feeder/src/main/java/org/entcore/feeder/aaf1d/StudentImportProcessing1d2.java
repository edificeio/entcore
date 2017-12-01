/*
 * Copyright © WebServices pour l'Éducation, 2015
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
 */

package org.entcore.feeder.aaf1d;

import org.entcore.feeder.aaf.StudentImportProcessing2;
import org.entcore.feeder.dictionary.structures.Structure;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class StudentImportProcessing1d2 extends StudentImportProcessing2 {

	protected StudentImportProcessing1d2(String path, Vertx vertx) {
		super(path, vertx);
	}

	@Override
	protected void preCommit() {
		super.preCommit();
		importer.removeOldFunctionalGroup();
		importer.removeEmptyClasses();
		importer.restorePreDeletedUsers();
	}

	@Override
	public void start(final Handler<Message<JsonObject>> handler) {
		initAcademyPrefix(path);
		importer.markMissingUsers(null, getAcademyPrefix(), new Handler<Void>() {
			@Override
			public void handle(Void event) {
				parse(handler, null);
			}
		});
	}

	@Override
	public String getMappingResource() {
		return "dictionary/mapping/aaf1d/Eleve.json";
	}

	@Override
	protected JsonArray parseRelativeField(JsonArray relative) {
		return StudentImportProcessing1d.parseRelativeField1d(relative);
	}

	@Override
	protected String[][] createClasses(JsonArray classes) {
		String [][] linkStructureClasses = null;
		if (classes != null && classes.size() > 0) {
			linkStructureClasses = new String[classes.size()][2];
			int i = 0;
			for (Object o : classes) {
				if (!(o instanceof String)) continue;
				String [] c = ((String) o).split("\\$");
				if (c.length == 5) {
					Structure s = importer.getStructure(c[0]);
					if (s != null) {
						String classExternalId = c[3];
						s.createClassIfAbsent(classExternalId, c[4]);
						linkStructureClasses[i][0] = s.getExternalId();
						linkStructureClasses[i++][1] = classExternalId;
					}
				}
			}
		}
		return linkStructureClasses;
	}

}
