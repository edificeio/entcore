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
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

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
		filteredInfos.put("type", type);
		filteredInfos.remove("cache");
		if (filteredInfos.getString("level") == null) {
			filteredInfos.put("level", "");
		} else if (filteredInfos.getString("level").contains("$")) {
			String[] level = filteredInfos.getString("level").split("\\$");
			filteredInfos.put("level", level[level.length -1]);
		}
		if (clientId != null && !clientId.trim().isEmpty()) {
			JsonArray classNames = filteredInfos.getJsonArray("classNames");
			filteredInfos.remove("classNames");
			JsonArray structureNames = filteredInfos.getJsonArray("structureNames");
			filteredInfos.remove("structureNames");
			filteredInfos.remove("federated");
			if (classNames != null && classNames.size() > 0) {
				filteredInfos.put("classId", classNames.getString(0));
			}
			if (structureNames != null && structureNames.size() > 0) {
				filteredInfos.put("schoolName", structureNames.getString(0));
			}
			filteredInfos.remove("functions");
			filteredInfos.remove("groupsIds");
			filteredInfos.remove("structures");
			filteredInfos.remove("classes");
			filteredInfos.remove("apps");
			filteredInfos.remove("authorizedActions");
			filteredInfos.remove("children");
			JsonArray authorizedActions = new fr.wseduc.webutils.collections.JsonArray();
			for (Object o: info.getJsonArray("authorizedActions")) {
				JsonObject j = (JsonObject) o;
				String name = j.getString("name");
				if (name != null && name.startsWith(clientId + "|")) {
					authorizedActions.add(j);
				}
			}
			if (authorizedActions.size() > 0) {
				filteredInfos.put("authorizedActions", authorizedActions);
			}
		}
		return filteredInfos;
	}

}
