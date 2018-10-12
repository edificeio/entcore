/*
 * Copyright © "Open Digital Education", 2016
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

package org.entcore.conversation.filters;

import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.http.Binding;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;

import java.util.List;

public class FoldersMessagesFilter extends FoldersFilter {

	@Override
	public void authorize(final HttpServerRequest request, Binding binding,
			final UserInfos user, final Handler<Boolean> handler) {

		super.authorize(request, binding, user, new Handler<Boolean>() {
			@Override
			public void handle(Boolean event) {
				if(event){
					RequestUtils.bodyToJson(request, new Handler<JsonObject>() {
						public void handle(final JsonObject body) {
							final List<String> messageIds = body.getJsonArray("id", new fr.wseduc.webutils.collections.JsonArray()).getList();
							String usersQuery =
									"SELECT count(distinct um) AS number FROM conversation.usermessages um " +
											"WHERE um.user_id = ? AND um.message_id IN " + Sql.listPrepared(messageIds.toArray());
							JsonArray values = new fr.wseduc.webutils.collections.JsonArray()
									.add(user.getUserId());
							for(String id : messageIds){
								values.add(id);
							}

							request.pause();

							sql.prepared(usersQuery, values, SqlResult.validUniqueResultHandler(new Handler<Either<String,JsonObject>>() {
								public void handle(Either<String, JsonObject> event) {

									request.resume();

									if(event.isLeft()){
										handler.handle(false);
										return;
									}

									int count = event.right().getValue().getInteger("number", 0);
									handler.handle(count == messageIds.size());
								}
							}));
						}
					});


				}else{
					handler.handle(false);
					return;
				}
			}
		});

	}

}
