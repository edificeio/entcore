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
		filteredInfos.remove("realClassesNames");
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
