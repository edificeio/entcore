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

package org.entcore.feeder.dictionary.structures;

import io.vertx.core.json.JsonObject;

public final class DefaultProfiles {

	private DefaultProfiles() {}

	public static final String TEACHER_PROFILE_EXTERNAL_ID = "PROFILE_TEACHER";
	public static final JsonObject TEACHER_PROFILE = new JsonObject()
			.put("externalId", TEACHER_PROFILE_EXTERNAL_ID)
			.put("name", "Teacher");

	public static final String RELATIVE_PROFILE_EXTERNAL_ID = "PROFILE_RELATIVE";
	public static final JsonObject RELATIVE_PROFILE = new JsonObject()
			.put("externalId", RELATIVE_PROFILE_EXTERNAL_ID)
			.put("name", "Relative");

	public static final String STUDENT_PROFILE_EXTERNAL_ID = "PROFILE_STUDENT";
	public static final JsonObject STUDENT_PROFILE = new JsonObject()
			.put("externalId", STUDENT_PROFILE_EXTERNAL_ID)
			.put("name", "Student");

	public static final String PERSONNEL_PROFILE_EXTERNAL_ID = "PROFILE_PERSONNEL";
	public static final JsonObject PERSONNEL_PROFILE = new JsonObject()
			.put("externalId", PERSONNEL_PROFILE_EXTERNAL_ID)
			.put("name", "Personnel");

	public static final String GUEST_PROFILE_EXTERNAL_ID = "PROFILE_GUEST";
	public static final JsonObject GUEST_PROFILE = new JsonObject()
			.put("externalId", GUEST_PROFILE_EXTERNAL_ID)
			.put("name", "Guest");

}
