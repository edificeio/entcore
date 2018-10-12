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

package org.entcore.common.service.impl;

import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.collections.Joiner;
import org.entcore.common.service.CrudService;
import org.entcore.common.service.VisibilityFilter;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlStatementsBuilder;
import org.entcore.common.user.UserInfos;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

import static org.entcore.common.sql.Sql.parseId;
import static org.entcore.common.sql.SqlResult.*;

public class SqlCrudService implements CrudService {

	protected final String resourceTable;
	protected final String shareTable;
	protected final Sql sql;
	protected final JsonArray defaultRetrieveValues;
	protected final JsonArray defaultListValues;
	protected final String schema;
	protected final String table;
	protected final boolean shared;

	public SqlCrudService(String table) {
		this(null, table, null, null, null, false);
	}

	public SqlCrudService(String schema, String table) {
		this(schema, table, null, null, null, false);
	}

	public SqlCrudService(String schema, String table, String shareTable) {
		this(schema, table, shareTable, null, null, false);
	}

	public SqlCrudService(String schema, String table, String shareTable,
			JsonArray defaultRetrieveValues, JsonArray defaultListValues) {
		this(schema, table, shareTable, defaultRetrieveValues, defaultListValues, false);
	}

	public SqlCrudService(String schema, String table, String shareTable,
			JsonArray defaultRetrieveValues, JsonArray defaultListValues, boolean shared) {
		this.table = table;
		this.sql = Sql.getInstance();
		this.defaultRetrieveValues = defaultRetrieveValues;
		this.defaultListValues = defaultListValues;
		this.shared = shared;
		if (schema != null && !schema.trim().isEmpty()) {
			this.resourceTable = schema + "." + table;
			this.schema = schema + ".";
			this.shareTable = this.schema+((shareTable != null && !shareTable.trim().isEmpty()) ? shareTable : "shares");
		} else {
			this.resourceTable = table;
			this.schema = "";
			this.shareTable = (shareTable != null && !shareTable.trim().isEmpty()) ? shareTable : "shares";
		}
	}

	@Override
	public void create(JsonObject data, UserInfos user, final Handler<Either<String, JsonObject>> handler) {
		SqlStatementsBuilder s = new SqlStatementsBuilder();
//		String userQuery = Sql.upsert(resourceTable,
//				"UPDATE users SET username = '" + user.getUsername() + "' WHERE id = '" + user.getUserId() + "'",
//				"INSERT INTO users (username, id) SELECT '" + user.getUsername() + "','" + user.getUserId() + "'"
//		);
//		s.raw(userQuery);
		String userQuery = "SELECT " + schema + "merge_users(?,?)";
		s.prepared(userQuery, new fr.wseduc.webutils.collections.JsonArray().add(user.getUserId()).add(user.getUsername()));
		data.put("owner", user.getUserId());
		s.insert(resourceTable, data, "id");
		sql.transaction(s.build(), validUniqueResultHandler(1, handler));
	}

	@Override
	public void retrieve(String id, final Handler<Either<String, JsonObject>> handler) {
		if (shared) {
			String query = "SELECT " + expectedValues() +
					", json_agg(row_to_json(row(member_id,action)::" + schema + "share_tuple)) as shared, " +
					" array_to_json(array_agg(group_id)) as groups " +
					" FROM " + resourceTable +
					" LEFT JOIN " + shareTable + " ON " + resourceTable + ".id = resource_id" +
					" LEFT JOIN " + schema + "members ON (member_id = " + schema + "members.id AND group_id IS NOT NULL) " +
					" WHERE " + resourceTable + ".id = ? " +
					" GROUP BY " + resourceTable + ".id";
			sql.prepared(query, new fr.wseduc.webutils.collections.JsonArray().add(parseId(id)), parseSharedUnique(handler));
		} else {
			String query = "SELECT " + expectedValues() + " FROM " + resourceTable + " WHERE id = ?";
			sql.prepared(query, new fr.wseduc.webutils.collections.JsonArray().add(parseId(id)), validUniqueResultHandler(handler));
		}
	}

	@Override
	public void retrieve(String id, UserInfos user, Handler<Either<String, JsonObject>> handler) {
		String filter = user == null ? " AND visibility = '" + VisibilityFilter.PUBLIC.name() + "'" : "";
		String query = "SELECT " + expectedValues() + " FROM " + resourceTable + " WHERE id = ?" + filter;
		sql.prepared(query, new fr.wseduc.webutils.collections.JsonArray().add(parseId(id)), validUniqueResultHandler(handler));
	}

	@Override
	public void update(String id, JsonObject data, Handler<Either<String, JsonObject>> handler) {
		update(id, data, null, handler);
	}

	@Override
	public void update(String id, JsonObject data, UserInfos user, Handler<Either<String, JsonObject>> handler) {
		StringBuilder sb = new StringBuilder();
		JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
		for (String attr : data.fieldNames()) {
			sb.append(attr).append(" = ?, ");
			values.add(data.getValue(attr));
		}
		String query =
				"UPDATE " + resourceTable +
				" SET " + sb.toString() + "modified = NOW() " +
				"WHERE id = ? ";
		sql.prepared(query, values.add(parseId(id)), validRowsResultHandler(handler));
	}

	@Override
	public void delete(String id, Handler<Either<String, JsonObject>> handler) {
		delete(id, null, handler);
	}

	@Override
	public void delete(String id, UserInfos user, Handler<Either<String, JsonObject>> handler) {
		String query = "DELETE FROM " + resourceTable + " WHERE id = ?";
		sql.prepared(query, new fr.wseduc.webutils.collections.JsonArray().add(parseId(id)), validRowsResultHandler(handler));
	}

	@Override
	public void list(Handler<Either<String, JsonArray>> handler) {
		if (shared) {
			String query =
					"SELECT " + expectedListValues() +
					", json_agg(row_to_json(row(member_id,action)::" + schema + "share_tuple)) as shared, " +
					"array_to_json(array_agg(group_id)) as groups FROM " + resourceTable +
					" LEFT JOIN " + shareTable + " ON " + resourceTable + ".id = resource_id" +
					" LEFT JOIN " + schema + "members ON (member_id = " + schema + "members.id AND group_id IS NOT NULL) " +
					" GROUP BY " + resourceTable + ".id";
			sql.raw(query, parseShared(handler));
		} else {
			sql.select(resourceTable, defaultListValues, validResultHandler(handler));
		}
	}

	@Override
	public void list(VisibilityFilter filter, UserInfos user, Handler<Either<String, JsonArray>> handler) {
		String query;
		JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
		if (user != null) {
			List<String> gu = new ArrayList<>();
			gu.add(user.getUserId());
			if (user.getGroupsIds() != null) {
				gu.addAll(user.getGroupsIds());
			}
			final Object[] groupsAndUserIds = gu.toArray();
			switch (filter) {
				case OWNER:
					query = "SELECT " + expectedListValues() + " FROM " + resourceTable + " WHERE owner = ?";
					values.add(user.getUserId());
					break;
				case OWNER_AND_SHARED:
					query = "SELECT DISTINCT " + expectedListValues() + " FROM " + resourceTable +
							" LEFT JOIN " + shareTable + " ON id = resource_id " +
							"WHERE member_id IN " + Sql.listPrepared(groupsAndUserIds) + " OR owner = ?";
					values = new fr.wseduc.webutils.collections.JsonArray(gu).add(user.getUserId());
					break;
				case SHARED:
					query = "SELECT DISTINCT " + expectedListValues() + " FROM " + resourceTable +
							" INNER JOIN " + shareTable + " ON id = resource_id " +
							"WHERE member_id IN " + Sql.listPrepared(groupsAndUserIds);
					values = new fr.wseduc.webutils.collections.JsonArray(gu);
					break;
				case PROTECTED:
					query = "SELECT " + expectedListValues() + " FROM " + resourceTable + " WHERE visibility = ?";
					values.add(VisibilityFilter.PROTECTED.name());
					break;
				case PUBLIC:
					query = "SELECT " + expectedListValues() + " FROM " + resourceTable + " WHERE visibility = ?";
					values.add(VisibilityFilter.PUBLIC.name());
					break;
				default:
					query = "SELECT " + expectedListValues() +
							", json_agg(row_to_json(row(member_id,action)::" + schema + "share_tuple)) as shared, " +
							"array_to_json(array_agg(group_id)) as groups FROM " + resourceTable +
							" LEFT JOIN " + shareTable + " ON " + resourceTable + ".id = resource_id" +
							" LEFT JOIN " + schema + "members ON (member_id = " + schema + "members.id AND group_id IS NOT NULL) " +
							"WHERE member_id IN " + Sql.listPrepared(groupsAndUserIds) +
							" OR owner = ? OR visibility IN (?,?) " +
							" GROUP BY " + resourceTable + ".id";
					values = new fr.wseduc.webutils.collections.JsonArray(gu).add(user.getUserId())
						.add(VisibilityFilter.PROTECTED.name())
						.add(VisibilityFilter.PUBLIC.name());
					break;
			}
		} else {
			query = "SELECT " + expectedListValues() + " FROM " + resourceTable + " WHERE visibility = ?";
			values.add(VisibilityFilter.PUBLIC.name());
		}
		query += " ORDER BY modified DESC";
		Handler<Message<JsonObject>> h = (VisibilityFilter.ALL.equals(filter)) ?
				parseShared(handler) : validResultHandler(handler);
		sql.prepared(query, values, h);
	}

	@Override
	public void isOwner(String id, UserInfos user, final Handler<Boolean> handler) {
		String query = "SELECT count(*) FROM " + resourceTable + " WHERE owner = ? AND id = ?";
		sql.prepared(query, new fr.wseduc.webutils.collections.JsonArray().add(user.getUserId()).add(parseId(id)), new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> message) {
				Long count = countResult(message);
				handler.handle(count != null && count == 1);
			}
		});
	}

	private String expectedValues() {
		return defaultRetrieveValues != null ?
				resourceTable + "." + Joiner.on(", " + resourceTable + ".").join(defaultRetrieveValues) : "*";
	}

	private String expectedListValues() {
		return defaultListValues != null ?
				resourceTable + "." + Joiner.on(", " + resourceTable + ".").join(defaultListValues) : "*";
	}

}
