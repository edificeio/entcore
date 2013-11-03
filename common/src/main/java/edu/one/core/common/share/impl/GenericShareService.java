package edu.one.core.common.share.impl;

import edu.one.core.common.share.ShareService;
import edu.one.core.common.user.UserUtils;
import edu.one.core.infra.security.ActionType;
import edu.one.core.infra.security.SecuredAction;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.*;

public abstract class GenericShareService implements ShareService {

	protected final EventBus eb;

	public GenericShareService(EventBus eb) {
		this.eb = eb;
	}

	protected JsonArray getResoureActions(Map<String, SecuredAction> securedActions) {
		JsonObject resourceActions = new JsonObject();
		for (SecuredAction action: securedActions.values()) {
			if (ActionType.RESOURCE.name().equals(action.getType())) {
				JsonObject a = resourceActions.getObject(action.getDisplayName());
				if (a == null) {
					a = new JsonObject()
							.putArray("name", new JsonArray().add(action.getName()))
							.putString("displayName", action.getDisplayName())
							.putString("type", action.getType());
					resourceActions.putObject(action.getDisplayName(), a);
				} else {
					a.getArray("name").add(action.getName());
				}
			}
		}
		return new JsonArray(resourceActions.toMap().values().toArray());
	}

	private void groupDisplayName(JsonObject group) {
		String name = group.getString("name");
		if (name != null && name.contains("_")) {
			int idx = name.lastIndexOf('_');
			if (name.length() > idx && idx > 0) {
				String type = name.substring(idx + 1).toLowerCase();
				String value = name.substring(0, idx);
				String schoolOrClass = (group.getString("type", "")
						.startsWith("GROUP_CLASSE")) ? "class" : "school";
				group.putString("displayName", value);
			}
		}
	}

	protected void getShareInfos(final String userId, final JsonArray actions,
				final JsonObject checkedActions, final Handler<JsonObject> handler) {
		UserUtils.findVisibleProfilsGroups(eb, userId, new Handler<JsonArray>() {
			@Override
			public void handle(JsonArray visibleGroups) {
				final JsonObject groups = new JsonObject();
				JsonObject checked = new JsonObject();
				groups.putArray("visibles", visibleGroups);
				groups.putObject("checked", checked);
				for (Object u : visibleGroups) {
					if (!(u instanceof JsonObject)) continue;
					JsonObject group = (JsonObject) u;
					String groupId = group.getString("id");
					if (groupId != null) {
						JsonArray a = checkedActions.getArray(groupId);
						if (a != null) {
							checked.putArray(groupId, a);
						}
					}
				}
				UserUtils.findVisibleUsers(eb, userId, new Handler<JsonArray>() {
					@Override
					public void handle(JsonArray visibleUsers) {
						JsonObject users = new JsonObject();
						JsonObject userChecked = new JsonObject();
						users.putArray("visibles", visibleUsers);
						users.putObject("checked", userChecked);
						for (Object u : visibleUsers) {
							if (!(u instanceof JsonObject)) continue;
							JsonObject user = (JsonObject) u;
							String userId = user.getString("id");
							if (userId != null) {
								JsonArray a = checkedActions.getArray(userId);
								if (a != null) {
									userChecked.putArray(userId, a);
								}
							}
						}
						JsonObject share = new JsonObject()
								.putArray("actions", actions)
								.putObject("groups", groups)
								.putObject("users", users);
						handler.handle(share);
					}
				});
			}
		});
	}
}
