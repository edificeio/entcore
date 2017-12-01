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

import org.entcore.feeder.dictionary.structures.DefaultProfiles;
import org.entcore.feeder.dictionary.structures.Structure;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.HashSet;
import java.util.Set;

public class StudentImportProcessing extends BaseImportProcessing {

	protected Set<String> resp = new HashSet<>();
	protected StudentImportProcessing(String path, Vertx vertx) {
		super(path, vertx);
	}

	@Override
	public void start(final Handler<Message<JsonObject>> handler) {
		parse(handler, getNextImportProcessing());
	}

	@Override
	public String getMappingResource() {
		return "dictionary/mapping/aaf/Eleve.json";
	}

	protected ImportProcessing getNextImportProcessing() {
		return new UserImportProcessing(path, vertx, resp);
	}

	@Override
	public void process(JsonObject object) {
		createClasses(object.getJsonArray("classes"));
		createGroups(object.getJsonArray("groups"));
		JsonArray r = parseRelativeField(object.getJsonArray("relative"));
		if (r != null) {
			resp.addAll(r.getList());
		}
		object.put("profiles", new JsonArray().add("Student"));
		importer.createOrUpdateStudent(object, DefaultProfiles.STUDENT_PROFILE_EXTERNAL_ID,
				null, null, null, null, null, true, false);
	}

	protected JsonArray parseRelativeField(JsonArray relative) {
		JsonArray res = null;
		if (relative != null && relative.size() > 0) {
			res = new JsonArray();
			for (Object o : relative) {
				if (!(o instanceof String)) continue;
				String [] r = ((String) o).split("\\$");
				if (r.length == 6 && !"0".equals(r[3])) {
					res.add(r[0]);
				}
			}
		}
		return res;
	}

	protected String[][] createGroups(JsonArray groups) {
		String [][] linkStructureGroups = null;
		if (groups != null && groups.size() > 0) {
			linkStructureGroups = new String[groups.size()][2];
			int i = 0;
			for (Object o : groups) {
				if (!(o instanceof String)) continue;
				String [] g = ((String) o).split("\\$");
				if (g.length == 2) {
					Structure s = importer.getStructure(g[0]);
					if (s != null) {
						String groupExternalId = s.getExternalId() + "$" + g[1];
						s.createFunctionalGroupIfAbsent(groupExternalId, g[1]);
						linkStructureGroups[i][0] = s.getExternalId();
						linkStructureGroups[i++][1] = groupExternalId;
					}
				}
			}
		}
		return linkStructureGroups;
	}

	protected String[][] createClasses(JsonArray classes) {
		String [][] linkStructureClasses = null;
		if (classes != null && classes.size() > 0) {
			linkStructureClasses = new String[classes.size()][2];
			int i = 0;
			for (Object o : classes) {
				if (!(o instanceof String)) continue;
				String [] c = ((String) o).split("\\$");
				if (c.length == 2) {
					Structure s = importer.getStructure(c[0]);
					if (s != null) {
						String classExternalId = s.getExternalId() + "$" + c[1];
						s.createClassIfAbsent(classExternalId, c[1]);
						linkStructureClasses[i][0] = s.getExternalId();
						linkStructureClasses[i++][1] = classExternalId;
					}
				}
			}
		}
		return linkStructureClasses;
	}

	@Override
	protected String getFileRegex() {
		return ".*?_Eleve_[0-9]{4}\\.xml";
	}

}
