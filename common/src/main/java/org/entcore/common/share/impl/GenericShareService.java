/*
 * Copyright. Tous droits réservés. WebServices pour l’Education.
 */

package org.entcore.common.share.impl;

import org.entcore.common.share.ShareService;
import org.entcore.common.user.UserUtils;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.security.ActionType;
import fr.wseduc.webutils.security.SecuredAction;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.*;

import static org.entcore.common.user.UserUtils.findVisibleProfilsGroups;
import static org.entcore.common.user.UserUtils.findVisibleUsers;

public abstract class GenericShareService implements ShareService {

	protected final EventBus eb;
	protected final Map<String, SecuredAction> securedActions;
	protected final Map<String, List<String>> groupedActions;

	public GenericShareService(EventBus eb, Map<String, SecuredAction> securedActions,
			Map<String, List<String>> groupedActions) {
		this.eb = eb;
		this.securedActions = securedActions;
		this.groupedActions = groupedActions;
	}

	protected JsonArray getResoureActions(Map<String, SecuredAction> securedActions) {
		JsonObject resourceActions = new JsonObject();
		for (SecuredAction action: securedActions.values()) {
			if (ActionType.RESOURCE.name().equals(action.getType())) {
				JsonObject a = resourceActions.getObject(action.getDisplayName());
				if (a == null) {
					a = new JsonObject()
							.putArray("name", new JsonArray().add(action.getName().replaceAll("\\.", "-")))
							.putString("displayName", action.getDisplayName())
							.putString("type", action.getType());
					resourceActions.putObject(action.getDisplayName(), a);
				} else {
					a.getArray("name").add(action.getName().replaceAll("\\.", "-"));
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
				findVisibleUsers(eb, userId, false, new Handler<JsonArray>() {
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

	// TODO improve query
	protected void profilGroupIsVisible(String userId, final String groupId, final Handler<Boolean> handler) {
		if (userId == null || groupId == null) {
			handler.handle(false);
			return;
		}
		findVisibleProfilsGroups(eb, userId, new Handler<JsonArray>() {
			@Override
			public void handle(JsonArray visibleGroups) {
				final List<String> visibleGroupsIds = new ArrayList<>();
				for (int i = 0; i < visibleGroups.size(); i++) {
					JsonObject j = visibleGroups.get(i);
					if (j != null && j.getString("id") != null) {
						visibleGroupsIds.add(j.getString("id"));
					}
				}
				handler.handle(visibleGroupsIds.contains(groupId));
			}
		});
	}

	protected void userIsVisible(String userId, final String userShareId, final Handler<Boolean> handler) {
		if (userId == null || userShareId == null) {
			handler.handle(false);
			return;
		}
		findVisibleUsers(eb, userId, false, new Handler<JsonArray>() {
			@Override
			public void handle(JsonArray visibleUsers) {
				final List<String> visibleUsersIds = new ArrayList<>();
				for (int i = 0; i < visibleUsers.size(); i++) {
					JsonObject j = visibleUsers.get(i);
					if (j != null && j.getString("id") != null) {
						visibleUsersIds.add(j.getString("id"));
					}
				}
				handler.handle(visibleUsersIds.contains(userShareId));
			}
		});
	}

	protected boolean actionsExists(List<String> actions) {
		if (securedActions != null) {
			List<String> a = new ArrayList<>();
			for (String action: securedActions.keySet()) {
				a.add(action.replaceAll("\\.", "-"));
			}
			return a.containsAll(actions);
		}
		return false;
	}

	protected void groupShareValidation(String userId, String groupShareId, final List<String> actions,
			final Handler<Either<String, JsonObject>> handler) {
		profilGroupIsVisible(userId, groupShareId, new Handler<Boolean>() {
			@Override
			public void handle(Boolean visible) {
				if (Boolean.TRUE.equals(visible)) {
					if (actionsExists(actions)) {
						handler.handle(new Either.Right<String, JsonObject>(new JsonObject()));
					} else {
						handler.handle(new Either.Left<String, JsonObject>("Invalid actions."));
					}
				} else {
					handler.handle(new Either.Left<String, JsonObject>("Profil group not found."));
				}
			}
		});
	}

	protected void userShareValidation(String userId, String userShareId, final List<String> actions,
										final Handler<Either<String, JsonObject>> handler) {
		userIsVisible(userId, userShareId, new Handler<Boolean>() {
			@Override
			public void handle(Boolean visible) {
				if (Boolean.TRUE.equals(visible)) {
					if (actionsExists(actions)) {
						handler.handle(new Either.Right<String, JsonObject>(new JsonObject()));
					} else {
						handler.handle(new Either.Left<String, JsonObject>("Invalid actions."));
					}
				} else {
					handler.handle(new Either.Left<String, JsonObject>("User not found."));
				}
			}
		});
	}


	protected List<String> findRemoveActions(List<String> removeActions) {
		if (removeActions == null || removeActions.isEmpty()) {
			return null;
		}
		List<String> rmActions = new ArrayList<>(removeActions);
		if (groupedActions != null) {
			for (Map.Entry<String, List<String>> ga: groupedActions.entrySet()) {
				for (String a: removeActions) {
					if (ga.getValue().contains(a)) {
						rmActions.add(ga.getKey());
						break;
					}
				}
			}
		}
		return rmActions;
	}

}
