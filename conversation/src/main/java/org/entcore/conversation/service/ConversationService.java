package org.entcore.conversation.service;


import org.entcore.common.user.UserInfos;
import fr.wseduc.webutils.Either;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.Arrays;
import java.util.List;

public interface ConversationService {

	enum State { DRAFT, SENT }

	List<String> MESSAGE_FIELDS = Arrays.asList("id", "subject", "body", "from", "to", "cc", "date", "state");

	List<String> DRAFT_REQUIRED_FIELDS = Arrays.asList("id", "from", "date", "state");

	List<String> UPDATE_DRAFT_FIELDS = Arrays.asList("subject", "body", "to", "cc", "date");

	List<String> UPDATE_DRAFT_REQUIRED_FIELDS = Arrays.asList("date");

	void saveDraft(String parentMessageId, JsonObject message, UserInfos user,
			Handler<Either<String, JsonObject>> result);

	void updateDraft(String messageId, JsonObject message, UserInfos user,
			Handler<Either<String, JsonObject>> result);

	/**
	 * Reply if parentMessageId isn't null.
	 */
	void send(String parentMessageId, String draftId, JsonObject message, UserInfos user,
		Handler<Either<String, JsonObject>> result);

	void list(String folder, UserInfos user, int page, Handler<Either<String, JsonArray>> results);

	void trash(List<String> messagesId, UserInfos user, Handler<Either<String, JsonObject>> result);

	void restore(List<String> messagesId, UserInfos user, Handler<Either<String, JsonObject>> result);

	void delete(List<String> messagesId, UserInfos user, Handler<Either<String, JsonObject>> result);

	void get(String messageId, UserInfos user, Handler<Either<String, JsonObject>> result);

	void count(String folder, Boolean unread, UserInfos user, Handler<Either<String, JsonObject>> result);

	void findVisibleRecipients(String parentMessageId, UserInfos user,
		String acceptLanguage, Handler<Either<String, JsonObject>> result);

}
