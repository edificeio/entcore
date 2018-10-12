/*
 * Copyright © "Open Digital Education", 2015
 *
 * This program is published by "Open Digital Education".
 * You must indicate the name of the software and the company in any production /contribution
 * using the software and indicate on the home page of the software industry in question,
 * "powered by Open Digital Education" with a reference to the website: https://opendigitaleducation.com/.
 *
 * This program is free software, licensed under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation, version 3 of the License.
 *
 * You can redistribute this application and/or modify it since you respect the terms of the GNU Affero General Public License.
 * If you modify the source code and then use this modified source code in your creation, you must make available the source code of your modifications.
 *
 * You should have received a copy of the GNU Affero General Public License along with the software.
 * If not, please see : <http://www.gnu.org/licenses/>. Full compliance requires reading the terms of this license and following its directives.

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
		Boolean isTeacher = object.getBoolean("isTeacher");

		if (isTeacher != null) {
			return isTeacher ? TEACHER_PROFILE_EXTERNAL_ID : PERSONNEL_PROFILE_EXTERNAL_ID;
		} else {
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
	}

	@Override
	public void process(JsonObject object) {
		String profile = detectProfile(object);
		object.put("profiles", new fr.wseduc.webutils.collections.JsonArray()
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
			structuresByFunctions = new fr.wseduc.webutils.collections.JsonArray(new ArrayList<>(s));
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
