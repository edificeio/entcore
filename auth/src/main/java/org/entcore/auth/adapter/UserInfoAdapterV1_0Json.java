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

package org.entcore.auth.adapter;


import fr.wseduc.webutils.Utils;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class UserInfoAdapterV1_0Json implements UserInfoAdapter {

	private static final Map<String, String> types;

	static {
		Map<String, String> t = new HashMap<>();
		t.put("Teacher", "ENSEIGNANT");
		t.put("Student", "ELEVE");
		t.put("Relative", "PERSRELELEVE");
		t.put("SuperAdmin", "SUPERADMIN");
		t.put("Personnel", "PERSEDUCNAT");
		types = Collections.unmodifiableMap(t);
	}

	@Override
	public JsonObject getInfo(JsonObject info, String clientId) {
		final JsonObject filteredInfos = getCommonFilteredInfos(info, clientId);
		filteredInfos.removeField("realClassesNames");
		return filteredInfos;
	}

	protected JsonObject getCommonFilteredInfos(JsonObject info, String clientId) {
		JsonObject filteredInfos = info.copy();
		String type = Utils.getOrElse(types.get(info.getString("type", "")), "");
		filteredInfos.putString("type", type);
		filteredInfos.removeField("cache");
		if (filteredInfos.getString("level") == null) {
			filteredInfos.putString("level", "");
		} else if (filteredInfos.getString("level").contains("$")) {
			String[] level = filteredInfos.getString("level").split("\\$");
			filteredInfos.putString("level", level[level.length -1]);
		}
		if (clientId != null && !clientId.trim().isEmpty()) {
			JsonArray classNames = filteredInfos.getArray("classNames");
			filteredInfos.removeField("classNames");
			JsonArray structureNames = filteredInfos.getArray("structureNames");
			filteredInfos.removeField("structureNames");
			filteredInfos.removeField("federated");
			if (classNames != null && classNames.size() > 0) {
				filteredInfos.putString("classId", classNames.<String>get(0));
			}
			if (structureNames != null && structureNames.size() > 0) {
				filteredInfos.putString("schoolName", structureNames.<String>get(0));
			}
			filteredInfos.removeField("functions");
			filteredInfos.removeField("groupsIds");
			filteredInfos.removeField("structures");
			filteredInfos.removeField("classes");
			filteredInfos.removeField("apps");
			filteredInfos.removeField("authorizedActions");
			filteredInfos.removeField("children");
			JsonArray authorizedActions = new JsonArray();
			for (Object o: info.getArray("authorizedActions")) {
				JsonObject j = (JsonObject) o;
				String name = j.getString("name");
				if (name != null && name.startsWith(clientId + "|")) {
					authorizedActions.addObject(j);
				}
			}
			if (authorizedActions.size() > 0) {
				filteredInfos.putArray("authorizedActions", authorizedActions);
			}
		}
		return filteredInfos;
	}

}
