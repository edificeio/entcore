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
		types = Collections.unmodifiableMap(t);
	}

	@Override
	public JsonObject getInfo(JsonObject info, String clientId) {
		JsonObject filteredInfos = info.copy();
		String type = Utils.getOrElse(types.get(info.getString("type", "")), "");
		filteredInfos.putString("type", type);
		if (clientId != null && !clientId.trim().isEmpty()) {
			filteredInfos.removeField("classes");
			filteredInfos.removeField("apps");
			filteredInfos.removeField("authorizedActions");
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
