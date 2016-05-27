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

public class FoldersFilter implements ResourcesProvider {

	private Sql sql;

	public FoldersFilter(){
		this.sql = Sql.getInstance();
	}

	@Override
	public void authorize(final HttpServerRequest request, Binding binding,
			final UserInfos user, final Handler<Boolean> handler) {

		final List<String> messageIds = request.params().getAll("id");
		final String folderId = request.params().get("folderId");

		if(folderId == null | folderId.trim().isEmpty()){
			handler.handle(false);
			return;
		}

		String foldersQuery =
			"SELECT count(*) as number FROM conversation.folders " +
			"WHERE user_id = ? AND id = ?";
		JsonArray values = new JsonArray()
			.add(user.getUserId())
			.add(folderId);

		request.pause();

		sql.prepared(foldersQuery, values, SqlResult.validUniqueResultHandler(new Handler<Either<String,JsonObject>>() {
			public void handle(Either<String, JsonObject> event) {

				request.resume();

				if(event.isLeft()){
					handler.handle(false);
					return;
				}

				int folderCount = event.right().getValue().getInteger("number", 0);
				if(messageIds == null || messageIds.isEmpty()){
					handler.handle(folderCount == 1);
					return;
				}

				if(folderCount != 1){
					handler.handle(false);
					return;
				}

				String usersQuery =
					"SELECT count(distinct um) AS number FROM conversation.usermessages um " +
					"WHERE um.user_id = ? AND um.message_id IN " + Sql.listPrepared(messageIds.toArray());
				JsonArray values = new JsonArray()
					.addString(user.getUserId());
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
		}));


	}

}
