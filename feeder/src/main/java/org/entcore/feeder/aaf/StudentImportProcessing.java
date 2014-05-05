/*
 * Copyright. Tous droits réservés. WebServices pour l’Education.
 */

package org.entcore.feeder.aaf;

import org.entcore.feeder.dictionary.structures.DefaultProfiles;
import org.entcore.feeder.dictionary.structures.Structure;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

public class StudentImportProcessing extends BaseImportProcessing {

	protected StudentImportProcessing(String path, Vertx vertx) {
		super(path, vertx);
	}

	@Override
	public void start(final Handler<Message<JsonObject>> handler) {
		parse(handler, new PersonnelImportProcessing2(path, vertx));
	}

	@Override
	public String getMappingResource() {
		return "dictionary/mapping/aaf/Eleve.json";
	}

	@Override
	public void process(JsonObject object) {
		createClasses(object.getArray("classes"));
		createGroups(object.getArray("groups"));
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
				if (r.length > 2) {
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
						s.createFunctionalGroupIfAbsent((String) o, g[1]);
						linkStructureGroups[i][0] = g[0];
						linkStructureGroups[i++][1] = (String) o;
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
						s.createClassIfAbsent((String) o, c[1]);
						linkStructureClasses[i][0] = c[0];
						linkStructureClasses[i++][1] = (String) o;
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
