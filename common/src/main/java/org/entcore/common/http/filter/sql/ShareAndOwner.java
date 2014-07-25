/*
 * Copyright © WebServices pour l'Éducation, 2014
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
 */

package org.entcore.common.http.filter.sql;

import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import fr.wseduc.mongodb.MongoQueryBuilder;
import fr.wseduc.webutils.http.Binding;
import org.entcore.common.http.filter.MongoAppFilter;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.mongodb.MongoDbConf;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlConf;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class ShareAndOwner implements ResourcesProvider {

	private SqlConf conf = SqlConf.getInstance();

	@Override
	public void authorize(HttpServerRequest request, Binding binding, UserInfos user, final Handler<Boolean> handler) {
		String id = request.params().get(conf.getResourceIdLabel());
		if (id != null && !id.trim().isEmpty()) {
			String sharedMethod = binding.getServiceMethod().replaceAll("\\.", "-");
			List<String> gu = new ArrayList<>();
			gu.add(user.getUserId());
			if (user.getProfilGroupsIds() != null) {
				gu.addAll(user.getProfilGroupsIds());
			}
			final Object[] groupsAndUserIds = gu.toArray();
			String query =
					"SELECT count(*) FROM " + conf.getSchema() + conf.getTable() +
					" LEFT JOIN " + conf.getSchema() + "shares ON id = resource_id " +
					"WHERE ((member_id IN " + Sql.listPrepared(groupsAndUserIds) + " AND action = ?) " +
					"OR owner = ?) AND id = ?";
			JsonArray values = new JsonArray(groupsAndUserIds).add(sharedMethod)
					.add(user.getUserId()).add(Sql.parseId(id));
			Sql.getInstance().prepared(query, values, new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> message) {
					Long count = SqlResult.countResult(message);
					handler.handle(count != null && count > 0);
				}
			});
		} else {
			handler.handle(false);
		}
	}

}
