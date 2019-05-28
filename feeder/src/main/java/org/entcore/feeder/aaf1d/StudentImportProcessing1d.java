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

import io.vertx.core.json.JsonObject;
import org.entcore.feeder.aaf.ImportProcessing;
import org.entcore.feeder.aaf.StudentImportProcessing;
import org.entcore.feeder.dictionary.structures.Importer;
import org.entcore.feeder.dictionary.structures.Structure;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;

public class StudentImportProcessing1d extends StudentImportProcessing {

	protected StudentImportProcessing1d(String path, Vertx vertx) {
		super(path, vertx);
	}

	@Override
	public String getMappingResource() {
		return "dictionary/mapping/aaf1d/Eleve.json";
	}

	@Override
	protected ImportProcessing getNextImportProcessing() {
		return new UserImportProcessing1d(path, vertx, resp);
	}

	@Override
	public void process(JsonObject object) {
		if (!importer.blockedIne(object)) {
			super.process(object);
		}
	}

	@Override
	protected JsonArray parseRelativeField(JsonArray relative) {
		return parseRelativeField1d(relative);
	}

	static JsonArray parseRelativeField1d(JsonArray relative) {
		JsonArray res = null;
		if (relative != null && relative.size() > 0) {
			res = new fr.wseduc.webutils.collections.JsonArray();
			for (Object o : relative) {
				if (!(o instanceof String)) continue;
				String [] r = ((String) o).split("\\$");
				res.add(r[0]);
			}
		}
		return res;
	}

	protected String[][] createGroups(JsonArray groups) {
		return createGroups(groups, importer, getAcademyPrefix());
	}

	static String[][] createGroups(JsonArray groups, Importer importer, String academyPrefix) {
		String [][] linkStructureGroups = null;
		if (groups != null && groups.size() > 0) {
			linkStructureGroups = new String[groups.size()][2];
			int i = 0;
			for (Object o : groups) {
				if (!(o instanceof String)) continue;
				String [] g = ((String) o).split("\\$");
				if (g.length == 5) {
					Structure s = importer.getStructure(g[0]);
					if (s != null) {
						String groupExternalId = academyPrefix + g[3];
						s.createFunctionalGroupIfAbsent(groupExternalId, g[4]);
						linkStructureGroups[i][0] = s.getExternalId();
						linkStructureGroups[i++][1] = groupExternalId;
					}
				}
			}
		}
		return linkStructureGroups;
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
						String classExternalId = getAcademyPrefix() + c[3];
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
