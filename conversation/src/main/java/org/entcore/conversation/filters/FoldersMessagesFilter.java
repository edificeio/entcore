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

import fr.wseduc.webutils.http.Binding;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.sqlclient.Tuple;
import org.entcore.common.user.UserInfos;
import org.entcore.conversation.service.impl.ReactiveSql;

import java.util.List;

public class FoldersMessagesFilter extends FoldersFilter {

	@Override
	public void authorize(final HttpServerRequest request, Binding binding,
			final UserInfos user, final Handler<Boolean> handler) {

		super.authorize(request, binding, user, event -> {
      if(event){
        RequestUtils.bodyToJson(request, body -> {
final List<String> messageIds = body.getJsonArray("id", new fr.wseduc.webutils.collections.JsonArray()).getList();
String usersQuery =
"SELECT count(distinct um) AS number FROM conversation.usermessages um " +
"WHERE um.user_id = $1 AND um.message_id IN " + ReactiveSql.listPrepared(messageIds, 2);
final Tuple values = Tuple.tuple().addString(user.getUserId());
for(String id : messageIds){
values.addString(id);
}

request.pause();

sql.withReadOnlyTransaction(connection -> {
final Promise<Void> promise = Promise.promise();
sql.prepared(usersQuery, values, connection).onComplete(r -> ReactiveSql.validUniqueResult(r, event1 -> {
request.resume();

if (event1.isLeft()) {
handler.handle(false);
promise.complete();
return;
}

int count = event1.right().getValue().getInteger("number", 0);
handler.handle(count == messageIds.size());
promise.complete();
}));
return promise.future();
});
});


      }else{
        handler.handle(false);
        return;
      }
    });

	}

}
