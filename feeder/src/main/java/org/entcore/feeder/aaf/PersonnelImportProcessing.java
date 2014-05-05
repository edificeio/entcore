/*
 * Copyright. Tous droits réservés. WebServices pour l’Education.
 */

package org.entcore.feeder.aaf;

import static org.entcore.feeder.dictionary.structures.DefaultProfiles.*;
import org.entcore.feeder.dictionary.structures.Structure;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

public class PersonnelImportProcessing extends BaseImportProcessing {

	protected PersonnelImportProcessing(String path, Vertx vertx) {
		super(path, vertx);
	}

	@Override
	public void start(final Handler<Message<JsonObject>> handler) {
		parse(handler, new StudentImportProcessing(path, vertx));
	}

	@Override
	public String getMappingResource() {
		return "dictionary/mapping/aaf/PersEducNat.json";
	}

	@Override
	public void process(JsonObject object) {
		createClasses(object.getArray("classes"));
		createGroups(object.getArray("groups"));
		linkMef(object.getArray("modules"));
		linkClassesFieldOfStudy(object.getArray("classesFieldOfStudy"));
		linkGroupsFieldOfStudy(object.getArray("groupsFieldOfStudy"));
		String profile = detectProfile(object);
		importer.createOrUpdatePersonnel(object, profile, null, null, true, false);
	}

	protected String detectProfile(JsonObject object) {
		JsonArray functions = object.getArray("functions");
		return (object.getBoolean("teaches", false) || (functions != null &&
				functions.size() == 1 && ((String) functions.get(0))
				.contains("DOC"))) ? TEACHER_PROFILE_EXTERNAL_ID : PERSONNEL_PROFILE_EXTERNAL_ID;
	}

	protected void linkGroupsFieldOfStudy(JsonArray groupsFieldOfStudy) {
		if (groupsFieldOfStudy != null) {
			for (Object o : groupsFieldOfStudy) {
				if (!(o instanceof String)) continue;
				String [] c = ((String) o).split("\\$");
				if (c.length == 3) {
					Structure s = importer.getStructure(c[0]);
					if (s != null) {
						s.linkGroupFieldOfStudy(c[0] + "$" + c[1], c[2]);
					}
				}
			}
		}
	}

	protected void linkClassesFieldOfStudy(JsonArray classesFieldOfStudy) {
		if (classesFieldOfStudy != null) {
			for (Object o : classesFieldOfStudy) {
				if (!(o instanceof String)) continue;
				String [] c = ((String) o).split("\\$");
				if (c.length == 3) {
					Structure s = importer.getStructure(c[0]);
					if (s != null) {
						s.linkClassFieldOfStudy(c[0] + "$" + c[1], c[2]);
					}
				}
			}
		}
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
		return ".*?PersEducNat_[0-9]{4}\\.xml";
	}
}
