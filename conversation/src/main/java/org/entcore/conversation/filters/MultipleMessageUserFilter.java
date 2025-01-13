/*
 * Copyright © "Open Digital Education", 2018
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

import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.Vertx;
import io.vertx.sqlclient.Tuple;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.user.UserInfos;
import fr.wseduc.webutils.http.Binding;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import org.entcore.conversation.service.impl.ReactivePGClient;
import org.entcore.conversation.service.impl.ReactiveSql;

import java.util.List;

public class MultipleMessageUserFilter implements ResourcesProvider  {

    private final ReactivePGClient sql;

    public MultipleMessageUserFilter(){
        final Vertx vertx = Vertx.currentContext().owner();
        final JsonObject config = vertx.getOrCreateContext().config();
        this.sql = new ReactivePGClient(vertx, config);
    }

    public void authorize(final HttpServerRequest request, Binding binding,
                          UserInfos user, final Handler<Boolean> handler) {
        RequestUtils.bodyToJson(request, body -> {
            List<String> messageIds = body.getJsonArray("id", new fr.wseduc.webutils.collections.JsonArray()).getList();
            if(messageIds == null || messageIds.isEmpty()){
                handler.handle(false);
                return;
            }
            checkMessages(request, messageIds, user, handler);
        });
    }


    protected void checkMessages(final HttpServerRequest request, final List<String> messageIds, UserInfos user,final Handler<Boolean> handler){
        String query =
                "SELECT count(distinct um) AS number FROM conversation.usermessages um " +
                        "WHERE um.user_id = $1 AND um.message_id IN " + ReactiveSql.listPrepared(messageIds, 2);

        final Tuple values = Tuple.tuple().addString(user.getUserId());
        messageIds.forEach(values::addString);

        request.pause();

        sql.withReadOnlyTransaction(connection ->
            sql.prepared(query, values, connection)
            .onComplete(r -> ReactiveSql.validUniqueResult(r, event -> {
                    request.resume();
                    if(event.isLeft()){
                        handler.handle(false);
                        return;
                    }
                    int count = event.right().getValue().getInteger("number", 0);
                    handler.handle(count == messageIds.size());
                })
            )
        );
    }
}
