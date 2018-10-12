/*
 * Copyright © "Open Digital Education", 2014
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

package org.entcore.feeder.dictionary.structures;

import io.vertx.core.json.JsonObject;

import static org.entcore.feeder.dictionary.structures.DefaultProfiles.PERSONNEL_PROFILE_EXTERNAL_ID;
import static org.entcore.feeder.dictionary.structures.DefaultProfiles.TEACHER_PROFILE_EXTERNAL_ID;

public final class DefaultFunctions {

	private DefaultFunctions() {}

	public static final String ADMIN_LOCAL_EXTERNAL_ID = "ADMIN_LOCAL";
	public static final JsonObject ADMIN_LOCAL = new JsonObject()
			.put("externalId", ADMIN_LOCAL_EXTERNAL_ID)
			.put("name", "AdminLocal");

	public static final String CLASS_ADMIN_EXTERNAL_ID = "CLASS_ADMIN";
	public static final JsonObject CLASS_ADMIN = new JsonObject()
			.put("externalId", CLASS_ADMIN_EXTERNAL_ID)
			.put("name", "ClassAdmin");

	public static void createOrUpdateFunctions(Importer importer) {
		Profile p = importer.getProfile(PERSONNEL_PROFILE_EXTERNAL_ID);
		if (p != null) {
			JsonObject f = DefaultFunctions.ADMIN_LOCAL;
			p.createFunctionIfAbsent(f.getString("externalId"), f.getString("name"));
		}
		Profile t = importer.getProfile(TEACHER_PROFILE_EXTERNAL_ID);
		if (t != null) {
			JsonObject f = DefaultFunctions.CLASS_ADMIN;
			t.createFunctionIfAbsent(f.getString("externalId"), f.getString("name"));
		}
	}

}
