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
