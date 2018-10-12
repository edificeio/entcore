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

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.*;

public class PersonnelImportProcessing2 extends PersonnelImportProcessing {

	protected PersonnelImportProcessing2(String path, Vertx vertx) {
		super(path, vertx);
	}

	@Override
	protected void preCommit() {
		importer.getPersEducNat().createAndLinkSubjects();
	}

	@Override
	public void start(final Handler<Message<JsonObject>> handler) {
		if (importer.isFirstImport()) {
			parse(handler, new StudentImportProcessing2(path, vertx));
		} else {
			initAcademyPrefix(path);
			importer.markMissingUsers(null, getAcademyPrefix(), new Handler<Void>() {
				@Override
				public void handle(Void event) {
					parse(handler, new StudentImportProcessing2(path, vertx));
				}
			});
		}
	}

	@Override
	public void process(JsonObject object) {
		List<String> c = object.getJsonArray("classes") != null ? object.getJsonArray("classes").getList() : new LinkedList<String>();
		final List<String[]> groups = new ArrayList<>();
		createGroups(object.getJsonArray("groups"), c, groups);
		String[][] classes = createClasses(new fr.wseduc.webutils.collections.JsonArray(c));
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
		createFunctionGroups(object.getJsonArray("functions"), groups);
		createHeadTeacherGroups(object.getJsonArray("headTeacher"), groups);
		importer.createOrUpdatePersonnel(object, detectProfile(object), structuresByFunctions,
				classes, groups.toArray(new String[][]{}), false, true);
	}

}
