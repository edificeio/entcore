package org.entcore.conversation.filters;

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

public class MessageOwnerFilter implements ResourcesProvider {

	private Sql sql;

	public MessageOwnerFilter(){
		this.sql = Sql.getInstance();
	}

	public void authorize(final HttpServerRequest request, Binding binding, UserInfos user, final Handler<Boolean> handler) {

		String messageId = request.params().get("id");

		if(messageId == null || messageId.trim().isEmpty()){
			handler.handle(false);
			return;
		}

		String query =
			"SELECT count(distinct m) AS number FROM conversation.messages m " +
			"JOIN conversation.usermessages um ON m.id = um.message_id " +
			"WHERE um.user_id = ? AND um.message_id = ? AND m.from = ?";

		JsonArray values = new JsonArray()
			.addString(user.getUserId())
			.addString(messageId)
			.addString(user.getUserId());

		request.pause();

		sql.prepared(query, values, SqlResult.validUniqueResultHandler(new Handler<Either<String,JsonObject>>() {
			public void handle(Either<String, JsonObject> event) {

				request.resume();

				if(event.isLeft()){
					handler.handle(false);
					return;
				}

				int count = event.right().getValue().getInteger("number", 0);
				handler.handle(count == 1);
			}
		}));
	}

}
