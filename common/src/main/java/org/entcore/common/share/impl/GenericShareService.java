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
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;
import org.entcore.common.neo4j.StatementsBuilder;
import org.entcore.common.share.ShareService;
import org.entcore.common.user.UserUtils;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.security.ActionType;
import fr.wseduc.webutils.security.SecuredAction;
import org.entcore.common.validation.StringValidation;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static fr.wseduc.webutils.Utils.getOrElse;
import static org.entcore.common.neo4j.Neo4jResult.validResultHandler;
import static org.entcore.common.user.UserUtils.findVisibleProfilsGroups;
import static org.entcore.common.user.UserUtils.findVisibleUsers;
import static org.entcore.common.validation.StringValidation.cleanId;

public abstract class GenericShareService implements ShareService {

	protected static final Logger log = LoggerFactory.getLogger(GenericShareService.class);
	private static final String GROUP_SHARED =
			"MATCH (g:Group) WHERE g.id in {groupIds} " +
			"RETURN distinct g.id as id, g.name as name, g.groupDisplayName as groupDisplayName, g.structureName as structureName " +
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
				JsonObject a = resourceActions.getJsonObject(action.getDisplayName());
				if (a == null) {
					a = new JsonObject()
							.put("name", new fr.wseduc.webutils.collections.JsonArray().add(action.getName().replaceAll("\\.", "-")))
							.put("displayName", action.getDisplayName())
							.put("type", action.getType());
					resourceActions.put(action.getDisplayName(), a);
				} else {
					a.getJsonArray("name").add(action.getName().replaceAll("\\.", "-"));
				}
			}
		}
		this.resourceActions = new fr.wseduc.webutils.collections.JsonArray(new ArrayList<>(resourceActions.getMap().values()));
		return this.resourceActions;
	}

	protected void getShareInfos(final String userId, final JsonArray actions,
			final JsonObject groupCheckedActions, final JsonObject userCheckedActions,
			final String acceptLanguage, String search, final Handler<JsonObject> handler) {
		final JsonObject params = new JsonObject().put("groupIds",
				new fr.wseduc.webutils.collections.JsonArray(new ArrayList<>(groupCheckedActions.fieldNames())));
		final JsonObject params2 = new JsonObject().put("userIds",
				new fr.wseduc.webutils.collections.JsonArray(new ArrayList<>(userCheckedActions.fieldNames())));
		if (search != null && search.trim().isEmpty()) {
			final Neo4j neo4j = Neo4j.getInstance();
			neo4j.execute(GROUP_SHARED, params, validResultHandler(new Handler<Either<String, JsonArray>>() {
				@Override
				public void handle(Either<String, JsonArray> sg) {
					JsonArray visibleGroups;
					if (sg.isRight()) {
						visibleGroups = sg.right().getValue();
					} else {
						visibleGroups = new fr.wseduc.webutils.collections.JsonArray();
					}
					final JsonObject groups = new JsonObject();
					groups.put("visibles", visibleGroups);
					groups.put("checked", groupCheckedActions);
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
								visibleUsers = new fr.wseduc.webutils.collections.JsonArray();
							}
							JsonObject users = new JsonObject();
							users.put("visibles", visibleUsers);
							users.put("checked", userCheckedActions);
							JsonObject share = new JsonObject()
									.put("actions", actions)
									.put("groups", groups)
									.put("users", users);
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
				params.put("search", sanitizedSearch);
				params2.put("search", sanitizedSearch);
			} else {
				preFilter = null;
			}

			final String q =
					"RETURN distinct profileGroup.id as id, profileGroup.name as name, " +
					"profileGroup.groupDisplayName as groupDisplayName, profileGroup.structureName as structureName " +
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
					groups.put("visibles", visibleGroups);
					groups.put("checked", groupCheckedActions);
					for (Object u : visibleGroups) {
						if (!(u instanceof JsonObject)) continue;
						JsonObject group = (JsonObject) u;
						UserUtils.groupDisplayName(group, acceptLanguage);
					}
					findVisibleUsers(eb, userId, true, preFilter, q2, params2, new Handler<JsonArray>() {
						@Override
						public void handle(JsonArray visibleUsers) {
							JsonObject users = new JsonObject();
							users.put("visibles", visibleUsers);
							users.put("checked", userCheckedActions);
							JsonObject share = new JsonObject()
									.put("actions", actions)
									.put("groups", groups)
									.put("users", users);
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
					JsonObject j = visibleGroups.getJsonObject(i);
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
					JsonObject j = visibleUsers.getJsonObject(i);
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

	protected void shareValidation(String resourceId, String userId, JsonObject share, Handler<Either<String, JsonObject>> handler) {
		final JsonObject groups = share.getJsonObject("groups");
		final JsonObject users = share.getJsonObject("users");
		final JsonObject shareBookmark = share.getJsonObject("bookmarks");
		final HashMap<String, Set<String>> membersActions = new HashMap<>();

		if (groups != null && groups.size() > 0) {
			for (String attr : groups.fieldNames()) {
				JsonArray actions = groups.getJsonArray(attr);
				if (actionsExists(actions.getList())) {
					membersActions.put(attr, new HashSet<>(actions.getList()));
				}
			}
		}
		if (users != null && users.size() > 0) {
			for (String attr : users.fieldNames()) {
				JsonArray actions = users.getJsonArray(attr);
				if (actionsExists(actions.getList())) {
					membersActions.put(attr, new HashSet<>(actions.getList()));
				}
			}
		}
		if (shareBookmark != null && shareBookmark.size() > 0) {
			final JsonObject p = new JsonObject().put("userId", userId);
			StatementsBuilder statements = new StatementsBuilder();
			for (String sbId: shareBookmark.fieldNames()) {
				final String csbId = cleanId(sbId);
				final String query =
						"MATCH (:User {id:{userId}})-[:HAS_SB]->(sb:ShareBookmark) " +
						"RETURN DISTINCT '" + csbId + "' as id, TAIL(sb." + csbId + ") as members ";
				statements.add(query, p);
			}
			Neo4j.getInstance().executeTransaction(statements.build(), null, true, Neo4jResult.validResultsHandler(sbRes -> {
				if (sbRes.isRight()) {
					JsonArray a = sbRes.right().getValue();
					for (Object o: a) {
						JsonObject r = ((JsonArray) o).getJsonObject(0);
						JsonArray actions = shareBookmark.getJsonArray(r.getString("id"));
						JsonArray mIds = r.getJsonArray("members");
						if (actions != null && mIds != null && mIds.size() > 0 && actionsExists(actions.getList())) {
							for (Object mId: mIds) {
								Set<String> actionsShare = membersActions.get(mId.toString());
								if (actionsShare == null) {
									actionsShare = new HashSet<>(new HashSet<>(actions.getList()));
									membersActions.put(mId.toString(), actionsShare);
//								} else {
//									actionsShare.addAll(new HashSet<>(actions.getList()));
								}
							}
						}
					}
					shareValidationVisible(userId, resourceId, handler, membersActions, shareBookmark.fieldNames());
				} else {
					handler.handle(new Either.Left<>(sbRes.left().getValue()));
				}
			}));
		} else {
			shareValidationVisible(userId, resourceId, handler, membersActions, null);
		}
	}

	private void shareValidationVisible(String userId, String resourceId, Handler<Either<String, JsonObject>> handler, HashMap<String, Set<String>> membersActions, Set<String> shareBookmarkIds) {
		final String preFilter = "AND m.id IN {members} ";
		final Set<String> members = membersActions.keySet();
		final JsonObject params = new JsonObject().put("members", new JsonArray(new ArrayList<>(members)));
		final String customReturn = "RETURN DISTINCT visibles.id as id, has(visibles.login) as isUser";
		UserUtils.findVisibles(eb, userId, customReturn, params, true, true, false, null, preFilter, res -> {
			if (res != null) {
				final JsonArray users = new JsonArray();
				final JsonArray groups = new JsonArray();
				final JsonArray shared = new JsonArray();
				final JsonArray notifyMembers = new JsonArray();
				for (Object o: res) {
					JsonObject j = (JsonObject) o;
					final String attr = j.getString("id");
					if (Boolean.TRUE.equals(j.getBoolean("isUser"))) {
						users.add(attr);
						notifyMembers.add(new JsonObject().put("userId", attr));
						prepareSharedArray(resourceId, "userId", shared, attr, membersActions.get(attr));
					} else {
						groups.add(attr);
						notifyMembers.add(new JsonObject().put("groupId", attr));
						prepareSharedArray(resourceId, "groupId", shared, attr, membersActions.get(attr));
					}
				}
				handler.handle(new Either.Right<>(params.put("shared", shared)
						.put("groups", groups).put("users", users).put("notify-members", notifyMembers)));
				if (shareBookmarkIds != null && res.size() < members.size()) {
					members.removeAll(groups.getList());
					members.removeAll(users.getList());
					resyncShareBookmark(userId, members, shareBookmarkIds);
				}
			} else {
				handler.handle(new Either.Left<>("Invalid members count."));
			}
		});
	}

	private void resyncShareBookmark(String userId, Set<String> members, Set<String> shareBookmarkIds) {
		final StringBuilder query = new StringBuilder("MATCH (:User {id:{userId}})-[:HAS_SB]->(sb:ShareBookmark) SET ");
		for (String sb: shareBookmarkIds) {
			query.append("sb.").append(sb).append(" = FILTER(mId IN sb.").append(sb).append(" WHERE NOT(mId IN {members})), ");
		}
		JsonObject params = new JsonObject().put("userId", userId).put("members", new JsonArray(new ArrayList<>(members)));
		Neo4j.getInstance().execute(query.substring(0, query.length() - 2), params, res -> {
			if ("ok".equals(res.body().getString("status"))) {
				log.info("Resync share bookmark for user " + userId);
			} else {
				log.error("Error when resync share bookmark for user " + userId + " : " + res.body().getString("message"));
			}
		});
	}


	protected void getNotifyMembers(Handler<Either<String, JsonObject>> handler, JsonArray oldShared, JsonArray members, Function<Object, String> f) {
		JsonArray notifyMembers;
		if (oldShared != null &&  oldShared.size() > 0 && members != null && members.size() > 0) {
			final Set<String> oldMembersIds = oldShared.stream()
					.map(f)
					.collect(Collectors.toSet());
			notifyMembers = new JsonArray();
			for (Object o : members) {
				final JsonObject j = (JsonObject) o;
				final String memberId = getOrElse(j.getString("groupId"), j.getString("userId"));
				if (!oldMembersIds.contains(memberId)) {
					notifyMembers.add(j);
				}
			}
		} else {
			notifyMembers = members;
		}
		handler.handle(new Either.Right<>(new JsonObject().put("notify-timeline-array", notifyMembers)));
	}

	protected abstract void prepareSharedArray(String resourceId, String type, JsonArray shared, String attr, Set<String> actions);

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
