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
		importer.addStructureNameInGroups(getAcademyPrefix());
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

	protected String[][] createGroups(JsonArray groups) {
		return StudentImportProcessing1d.createGroups(groups, importer, getAcademyPrefix());
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
