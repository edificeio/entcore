/* Copyright © "Open Digital Education", 2014
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

 *
 */

package org.entcore.feeder.aaf;

import org.entcore.feeder.dictionary.structures.DefaultProfiles;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class StudentImportProcessing2 extends StudentImportProcessing {

	protected StudentImportProcessing2(String path, Vertx vertx) {
		super(path, vertx);
	}

	@Override
	public void start(final Handler<Message<JsonObject>> handler) {
		parse(handler, new CleanImportProcessing(path, vertx));
	}

	@Override
	protected void preCommit() {
		importer.linkRelativeToStructure(DefaultProfiles.RELATIVE_PROFILE_EXTERNAL_ID, getAcademyPrefix());
		importer.linkRelativeToClass(DefaultProfiles.RELATIVE_PROFILE_EXTERNAL_ID, getAcademyPrefix());
	}

	@Override
	public void process(JsonObject object) {
		String[][] classes = createClasses(object.getJsonArray("classes"));
		String[][] groups = createGroups(object.getJsonArray("groups"));
		JsonArray relative = parseRelativeField(object.getJsonArray("relative"));
		importer.createOrUpdateStudent(object, DefaultProfiles.STUDENT_PROFILE_EXTERNAL_ID,
				null, null, classes, groups, relative, false, true);
	}

}
