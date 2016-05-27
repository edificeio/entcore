package org.entcore.conversation.filters;

import java.util.List;

import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.http.Binding;

public class MessageUserFilter implements ResourcesProvider {

	private Sql sql;

	public MessageUserFilter(){
		this.sql = Sql.getInstance();
	}

	public void authorize(final HttpServerRequest request, Binding binding,
		UserInfos user, final Handler<Boolean> handler) {

		final List<String> messageIds = request.params().getAll("id");
		if(messageIds == null || messageIds.isEmpty()){
			handler.handle(false);
			return;
		}

		String query =
			"SELECT count(distinct um) AS number FROM conversation.usermessages um " +
			"WHERE um.user_id = ? AND um.message_id IN " + Sql.listPrepared(messageIds.toArray());


		JsonArray values = new JsonArray()
			.addString(user.getUserId());
		for(String id : messageIds){
			values.add(id);
		}

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
