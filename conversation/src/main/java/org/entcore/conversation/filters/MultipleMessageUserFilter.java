package org.entcore.conversation.filters;

import fr.wseduc.webutils.request.RequestUtils;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.http.Binding;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class MultipleMessageUserFilter implements ResourcesProvider  {

    private Sql sql;

    public MultipleMessageUserFilter(){
        this.sql = Sql.getInstance();
    }

    public void authorize(final HttpServerRequest request, Binding binding,
                          UserInfos user, final Handler<Boolean> handler) {
        RequestUtils.bodyToJson(request, new Handler<JsonObject>() {
            public void handle(final JsonObject body) {
                List<String> messageIds = body.getJsonArray("id", new fr.wseduc.webutils.collections.JsonArray()).getList();
                if(messageIds == null || messageIds.isEmpty()){
                    handler.handle(false);
                    return;
                }
                checkMessages(request, messageIds, user, handler);
            }
        });

        final List<String> messageIds = request.params().getAll("id");



    }


    protected void checkMessages(final HttpServerRequest request, final List<String> messageIds, UserInfos user,final Handler<Boolean> handler){
        String query =
                "SELECT count(distinct um) AS number FROM conversation.usermessages um " +
                        "WHERE um.user_id = ? AND um.message_id IN " + Sql.listPrepared(messageIds.toArray());

        JsonArray values = new fr.wseduc.webutils.collections.JsonArray().add(user.getUserId());
        messageIds.forEach(id -> values.add(id));

        request.pause();

        sql.prepared(query, values, SqlResult.validUniqueResultHandler(new Handler<Either<String,JsonObject>>() {
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
}
