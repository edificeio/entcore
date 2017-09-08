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

package org.entcore.common.share.impl;

import fr.wseduc.webutils.I18n;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.share.ShareService;
import org.entcore.common.user.UserUtils;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.security.ActionType;
import fr.wseduc.webutils.security.SecuredAction;
import org.entcore.common.validation.StringValidation;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.*;

import static org.entcore.common.neo4j.Neo4jResult.validResultHandler;
import static org.entcore.common.user.UserUtils.findVisibleProfilsGroups;
import static org.entcore.common.user.UserUtils.findVisibleUsers;

public abstract class GenericShareService implements ShareService {

	private static final String GROUP_SHARED =
			"MATCH (g:Group) WHERE g.id in {groupIds} " +
			"RETURN distinct g.id as id, g.name as name, g.groupDisplayName as groupDisplayName " +
			"ORDER BY name ";
	private static final String USER_SHARED =
			"MATCH (u:User) WHERE u.id in {userIds} " +
			"RETURN distinct u.id as id, u.login as login, u.displayName as username, " +
			"u.lastName as lastName, u.firstName as firstName, u.profiles[0] as profile  " +
			"ORDER BY username ";
	protected final EventBus eb;
	protected final Map<String, SecuredAction> securedActions;
	protected final Map<String, List<String>> groupedActions;
	protected static final I18n i18n = I18n.getInstance();
	private JsonArray resourceActions;

	public GenericShareService(EventBus eb, Map<String, SecuredAction> securedActions,
			Map<String, List<String>> groupedActions) {
		this.eb = eb;
		this.securedActions = securedActions;
		this.groupedActions = groupedActions;
	}

	protected JsonArray getResoureActions(Map<String, SecuredAction> securedActions) {
		if (resourceActions != null) {
			return resourceActions;
		}
		JsonObject resourceActions = new JsonObject();
		for (SecuredAction action: securedActions.values()) {
			if (ActionType.RESOURCE.name().equals(action.getType()) && !action.getDisplayName().isEmpty()) {
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
		this.resourceActions = new JsonArray(resourceActions.toMap().values().toArray());
		return this.resourceActions;
	}

	protected void getShareInfos(final String userId, final JsonArray actions,
			final JsonObject groupCheckedActions, final JsonObject userCheckedActions,
			final String acceptLanguage, String search, final Handler<JsonObject> handler) {
		final JsonObject params = new JsonObject().putArray("groupIds",
				new JsonArray(groupCheckedActions.getFieldNames().toArray()));
		final JsonObject params2 = new JsonObject().putArray("userIds",
				new JsonArray(userCheckedActions.getFieldNames().toArray()));
		if (search != null && search.trim().isEmpty()) {
			final Neo4j neo4j = Neo4j.getInstance();
			neo4j.execute(GROUP_SHARED, params, validResultHandler(new Handler<Either<String, JsonArray>>() {
				@Override
				public void handle(Either<String, JsonArray> sg) {
					JsonArray visibleGroups;
					if (sg.isRight()) {
						visibleGroups = sg.right().getValue();
					} else {
						visibleGroups = new JsonArray();
					}
					final JsonObject groups = new JsonObject();
					groups.putArray("visibles", visibleGroups);
					groups.putObject("checked", groupCheckedActions);
					for (Object u : visibleGroups) {
						if (!(u instanceof JsonObject)) continue;
						JsonObject group = (JsonObject) u;
						UserUtils.groupDisplayName(group, acceptLanguage);
					}
					neo4j.execute(USER_SHARED, params2, validResultHandler(new Handler<Either<String, JsonArray>>() {
						@Override
						public void handle(Either<String, JsonArray> event) {
							JsonArray visibleUsers;
							if (event.isRight()) {
								visibleUsers = event.right().getValue();
							} else {
								visibleUsers = new JsonArray();
							}
							JsonObject users = new JsonObject();
							users.putArray("visibles", visibleUsers);
							users.putObject("checked", userCheckedActions);
							JsonObject share = new JsonObject()
									.putArray("actions", actions)
									.putObject("groups", groups)
									.putObject("users", users);
							handler.handle(share);
						}
					}));
				}
			}));
		} else {
			final String preFilter;
			if (search != null) {
				preFilter = "AND m.displayNameSearchField CONTAINS {search} ";
				String sanitizedSearch = StringValidation.removeAccents(search.trim()).toLowerCase();
				params.putString("search", sanitizedSearch);
				params2.putString("search", sanitizedSearch);
			} else {
				preFilter = null;
			}

			final String q =
					"RETURN distinct profileGroup.id as id, profileGroup.name as name, " +
					"profileGroup.groupDisplayName as groupDisplayName " +
					"ORDER BY name " +
					"UNION " +
					GROUP_SHARED;

			final String q2 =
					"RETURN distinct visibles.id as id, visibles.login as login, visibles.displayName as username, " +
					"visibles.lastName as lastName, visibles.firstName as firstName, visibles.profiles[0] as profile " +
					"ORDER BY username " +
					"UNION " +
					USER_SHARED;

			UserUtils.findVisibleProfilsGroups(eb, userId, q, params, new Handler<JsonArray>() {
				@Override
				public void handle(JsonArray visibleGroups) {
					final JsonObject groups = new JsonObject();
					groups.putArray("visibles", visibleGroups);
					groups.putObject("checked", groupCheckedActions);
					for (Object u : visibleGroups) {
						if (!(u instanceof JsonObject)) continue;
						JsonObject group = (JsonObject) u;
						UserUtils.groupDisplayName(group, acceptLanguage);
					}
					findVisibleUsers(eb, userId, true, preFilter, q2, params2, new Handler<JsonArray>() {
						@Override
						public void handle(JsonArray visibleUsers) {
							JsonObject users = new JsonObject();
							users.putArray("visibles", visibleUsers);
							users.putObject("checked", userCheckedActions);
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
