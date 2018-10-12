/* Copyright © "Open Digital Education", 2014
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

 *
 */

package org.entcore.conversation.service;


import org.entcore.common.user.UserInfos;

import fr.wseduc.webutils.Either;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

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

	void saveDraft(String parentMessageId, String threadId, JsonObject message, UserInfos user,
			Handler<Either<String, JsonObject>> result);

	void updateDraft(String messageId, JsonObject message, UserInfos user,
			Handler<Either<String, JsonObject>> result);

	/**
	 * Reply if parentMessageId isn't null.
	 */
	void send(String parentMessageId, String draftId, JsonObject message, UserInfos user,
		Handler<Either<String, JsonObject>> result);

	void list(String folder, String restrain, Boolean unread, UserInfos user, int page, String searchWords, Handler<Either<String, JsonArray>> results);

	void listThreads(UserInfos user, int page, Handler<Either<String, JsonArray>> results);

	void listThreadMessages(String threadId, int page, UserInfos user, Handler<Either<String, JsonArray>> results);

	void listThreadMessagesNavigation(String messageId, boolean previous, UserInfos user, Handler<Either<String, JsonArray>> results);

	void trash(List<String> messagesId, UserInfos user, Handler<Either<String, JsonObject>> result);

	void restore(List<String> messagesId, UserInfos user, Handler<Either<String, JsonObject>> result);

	void delete(List<String> messagesId, Boolean deleteAll, UserInfos user, Handler<Either<String, JsonArray>> result);

	void get(String messageId, UserInfos user, Handler<Either<String, JsonObject>> result);

	void count(String folder, String restrain, Boolean unread, UserInfos user, Handler<Either<String, JsonObject>> result);

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
	void deleteFolder(String folderId, Boolean deleteAll, UserInfos user, Handler<Either<String, JsonArray>> result);

	//Attachments
	void addAttachment(String messageId, UserInfos user, JsonObject uploaded, Handler<Either<String, JsonObject>> result);
	void getAttachment(String messageId, String attachmentId, UserInfos user, Handler<Either<String, JsonObject>> result);
	void getAllAttachments(String messageId, UserInfos user, Handler<Either<String, JsonArray>> result);
	void removeAttachment(String messageId, String attachmentId, UserInfos user, final Handler<Either<String, JsonObject>> result);
	void forwardAttachments(String forwardId, String messageId, UserInfos user, Handler<Either<String, JsonObject>> result);
}
