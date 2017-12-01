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

package org.entcore.common.http.filter;

import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import fr.wseduc.mongodb.MongoQueryBuilder;
import fr.wseduc.webutils.http.Binding;
import org.entcore.common.mongodb.MongoDbConf;
import org.entcore.common.user.UserInfos;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;

import java.util.ArrayList;
import java.util.List;

public class ShareAndOwner implements ResourcesProvider {

	private MongoDbConf conf = MongoDbConf.getInstance();

	@Override
	public void authorize(HttpServerRequest request, Binding binding, UserInfos user, Handler<Boolean> handler) {
		String id = request.params().get(conf.getResourceIdLabel());
		if (id != null && !id.trim().isEmpty()) {
			List<DBObject> groups = new ArrayList<>();
			String sharedMethod = binding.getServiceMethod().replaceAll("\\.", "-");
			groups.add(QueryBuilder.start("userId").is(user.getUserId())
					.put(sharedMethod).is(true).get());
			for (String gpId: user.getGroupsIds()) {
				groups.add(QueryBuilder.start("groupId").is(gpId)
						.put(sharedMethod).is(true).get());
			}
			QueryBuilder query = QueryBuilder.start("_id").is(id).or(
					QueryBuilder.start("owner.userId").is(user.getUserId()).get(),
					QueryBuilder.start("shared").elemMatch(
							new QueryBuilder().or(groups.toArray(new DBObject[groups.size()])).get()).get()
			);
			MongoAppFilter.executeCountQuery(request, conf.getCollection(), MongoQueryBuilder.build(query), 1, handler);
		} else {
			handler.handle(false);
		}
	}

}
