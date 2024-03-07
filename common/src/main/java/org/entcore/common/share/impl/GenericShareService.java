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

package org.entcore.common.share.impl;

import static fr.wseduc.webutils.Utils.getOrElse;
import static fr.wseduc.webutils.Utils.isEmpty;
import static org.entcore.common.neo4j.Neo4jResult.validResultHandler;
import static org.entcore.common.user.UserUtils.findVisibleProfilsGroups;
import static org.entcore.common.user.UserUtils.findVisibleUsers;
import static org.entcore.common.validation.StringValidation.cleanId;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.vertx.core.Promise;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;
import org.entcore.common.neo4j.StatementsBuilder;
import org.entcore.common.share.ShareInfosQuery;
import org.entcore.common.share.ShareService;
import org.entcore.common.user.UserUtils;
import org.entcore.common.validation.StringValidation;

import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.security.ActionType;
import fr.wseduc.webutils.security.SecuredAction;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public abstract class GenericShareService implements ShareService {

	protected static final Logger log = LoggerFactory.getLogger(GenericShareService.class);
	private static final String GROUP_SHARED = "MATCH (g:Group) WHERE g.id in {groupIds} "
			+ "RETURN distinct g.id as id, g.name as name, g.groupDisplayName as groupDisplayName, g.structureName as structureName "
			+ "ORDER BY name ";
	private static final String USER_SHARED = "MATCH (u:User) WHERE u.id in {userIds} "
			+ "RETURN distinct u.id as id, u.login as login, u.displayName as username, "
			+ "u.lastName as lastName, u.firstName as firstName, u.profiles[0] as profile  " + "ORDER BY username ";
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

	protected Future<Set<String>> userIdsForGroupIds(Set<String> groupsIds, String currentUserId) {
		@SuppressWarnings("rawtypes")
		List<Future> futures = groupsIds.stream().map(groupId -> {
			Future<Set<String>> future = Future.future();
			UserUtils.findUsersInProfilsGroups(groupId, eb, currentUserId, false, ev -> {
				Set<String> ids = new HashSet<>();
				if (ev != null) {
					for (Object o : ev) {
						if (!(o instanceof JsonObject))
							continue;
						JsonObject j = (JsonObject) o;
						String id = j.getString("id");
						ids.add(id);
					}
				}
				future.complete(ids);
			});
			return future;
		}).collect(Collectors.toList());
		return CompositeFuture.all(futures).map(result -> {
			List<Set<String>> all = result.list();
			return all.stream().reduce(new HashSet<String>(), (a1, a2) -> {
				a1.addAll(a2);
				return a1;
			});
		});
	}

	protected JsonArray getResoureActions(Map<String, SecuredAction> securedActions) {
		if (resourceActions != null) {
			return resourceActions;
		}
		JsonObject resourceActions = new JsonObject();
		for (SecuredAction action : securedActions.values()) {
			if (ActionType.RESOURCE.name().equals(action.getType()) && !action.getDisplayName().isEmpty()) {
				JsonObject a = resourceActions.getJsonObject(action.getDisplayName());
				if (a == null) {
					a = new JsonObject()
							.put("name",
									new fr.wseduc.webutils.collections.JsonArray()
											.add(action.getName().replaceAll("\\.", "-")))
							.put("displayName", action.getDisplayName()).put("type", action.getType());
					resourceActions.put(action.getDisplayName(), a);
				} else {
					a.getJsonArray("name").add(action.getName().replaceAll("\\.", "-"));
				}
			}
		}
		this.resourceActions = new fr.wseduc.webutils.collections.JsonArray(
				new ArrayList<>(resourceActions.getMap().values()));
		return this.resourceActions;
	}

	protected boolean hasOverridenActions(JsonArray inheritActions, JsonArray actions) {
		Set<String> inheritActionSet = inheritActions.stream().filter(act -> act instanceof String)
				.map(act -> (String) act).collect(Collectors.toSet());
		Set<String> actionSet = actions.stream().filter(act -> act instanceof String).map(act -> (String) act)
				.collect(Collectors.toSet());
		return !inheritActionSet.containsAll(actionSet) || !actionSet.containsAll(inheritActionSet);
	}
	
	@Override
	public void shareInfos(String userId, String resourceId, String acceptLanguage, String search, Handler<Either<String, JsonObject>> handler) {
		shareInfos(userId, resourceId, acceptLanguage, new ShareInfosQuery(search), handler);
	}
	
	protected void getInheritShareInfos(final String userId, final JsonArray actions,
			final JsonObject inheritGroupCheckedActions, final JsonObject inheritUserCheckedActions,
			final JsonObject groupCheckedActions, final JsonObject userCheckedActions, final String acceptLanguage,
			String search, final Handler<JsonObject> handler) {
		getShareInfos(userId, actions, inheritGroupCheckedActions, inheritUserCheckedActions, acceptLanguage, new ShareInfosQuery(search),
				res -> {
					if (res != null) {
						// if shared does not contains userId => it is from inherited shared
						{
							JsonObject users = res.getJsonObject("users");
							JsonObject checked = users.getJsonObject("checked");
							JsonObject checkedInherited = users.put("checkedInherited", new JsonObject())
									.getJsonObject("checkedInherited");
							Set<String> fieldNames = new HashSet<String>(checked.fieldNames());
							for (String cUserId : fieldNames) {
								if (userCheckedActions.containsKey(cUserId)) {
									// if it is in both inherit and not inhertid keep both and has not same actions
									JsonArray inheritUserActions = inheritUserCheckedActions.getJsonArray(cUserId);
									JsonArray userActions = userCheckedActions.getJsonArray(cUserId);
									if (hasOverridenActions(inheritUserActions, userActions)) {
										checkedInherited.put(cUserId, inheritUserActions);
										checked.put(cUserId, userActions);
									}
								} else {
									checkedInherited.put(cUserId, checked.getValue(cUserId));
									checked.remove(cUserId);
								}
							}
						}
						// if shared does not contains groupId => it is from inherited shared
						{
							JsonObject groups = res.getJsonObject("groups");
							JsonObject checked = groups.getJsonObject("checked");
							JsonObject checkedInherited = groups.put("checkedInherited", new JsonObject())
									.getJsonObject("checkedInherited");
							Set<String> fieldNames = new HashSet<String>(checked.fieldNames());
							for (String cGroupId : fieldNames) {
								// if it is in both inherit and not inhertid keep both
								if (groupCheckedActions.containsKey(cGroupId)) {
									// if it is in both inherit and not inhertid keep both and has not same actions
									JsonArray inheritGroupActions = inheritGroupCheckedActions.getJsonArray(cGroupId);
									JsonArray groupActions = groupCheckedActions.getJsonArray(cGroupId);
									if (hasOverridenActions(inheritGroupActions, groupActions)) {
										checkedInherited.put(cGroupId, inheritGroupActions);
										checked.put(cGroupId, groupActions);
									}
								} else {
									checkedInherited.put(cGroupId, checked.getValue(cGroupId));
									checked.remove(cGroupId);
								}
							}
						}
						handler.handle(res);
					} else {
						handler.handle(res);
					}
				});
	}

	protected void getShareInfos(final String userId, final JsonArray actions, final JsonObject groupCheckedActions,
			final JsonObject userCheckedActions, final String acceptLanguage, ShareInfosQuery query,
			final Handler<JsonObject> handler) {
		final JsonObject groupParams = new JsonObject().put("groupIds",
				new fr.wseduc.webutils.collections.JsonArray(new ArrayList<>(groupCheckedActions.fieldNames())));
		final JsonObject userParams = new JsonObject().put("userIds",
				new fr.wseduc.webutils.collections.JsonArray(new ArrayList<>(userCheckedActions.fieldNames())));
		final String search = query.getSearch();
		if (search != null && search.trim().isEmpty()) {
			final Neo4j neo4j = Neo4j.getInstance();
			neo4j.execute(GROUP_SHARED, groupParams, validResultHandler(sg-> {
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
					if (!(u instanceof JsonObject))
						continue;
					JsonObject group = (JsonObject) u;
					UserUtils.groupDisplayName(group, acceptLanguage);
				}
				neo4j.execute(USER_SHARED, userParams, validResultHandler(event-> {
					JsonArray visibleUsers;
					if (event.isRight()) {
						visibleUsers = event.right().getValue();
					} else {
						visibleUsers = new fr.wseduc.webutils.collections.JsonArray();
					}
					JsonObject users = new JsonObject();
					users.put("visibles", visibleUsers);
					users.put("checked", userCheckedActions);
					JsonObject share = new JsonObject().put("actions", actions).put("groups", groups)
							.put("users", users);
					handler.handle(share);
				}));
			}));
		} else {
			final String groupQuery = "RETURN distinct profileGroup.id as id, profileGroup.name as name, "
					+ "profileGroup.groupDisplayName as groupDisplayName, profileGroup.structureName as structureName "
					+ "ORDER BY name " + "UNION " + GROUP_SHARED;

			final String userQuery = "RETURN distinct visibles.id as id, visibles.login as login, visibles.displayName as username, "
					+ "visibles.lastName as lastName, visibles.firstName as firstName, visibles.profiles[0] as profile "
					+ "ORDER BY username " + "UNION " + USER_SHARED;
			//
			final StringBuilder preFilterUserBuilder = new StringBuilder();
			final StringBuilder preFilterGroupBuilder = new StringBuilder();
			//PREFILTER GROUPS AND USERS BY SEARCH
			if (search != null) {
				String sanitizedSearch = StringValidation.sanitize(search);
				groupParams.put("search", sanitizedSearch);
				userParams.put("search", sanitizedSearch);
				//
				preFilterUserBuilder.append(" AND m.displayNameSearchField CONTAINS {search} ");
				// historically this filter was not user. should it be?
				//preFilterGroupBuilder.append(" AND profileGroup.displayNameSearchField CONTAINS {search} ");
			}
			//PREFILTER GROUPS BY FILTER
			if(query.getOnlyGroupsWithFilters() != null && query.getOnlyGroupsWithFilters().size() > 0) {
				groupParams.put("onlyGroupsWithFilters", new JsonArray(query.getOnlyGroupsWithFilters()));
				preFilterGroupBuilder.append(" AND gp.filter IN {onlyGroupsWithFilters} ");
			}
			//PREFILTER USERS BY PROFILES
			if(query.getOnlyUsersWithProfiles() != null && query.getOnlyUsersWithProfiles().size() > 0) {
				userParams.put("onlyUserWithProfiles", new JsonArray(query.getOnlyUsersWithProfiles()));
				preFilterUserBuilder.append(" AND ANY(pro IN m.profiles WHERE pro IN {onlyUserWithProfiles}) ");
			}
			//
			String preFilterGroup = preFilterGroupBuilder.toString().trim().isEmpty() ? null : preFilterGroupBuilder.toString();
			UserUtils.findVisibleProfilsGroups(eb, userId, preFilterGroup, groupQuery, groupParams, visibleGroups-> {
				final JsonObject groups = new JsonObject();
				groups.put("visibles", visibleGroups);
				groups.put("checked", groupCheckedActions);
				for (Object u : visibleGroups) {
					if (!(u instanceof JsonObject))
						continue;
					JsonObject group = (JsonObject) u;
					UserUtils.groupDisplayName(group, acceptLanguage);
				}
				//
				String preFilterUser = preFilterUserBuilder.toString().trim().isEmpty() ? null : preFilterUserBuilder.toString();
				findVisibleUsers(eb, userId, false, preFilterUser, userQuery, userParams,visibleUsers-> {
					JsonObject users = new JsonObject();
					users.put("visibles", visibleUsers);
					users.put("checked", userCheckedActions);
					JsonObject share = new JsonObject().put("actions", actions).put("groups", groups)
							.put("users", users);
					handler.handle(share);
				});
			});
		}
	}

	// TODO improve query
	protected void profilGroupIsVisible(String userId, final String groupId, final Handler<Boolean> handler) {
		if (userId == null || groupId == null) {
			handler.handle(false);
			return;
		}
		findVisibleProfilsGroups(eb, userId, true, new Handler<JsonArray>() {
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
			for (String action : securedActions.keySet()) {
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

	protected void shareValidation(String resourceId, String userId, JsonObject share,
			Handler<Either<String, JsonObject>> handler) {
		final JsonObject groups = share.getJsonObject("groups");
		final JsonObject users = share.getJsonObject("users");
		final JsonObject shareBookmark = share.getJsonObject("bookmarks");
		final Map<String, Set<String>> membersActions = new HashMap<>();
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
				for (String sbId : shareBookmark.fieldNames()) {
					final String csbId = cleanId(sbId);
					final String query = "MATCH (:User {id:{userId}})-[:HAS_SB]->(sb:ShareBookmark) " + "RETURN DISTINCT '"
							+ csbId + "' as id, TAIL(sb." + csbId + ") as members ";
					statements.add(query, p);
				}
				Neo4j.getInstance().executeTransaction(statements.build(), null, true,
						Neo4jResult.validResultsHandler(sbRes -> {
							if (sbRes.isRight()) {
								JsonArray a = sbRes.right().getValue();
								for (Object o : a) {
									JsonObject r = ((JsonArray) o).getJsonObject(0);
									JsonArray actions = shareBookmark.getJsonArray(r.getString("id"));
									JsonArray mIds = r.getJsonArray("members");
									if (actions != null && mIds != null && mIds.size() > 0
											&& actionsExists(actions.getList())) {
										for (Object mId : mIds) {
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
								shareValidationVisible(userId, resourceId, handler, membersActions,
										shareBookmark.fieldNames());
							} else {
								handler.handle(new Either.Left<>(sbRes.left().getValue()));
							}
						}));
			} else {
				shareValidationVisible(userId, resourceId, handler, membersActions, null);
			}
	}

	/**
	 * Check that the user can set the specified shares to a resource.
	 * It allows the user to :
	 * <ul>
	 *   <li>let unchanged shares on users or groups that are not visible</li>
	 *   <li>change or delete rights on users or groups only if they are within
	 *   the visible users and groups of the user</li>
	 * </ul>
	 * @param userId Id of the user who wants to perform the update
	 * @param originalShares Actual shares of the user
	 * @param shareUpdates Shares that the user wants to apply
	 * @return A {@code Future} that completes with {@code true} iff all the detailed conditions
	 * above were met and {@code false} otherwise.
	 */
	private Future<Boolean> checkCanApplyShares(final String userId,
																					 final JsonArray originalShares,
																					 final Map<String, Set<String>> shareUpdates) {
		final Promise<Boolean> promise = Promise.promise();
		final String customReturn = "RETURN DISTINCT visibles.id as id, has(visibles.login) as isUser";
		UserUtils.findVisibles(eb, userId, customReturn, null, true, true, false, "fr", visibleResponse -> {
			final Set<String> visibleUsersAndGroups = visibleResponse.stream()
					.map(entry -> ((JsonObject) entry).getString("id"))
					.collect(Collectors.toSet());
			// Check that original shares are untouched or that the ones that are modified are modified accordingly to
			// users/groups visibility
			boolean ok = true;
			final Set<String> originalUsersAndGroups = new HashSet<>();
			for (Object originalShare : originalShares) {
				final JsonObject share = (JsonObject) originalShare;
				final String idOfShare = getUserOrGroupIdOfShare(share);
				originalUsersAndGroups.add(idOfShare);
				if(visibleUsersAndGroups.contains(idOfShare)) {
					log.debug(idOfShare + " is visible so it can be changed");
				} else {
					final Set<String> originalRights = share.stream()
							.filter(e -> !e.getKey().equals("userId") && !e.getKey().equals("groupId") && (Boolean) e.getValue())
							.map(Map.Entry::getKey)
							.collect(Collectors.toSet());
					final Set<String> desiredRights = shareUpdates.getOrDefault(idOfShare, Collections.emptySet());
					if(desiredRights.equals(originalRights)) {
						log.debug("OK - desired rights and original rights are the same for " + idOfShare);
					} else {
						log.warn("KO - desired rights and original rights differ for " + idOfShare + " but the user has no visibility on it");
						ok = false;
						break;
					}
				}
			}
			if(ok) {
				// Check that added groups or users do not concern users or groups that the user does not have access to
				ok = shareUpdates.keySet().stream()
						.filter(id -> !originalUsersAndGroups.contains(id)) // Added users and groups
						.allMatch(id -> {
							if(visibleUsersAndGroups.contains(id)) {
								return true;
							} else {
								log.warn("KO - tried to add rights to a user/group " + id + " not visible to user");
								return false;
							}
						});
			}
			promise.complete(ok);
		});
		return promise.future();
	}

	private String getUserOrGroupIdOfShare(final JsonObject share) {
		String id = share.getString("groupId");
		if(isEmpty(id)) {
			id = share.getString("userId");
		}
		return id;
	}

	private Future<JsonArray> getOriginalShares(final String resourceId,
																							 final String userId) {
		final Promise<JsonArray> promise = Promise.promise();
		shareInfosWithoutVisible(userId, resourceId, e -> {
			if(e.isLeft()) {
				promise.fail(e.left().getValue());
			} else {
				promise.complete(e.right().getValue());
			}
		});
		return promise.future();
	}


	private void shareValidationVisible(final String userId, final String resourceId,
																			final Handler<Either<String, JsonObject>> handler,
																			final Map<String, Set<String>> membersActions,
																			final Set<String> shareBookmarkIds) {
		getOriginalShares(resourceId, userId)
		.compose(shares -> checkCanApplyShares(userId, shares, membersActions))
		.onSuccess(e -> {
			if(e) {
				//		final String preFilter = "AND m.id IN {members} ";
				final Set<String> members = membersActions.keySet();
				final JsonObject params = new JsonObject().put("members", new JsonArray(new ArrayList<>(members)));
				//		final String customReturn = "RETURN DISTINCT visibles.id as id, has(visibles.login) as isUser";
				//		UserUtils.findVisibles(eb, userId, customReturn, params, true, true, false, null, preFilter, res -> {
				checkMembers(params, res -> {
					if (res != null) {
						final JsonArray users = new JsonArray();
						final JsonArray groups = new JsonArray();
						final JsonArray shared = new JsonArray();
						final JsonArray notifyMembers = new JsonArray();
						for (Object o : res) {
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
						handler.handle(new Either.Right<>(params.put("shared", shared).put("groups", groups).put("users", users)
								.put("notify-members", notifyMembers)));
						if (shareBookmarkIds != null && res.size() < members.size()) {
							members.removeAll(groups.getList());
							members.removeAll(users.getList());
							resyncShareBookmark(userId, members, shareBookmarkIds);
						}
					} else {
						handler.handle(new Either.Left<>("Invalid members count."));
					}
				});
			} else {
				handler.handle(new Either.Left<>("insufficient.rights.to.modify.shares"));
			}
		})
		.onFailure(th -> handler.handle(new Either.Left<>(th.getMessage())));
	}

	private void checkMembers(JsonObject params, Handler<JsonArray> handler) {
		final String query = "MATCH (v:Visible) "
				+ "WHERE v.id IN {members} AND NOT(HAS(v.deleteDate)) AND (NOT(HAS(v.blocked)) OR v.blocked = false) "
				+ "RETURN DISTINCT v.id as id, has(v.login) as isUser ";
		Neo4j.getInstance().execute(query, params, event -> {
			if ("ok".equals(event.body().getString("status"))) {
				handler.handle(event.body().getJsonArray("result"));
			} else {
				handler.handle(null);
			}
		});
	}

	private void resyncShareBookmark(String userId, Set<String> members, Set<String> shareBookmarkIds) {
		final StringBuilder query = new StringBuilder("MATCH (:User {id:{userId}})-[:HAS_SB]->(sb:ShareBookmark) SET ");
		for (String sb : shareBookmarkIds) {
			query.append("sb.").append(sb).append(" = FILTER(mId IN sb.").append(sb)
					.append(" WHERE NOT(mId IN {members})), ");
		}
		JsonObject params = new JsonObject().put("userId", userId).put("members",
				new JsonArray(new ArrayList<>(members)));
		Neo4j.getInstance().execute(query.substring(0, query.length() - 2), params, res -> {
			if ("ok".equals(res.body().getString("status"))) {
				log.info("Resync share bookmark for user " + userId);
			} else {
				log.error("Error when resync share bookmark for user " + userId + " : "
						+ res.body().getString("message"));
			}
		});
	}

	protected void getNotifyMembers(Handler<Either<String, JsonObject>> handler, JsonArray oldShared, JsonArray members,
			Function<Object, String> f) {
		JsonArray notifyMembers;
		if (oldShared != null && oldShared.size() > 0 && members != null && members.size() > 0) {
			final Set<String> oldMembersIds = oldShared.stream().map(f).collect(Collectors.toSet());
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

	protected abstract void prepareSharedArray(String resourceId, String type, JsonArray shared, String attr,
			Set<String> actions);

	protected List<String> findRemoveActions(List<String> removeActions) {
		if (removeActions == null || removeActions.isEmpty()) {
			return null;
		}
		List<String> rmActions = new ArrayList<>(removeActions);
		if (groupedActions != null) {
			for (Map.Entry<String, List<String>> ga : groupedActions.entrySet()) {
				for (String a : removeActions) {
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
