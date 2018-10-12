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

package org.entcore.common.http.filter.sql;

import fr.wseduc.webutils.http.Binding;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlConf;
import org.entcore.common.sql.SqlConfs;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class ShareAndOwner implements ResourcesProvider {

	@Override
	public void authorize(final HttpServerRequest request, Binding binding, UserInfos user,
			final Handler<Boolean> handler) {
		SqlConf conf = SqlConfs.getConf(binding.getServiceMethod().substring(0, binding.getServiceMethod().indexOf('|')));
		String id = request.params().get(conf.getResourceIdLabel());
		if (id != null && !id.trim().isEmpty()) {
			request.pause();
			String sharedMethod = binding.getServiceMethod().replaceAll("\\.", "-");
			List<String> gu = new ArrayList<>();
			gu.add(user.getUserId());
			if (user.getGroupsIds() != null) {
				gu.addAll(user.getGroupsIds());
			}
			final Object[] groupsAndUserIds = gu.toArray();
			String query =
					"SELECT count(*) FROM " + conf.getSchema() + conf.getTable() +
					" LEFT JOIN " + conf.getSchema() + conf.getShareTable() + " ON id = resource_id " +
					"WHERE ((member_id IN " + Sql.listPrepared(groupsAndUserIds) + " AND action = ?) " +
					"OR owner = ?) AND id = ?";
			JsonArray values = new fr.wseduc.webutils.collections.JsonArray(gu).add(sharedMethod)
					.add(user.getUserId()).add(Sql.parseId(id));
			Sql.getInstance().prepared(query, values, new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> message) {
					request.resume();
					Long count = SqlResult.countResult(message);
					handler.handle(count != null && count > 0);
				}
			});
		} else {
			handler.handle(false);
		}
	}

}
