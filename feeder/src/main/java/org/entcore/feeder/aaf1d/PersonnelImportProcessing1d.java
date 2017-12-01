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

import org.entcore.feeder.aaf.ImportProcessing;
import org.entcore.feeder.aaf.PersonnelImportProcessing;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static org.entcore.feeder.dictionary.structures.DefaultProfiles.PERSONNEL_PROFILE_EXTERNAL_ID;
import static org.entcore.feeder.dictionary.structures.DefaultProfiles.TEACHER_PROFILE_EXTERNAL_ID;

public class PersonnelImportProcessing1d extends PersonnelImportProcessing {

	protected PersonnelImportProcessing1d(String path, Vertx vertx) {
		super(path, vertx);
	}

	@Override
	public void start(final Handler<Message<JsonObject>> handler) {
		parse(handler, getNextImportProcessing());
	}

	@Override
	protected String detectProfile(JsonObject object) {
		JsonArray functions = object.getJsonArray("functions");
		if (functions != null && functions.size() > 0) {
			for (Object f : functions) {
				if (!(f instanceof String)) continue;
				String function = (String) f;
				if (function.contains("ENS") || function.contains("DOC")) {
					return TEACHER_PROFILE_EXTERNAL_ID;
				}
			}
		}
		return PERSONNEL_PROFILE_EXTERNAL_ID;
	}

	@Override
	public void process(JsonObject object) {
		String profile = detectProfile(object);
		object.put("profiles", new JsonArray()
				.add((TEACHER_PROFILE_EXTERNAL_ID.equals(profile) ? "Teacher" : "Personnel")));
		String email = object.getString("email");
		if (email != null && !email.trim().isEmpty()) {
			object.put("emailAcademy", email);
		}
		JsonArray functions = object.getJsonArray("functions");
		JsonArray structuresByFunctions = null;
		if (functions != null) {
			Set<String> s = new HashSet<>();
			for (Object o: functions) {
				if (!(o instanceof String) || !o.toString().contains("$")) continue;
				s.add(o.toString().substring(0, o.toString().indexOf('$')));
			}
			structuresByFunctions = new JsonArray(new ArrayList<>(s));
		}
		importer.createOrUpdatePersonnel(object, profile, structuresByFunctions, null, null, true, true);
	}

	@Override
	public String getMappingResource() {
		return "dictionary/mapping/aaf1d/PersEducNat.json";
	}

	@Override
	protected ImportProcessing getNextImportProcessing() {
		return new StudentImportProcessing1d2(path, vertx);
	}

}
