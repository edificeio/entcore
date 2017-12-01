/*
 * Copyright © WebServices pour l'Éducation, 2014
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
