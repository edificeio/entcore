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

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class PersonnelImportProcessing2 extends PersonnelImportProcessing {

	protected PersonnelImportProcessing2(String path, Vertx vertx) {
		super(path, vertx);
	}

	@Override
	protected void preCommit() {
		importer.getPersEducNat().createAndLinkSubjects();
	}

	@Override
	public void start(final Handler<Message<JsonObject>> handler) {
		if (importer.isFirstImport()) {
			parse(handler, new StudentImportProcessing2(path, vertx));
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
		List<String> c = object.getArray("classes") != null ? object.getArray("classes").toList() : new LinkedList<String>();
		String[][] groups = createGroups(object.getArray("groups"), c);
		String[][] classes = createClasses(new JsonArray(c));
		JsonArray functions = object.getArray("functions");
		JsonArray structuresByFunctions = null;
		if (functions != null) {
			Set<String> s = new HashSet<>();
			for (Object o: functions) {
				if (!(o instanceof String) || !o.toString().contains("$")) continue;
				s.add(o.toString().substring(0, o.toString().indexOf('$')));
			}
			structuresByFunctions = new JsonArray(s.toArray());
		}
		importer.createOrUpdatePersonnel(object, detectProfile(object), structuresByFunctions,
				classes, groups, false, true);
	}

}
