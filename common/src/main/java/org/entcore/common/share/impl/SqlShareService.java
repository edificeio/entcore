/*
 * Copyright © "Open Digital Education", 2014
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

 */

package org.entcore.common.share.impl;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.entcore.common.share.ShareInfosQuery;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.sql.SqlStatementsBuilder;

import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.security.SecuredAction;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class SqlShareService extends GenericShareService {

	private final Sql sql;
	private final String schema;
	private final String shareTable;

	public SqlShareService(EventBus eb, Map<String, SecuredAction> securedActions,
			Map<String, List<String>> groupedActions) {
		this(null, null, eb, securedActions, groupedActions);
	}

	public SqlShareService(String schema, String shareTable, EventBus eb, Map<String, SecuredAction> securedActions,
			Map<String, List<String>> groupedActions) {
		super(eb, securedActions, groupedActions);
		sql = Sql.getInstance();
		this.schema = (schema != null && !schema.trim().isEmpty()) ? schema + "." : "";
		this.shareTable = this.schema + ((shareTable != null && !shareTable.trim().isEmpty()) ? shareTable : "shares");
	}

	@Override
	public void inheritShareInfos(String userId, String resourceId, String acceptLanguage, String search,
			Handler<Either<String, JsonObject>> handler) {
		// TODO implement inherit shares
		throw new UnsupportedOperationException("Postgres not implemented yet");
	}

	@Override
	public void findUserIdsForInheritShare(String resourceId, String userId, Optional<Set<String>> actions,
			Handler<AsyncResult<Set<String>>> h) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Postgres not implemented yet");
	}

	@Override
	public void findUserIdsForShare(String resourceId, String userId, Optional<Set<String>> actions,
			Handler<AsyncResult<Set<String>>> h) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Postgres not implemented yet");
	}

	@Override
	public void shareInfos(final String userId, String resourceId, final String acceptLanguage, final ShareInfosQuery search,
			final Handler<Either<String, JsonObject>> handler) {
		if (userId == null || userId.trim().isEmpty()) {
			handler.handle(new Either.Left<String, JsonObject>("Invalid userId."));
			return;
		}
		if (resourceId == null || resourceId.trim().isEmpty()) {
			handler.handle(new Either.Left<String, JsonObject>("Invalid resourceId."));
			return;
		}
		final JsonArray actions = getResoureActions(securedActions);
		String query = "SELECT s.member_id, s.action, m.group_id FROM " + shareTable + " AS s " + "JOIN " + schema
				+ "members AS m ON s.member_id = m.id WHERE resource_id = ?";
		sql.prepared(query, new fr.wseduc.webutils.collections.JsonArray().add(Sql.parseId(resourceId)),
				new Handler<Message<JsonObject>>() {
					@Override
					public void handle(Message<JsonObject> message) {
						if ("ok".equals(message.body().getString("status"))) {
							JsonArray r = message.body().getJsonArray("results");
							JsonObject groupCheckedActions = new JsonObject();
							JsonObject userCheckedActions = new JsonObject();
							for (Object o : r) {
								if (!(o instanceof JsonArray))
									continue;
								JsonArray row = (JsonArray) o;
								final String memberId = row.getString(0);
								if (memberId == null || memberId.equals(userId))
									continue;
								final JsonObject checkedActions = (row.getValue(2) != null) ? groupCheckedActions
										: userCheckedActions;
								JsonArray m = checkedActions.getJsonArray(memberId);
								if (m == null) {
									m = new fr.wseduc.webutils.collections.JsonArray();
									checkedActions.put(memberId, m);
								}
								m.add(row.getValue(1));
							}
							getShareInfos(userId, actions, groupCheckedActions, userCheckedActions, acceptLanguage,
									search, new Handler<JsonObject>() {
										@Override
										public void handle(JsonObject event) {
											if (event != null && event.size() == 3) {
												handler.handle(new Either.Right<String, JsonObject>(event));
											} else {
												handler.handle(new Either.Left<String, JsonObject>(
														"Error finding shared resource."));
											}
										}
									});
						}
					}
				});
	}

	@Override
	public void groupShare(final String userId, final String groupShareId, final String resourceId,
			final List<String> actions, final Handler<Either<String, JsonObject>> handler) {
		inShare(resourceId, groupShareId, new Handler<Boolean>() {

			@Override
			public void handle(Boolean event) {
				if (Boolean.TRUE.equals(event)) {
					share(resourceId, groupShareId, actions, "groups", handler);
				} else {
					groupShareValidation(userId, groupShareId, actions, new Handler<Either<String, JsonObject>>() {
						@Override
						public void handle(Either<String, JsonObject> event) {
							if (event.isRight()) {
								share(resourceId, groupShareId, actions, "groups", handler);
							} else {
								handler.handle(event);
							}
						}
					});
				}
			}
		});
	}

	private void inShare(String resourceId, String shareId, final Handler<Boolean> handler) {
		String query = "SELECT count(*) FROM " + shareTable + " WHERE resource_id = ? AND member_id = ?";
		JsonArray params = new fr.wseduc.webutils.collections.JsonArray().add(Sql.parseId(resourceId)).add(shareId);
		sql.prepared(query, params, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> message) {
				Long count = SqlResult.countResult(message);
				handler.handle(count != null && count > 0);
			}
		});
	}

	@Override
	public void userShare(final String userId, final String userShareId, final String resourceId,
			final List<String> actions, final Handler<Either<String, JsonObject>> handler) {
		inShare(resourceId, userShareId, new Handler<Boolean>() {
			@Override
			public void handle(Boolean event) {
				if (Boolean.TRUE.equals(event)) {
					share(resourceId, userShareId, actions, "users", handler);
				} else {
					userShareValidation(userId, userShareId, actions, new Handler<Either<String, JsonObject>>() {
						@Override
						public void handle(Either<String, JsonObject> event) {
							if (event.isRight()) {
								share(resourceId, userShareId, actions, "users", handler);
							} else {
								handler.handle(event);
							}
						}
					});
				}
			}
		});
	}

	@Override
	public void removeGroupShare(String groupId, String resourceId, List<String> actions,
			Handler<Either<String, JsonObject>> handler) {
		removeShare(resourceId, groupId, actions, handler);
	}

	@Override
	public void removeUserShare(String userId, String resourceId, List<String> actions,
			Handler<Either<String, JsonObject>> handler) {
		removeShare(resourceId, userId, actions, handler);
	}

	@Override
	public void share(String userId, String resourceId, JsonObject share, Handler<Either<String, JsonObject>> handler) {
		shareValidation(resourceId, userId, share, res -> {
			if (res.isRight()) {
				final SqlStatementsBuilder s = new SqlStatementsBuilder();
				s.prepared("SELECT member_id FROM " + shareTable + " WHERE resource_id = ?",
						new JsonArray().add(resourceId));
				s.prepared("DELETE FROM " + shareTable + " WHERE resource_id = ?", new JsonArray().add(resourceId));
				final JsonArray users = res.right().getValue().getJsonArray("users");
				if (users != null && users.size() > 0) {
					s.raw("LOCK TABLE " + schema + "users IN SHARE ROW EXCLUSIVE MODE");
					for (Object u : users) {
						s.raw("INSERT INTO " + schema + "users (id) SELECT '" + u.toString()
								+ "' WHERE NOT EXISTS (SELECT * FROM " + schema + "users WHERE id='" + u.toString()
								+ "');");
					}
				}
				final JsonArray groups = res.right().getValue().getJsonArray("groups");
				if (groups != null && groups.size() > 0) {
					s.raw("LOCK TABLE " + schema + "groups IN SHARE ROW EXCLUSIVE MODE");
					for (Object g : groups) {
						s.raw("INSERT INTO " + schema + "groups (id) SELECT '" + g.toString()
								+ "' WHERE NOT EXISTS (SELECT * FROM " + schema + "groups WHERE id='" + g.toString()
								+ "');");
					}
				}
				s.insert(shareTable, new JsonArray().add("member_id").add("resource_id").add("action"),
						res.right().getValue().getJsonArray("shared"));
				sql.transaction(s.build(), SqlResult.validResultHandler(0, old -> {
					if (old.isRight()) {
						JsonArray oldMembers = old.right().getValue();
						JsonArray members = res.right().getValue().getJsonArray("notify-members");
						getNotifyMembers(handler, oldMembers, members, (m -> ((JsonObject) m).getString("member_id")));
					} else {
						handler.handle(new Either.Left<>(old.left().getValue()));
					}
				}));
			} else {
				handler.handle(res);
			}
		});
	}

	private void removeShare(String resourceId, String userId, List<String> actions,
			Handler<Either<String, JsonObject>> handler) {
		String actionFilter;
		JsonArray values;
		if (actions != null && actions.size() > 0) {
			Object[] a = actions.toArray();
			actionFilter = "action IN " + Sql.listPrepared(a) + " AND ";
			values = new fr.wseduc.webutils.collections.JsonArray(actions);
		} else {
			actionFilter = "";
			values = new fr.wseduc.webutils.collections.JsonArray();
		}
		String query = "DELETE FROM " + shareTable + " WHERE " + actionFilter + "resource_id = ? AND member_id = ?";
		values.add(Sql.parseId(resourceId)).add(userId);
		sql.prepared(query, values, SqlResult.validUniqueResultHandler(handler));
	}

	private void share(String resourceId, final String shareId, List<String> actions, final String membersTable,
			final Handler<Either<String, JsonObject>> handler) {
		final SqlStatementsBuilder s = new SqlStatementsBuilder();
		s.raw("LOCK TABLE " + schema + membersTable + " IN SHARE ROW EXCLUSIVE MODE");
		s.raw("LOCK TABLE " + shareTable + " IN SHARE ROW EXCLUSIVE MODE");
		s.raw("INSERT INTO " + schema + membersTable + " (id) SELECT '" + shareId + "' WHERE NOT EXISTS (SELECT * FROM "
				+ schema + membersTable + " WHERE id='" + shareId + "');");
		final Object rId = Sql.parseId(resourceId);
		final String query = "INSERT INTO " + shareTable
				+ " (member_id, resource_id, action) SELECT ?, ?, ? WHERE NOT EXISTS " + "(SELECT * FROM " + shareTable
				+ " WHERE member_id = ? AND resource_id = ? AND action = ?);";
		for (String action : actions) {
			JsonArray ar = new fr.wseduc.webutils.collections.JsonArray().add(shareId).add(rId).add(action).add(shareId)
					.add(rId).add(action);
			s.prepared(query, ar);
		}
		sql.prepared("SELECT count(*) FROM " + shareTable + " WHERE member_id = ? AND resource_id = ?",
				new fr.wseduc.webutils.collections.JsonArray().add(shareId).add(Sql.parseId(resourceId)),
				new Handler<Message<JsonObject>>() {
					@Override
					public void handle(final Message<JsonObject> message) {
						final Long nb = SqlResult.countResult(message);
						sql.transaction(s.build(), new Handler<Message<JsonObject>>() {
							@Override
							public void handle(Message<JsonObject> res) {
								Either<String, JsonObject> r = SqlResult.validUniqueResult(2, res);
								if (r.isRight() && nb == 0) {
									JsonObject notify = new JsonObject();
									notify.put(membersTable.substring(0, membersTable.length() - 1) + "Id", shareId);
									r.right().getValue().put("notify-timeline", notify);
								}
								handler.handle(r);
							}
						});
					}
				});
	}

	@Override
	protected void prepareSharedArray(String resourceId, String type, JsonArray shared, String attr,
			Set<String> actions) {
		for (String action : actions) {
			shared.add(new JsonArray().add(attr).add(resourceId).add(action));
		}

	}

}
