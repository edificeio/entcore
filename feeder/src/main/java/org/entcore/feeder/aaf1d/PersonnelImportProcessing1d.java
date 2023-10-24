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
import org.entcore.feeder.dictionary.structures.ImporterStructure;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static org.entcore.feeder.dictionary.structures.DefaultProfiles.PERSONNEL_PROFILE_EXTERNAL_ID;
import static org.entcore.feeder.dictionary.structures.DefaultProfiles.TEACHER_PROFILE_EXTERNAL_ID;
import static fr.wseduc.webutils.Utils.isNotEmpty;

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
		final String externalId = object.getString("externalId");
		final JsonObject supportPers1D2D;
		if (isNotEmpty(externalId) && importer.getToSupportPerseducnat1D2D().containsKey(externalId)) {
			supportPers1D2D = importer.getToSupportPerseducnat1D2D().get(externalId);
			final JsonArray functions2d = supportPers1D2D.getJsonArray("functions");
			if (functions2d != null && !functions2d.isEmpty()) {
				final JsonArray function1d2d = object.getJsonArray("functions", new JsonArray());
				for (Object o: functions2d) {
					if (!(o instanceof String) || !o.toString().contains("$")) continue;
					function1d2d.add(o);
				}
				if (!function1d2d.isEmpty()) object.put("functions", function1d2d);
			}
			log.info(x -> "Use 1D2D support for user : " + externalId);
		} else {
			supportPers1D2D = null;
		}

		final String profile;
		if (supportPers1D2D != null) {
			profile = "Teacher".equals(supportPers1D2D.getString("profile")) ? TEACHER_PROFILE_EXTERNAL_ID : PERSONNEL_PROFILE_EXTERNAL_ID;
		} else {
			profile = detectProfile(object);
		}
		object.put("profiles", new JsonArray()
				.add((TEACHER_PROFILE_EXTERNAL_ID.equals(profile) ? "Teacher" : "Personnel")));
		String email = object.getString("email");
		if (email != null && !email.trim().isEmpty()) {
			object.put("emailAcademy", email);
		}
		final List<String[]> groups = new ArrayList<>();
		JsonArray functions = object.getJsonArray("functions");
		createDirectionGroups(object.getJsonArray("direction"), groups);
		getDirectionOrFunctionFromFunctions(functions, groups);
		cleanAndCreatePositions(object.getJsonArray("structures"), functions, externalId);
		JsonArray structuresByFunctions = null;
		if (functions != null) {
			Set<String> s = new HashSet<>();
			for (Object o: functions) {
				if (!(o instanceof String) || !o.toString().contains("$")) continue;
				s.add(o.toString().substring(0, o.toString().indexOf('$')));
			}
			if (supportPers1D2D != null) {
				final JsonArray structures2d = supportPers1D2D.getJsonArray("structuresExternalIds");
				if (structures2d != null) {
					structures2d.forEach(x -> s.add(x.toString()));
				}
			}
			structuresByFunctions = new JsonArray(new ArrayList<>(s));
		}
		importer.createOrUpdatePersonnel(object, profile, structuresByFunctions, null, groups.toArray(new String[][]{}), true, true);
	}

	protected void getDirectionOrFunctionFromFunctions(JsonArray functions, List<String[]> linkStructureGroups) {
		if (functions != null && functions.size() > 0) {
			for (Object o : functions) {
				if (!(o instanceof String)) continue;
				String [] g = ((String) o).split("\\$");
				if (g.length == 5) {
					ImporterStructure s = importer.getStructure(g[0]);

					if(s != null && DIRECTION_FONCTIONS.contains(g[4]) == true)
						createDirectionGroups(new JsonArray().add(g[0]), linkStructureGroups);

					//supportPers1D2D
					if (s != null && "AAF".equals(s.getStruct().getString("source"))) {
						String groupExternalId;
						if ("ENS".equals(g[1])) {
							groupExternalId = s.getExternalId() + "$" + g[3];
						} else if (!"-".equals(g[1])) {
							groupExternalId = s.getExternalId() + "$" + g[1];
						} else {
							continue;
						}
						if (linkStructureGroups != null) {
							final String[] group = new String[2];
							group[0] = s.getExternalId();
							group[1] = groupExternalId;
							linkStructureGroups.add(group);
						}
					}
				}
			}
		}
	}

	@Override
	public String getMappingResource() {
		return "dictionary/mapping/aaf1d/PersEducNat.json";
	}

	@Override
	protected ImportProcessing getNextImportProcessing() {
		return new MarkMissingUsers1d(path, vertx);
	}

}
