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

import static org.entcore.feeder.dictionary.structures.DefaultProfiles.*;
import org.entcore.feeder.dictionary.structures.Structure;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.LinkedList;
import java.util.List;

public class PersonnelImportProcessing extends BaseImportProcessing {

	protected PersonnelImportProcessing(String path, Vertx vertx) {
		super(path, vertx);
	}

	@Override
	public void start(final Handler<Message<JsonObject>> handler) {
		parse(handler, getNextImportProcessing());
	}

	@Override
	public String getMappingResource() {
		return "dictionary/mapping/aaf/PersEducNat.json";
	}

	@Override
	public void process(JsonObject object) {
		List<String> c = object.getJsonArray("classes") != null ? object.getJsonArray("classes").getList() : new LinkedList<String>();
		createGroups(object.getJsonArray("groups"), c);
		createClasses(new JsonArray(c));
		linkMef(object.getJsonArray("modules"));
		String profile = detectProfile(object);
		object.put("profiles", new JsonArray()
				.add((TEACHER_PROFILE_EXTERNAL_ID.equals(profile) ? "Teacher" : "Personnel")));
		String email = object.getString("email");
		if (email != null && !email.trim().isEmpty()) {
			object.put("emailAcademy", email);
		}
		importer.createOrUpdatePersonnel(object, profile, null, null, null, true, false);
	}

	protected String detectProfile(JsonObject object) {
		JsonArray functions = object.getJsonArray("functions");
		if (object.getBoolean("teaches", false)) {
			return TEACHER_PROFILE_EXTERNAL_ID;
		} else if (functions != null && functions.size() > 0) {
			for (Object function : functions.getList()) {
				if (function != null && (function.toString().contains("$DOC$") || function.toString().contains("$ENS$"))) {
					return TEACHER_PROFILE_EXTERNAL_ID;
				}
			}
		}
		return PERSONNEL_PROFILE_EXTERNAL_ID;
	}

	protected void linkMef(JsonArray modules) {
		if (modules != null) {
			for (Object o : modules) {
				if (!(o instanceof String)) continue;
				String [] m = ((String) o).split("\\$");
				if (m.length == 3) {
					Structure s = importer.getStructure(m[0]);
					if (s != null) {
						s.linkModules(m[1]);
					}
				}
			}
		}
	}

	protected String[][] createGroups(JsonArray groups) {
		return createGroups(groups, null);
	}

	protected String[][] createGroups(JsonArray groups, List<String> classes) {
		String [][] linkStructureGroups = null;
		if (groups != null && groups.size() > 0) {
			linkStructureGroups = new String[groups.size()][3];
			int i = 0;
			for (Object o : groups) {
				if (!(o instanceof String)) continue;
				String [] g = ((String) o).split("\\$");
				if (g.length == 2 || g.length == 3) {
					Structure s = importer.getStructure(g[0]);
					if (s != null) {
						String groupExternalId = s.getExternalId() + "$" + g[1];
						s.createFunctionalGroupIfAbsent(groupExternalId, g[1]);
						linkStructureGroups[i][0] = s.getExternalId();
						linkStructureGroups[i][1] = groupExternalId;
						linkStructureGroups[i++][2] = (g.length == 3) ? g[2] : "";
						if (classes != null) {
							final List<String> lc = importer.getGroupClasses().get(groupExternalId);
							if (lc != null) {
								classes.addAll(lc);
							}
						}
					}
				}
			}
		}
		return linkStructureGroups;
	}

	protected String[][] createClasses(JsonArray classes) {
		String [][] linkStructureClasses = null;
		if (classes != null && classes.size() > 0) {
			linkStructureClasses = new String[classes.size()][3];
			int i = 0;
			for (Object o : classes) {
				if (!(o instanceof String)) continue;
				String [] c = ((String) o).split("\\$");
				if (c.length == 2 || c.length == 3) {
					Structure s = importer.getStructure(c[0]);
					if (s != null) {
						String classExternalId = s.getExternalId() + "$" + c[1];
						s.createClassIfAbsent(classExternalId, c[1]);
						linkStructureClasses[i][0] = s.getExternalId();
						linkStructureClasses[i][1] = classExternalId;
						linkStructureClasses[i++][2] = (c.length == 3) ? c[2] : "";
					}
				}
			}
		}
		return linkStructureClasses;
	}

	@Override
	protected String getFileRegex() {
		return ".*?PersEducNat_[0-9]{4}\\.xml";
	}

	protected ImportProcessing getNextImportProcessing() {
		return new PersonnelImportProcessing2(path, vertx);
	}

}
