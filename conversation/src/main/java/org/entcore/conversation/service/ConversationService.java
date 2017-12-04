/* Copyright © WebServices pour l'Éducation, 2014
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
 *
 */

package org.entcore.conversation.service;


import org.entcore.common.user.UserInfos;

import fr.wseduc.webutils.Either;

import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.Arrays;
import java.util.List;

public interface ConversationService {

	static final int LIST_LIMIT = 25;

	enum State { DRAFT, SENT }

	List<String> MESSAGE_FIELDS = Arrays.asList("id", "subject", "body", "from", "to", "cc", "date", "state",
			"displayNames");

	List<String> DRAFT_REQUIRED_FIELDS = Arrays.asList("id", "from", "date", "state");

	List<String> UPDATE_DRAFT_FIELDS = Arrays.asList("subject", "body", "to", "cc", "date", "displayNames");

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

	void list(String folder, String restrain, UserInfos user, int page, Handler<Either<String, JsonArray>> results);

	void trash(List<String> messagesId, UserInfos user, Handler<Either<String, JsonObject>> result);

	void restore(List<String> messagesId, UserInfos user, Handler<Either<String, JsonObject>> result);

	void delete(List<String> messagesId, UserInfos user, Handler<Either<String, JsonArray>> result);

	void get(String messageId, UserInfos user, Handler<Either<String, JsonObject>> result);

	void count(String folder, Boolean unread, UserInfos user, Handler<Either<String, JsonObject>> result);

	void findVisibleRecipients(String parentMessageId, UserInfos user,
		String acceptLanguage, String search, Handler<Either<String, JsonObject>> result);

	void toggleUnread(List<String> messagesId, boolean unread, UserInfos user, Handler<Either<String, JsonObject>> result);

	//Folders
	void createFolder(String folderName, String parentFolderId, UserInfos user, Handler<Either<String, JsonObject>> result);
	void updateFolder(String folderId, JsonObject data, UserInfos user, Handler<Either<String, JsonObject>> result);
	void listFolders(String parentId, UserInfos user, Handler<Either<String, JsonArray>> result);
	void listTrashedFolders(UserInfos user, Handler<Either<String, JsonArray>> result);
	void moveToFolder(List<String> messageIds, String folderId, UserInfos user, Handler<Either<String, JsonObject>> result);
	void backToSystemFolder(List<String> messageIds, UserInfos user,Handler<Either<String, JsonObject>> result);
	void trashFolder(String folderId, UserInfos user, Handler<Either<String, JsonObject>> result);
	void restoreFolder(String folderId, UserInfos user, Handler<Either<String, JsonObject>> result);
	void deleteFolder(String folderId, UserInfos user, Handler<Either<String, JsonArray>> result);

	//Attachments
	void addAttachment(String messageId, UserInfos user, JsonObject uploaded, Handler<Either<String, JsonObject>> result);
	void getAttachment(String messageId, String attachmentId, UserInfos user, Handler<Either<String, JsonObject>> result);
	void getAllAttachments(String messageId, UserInfos user, Handler<Either<String, JsonArray>> result);
	void removeAttachment(String messageId, String attachmentId, UserInfos user, final Handler<Either<String, JsonObject>> result);
	void forwardAttachments(String forwardId, String messageId, UserInfos user, Handler<Either<String, JsonObject>> result);
}
