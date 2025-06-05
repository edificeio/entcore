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


import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.entcore.common.user.UserInfos;

import fr.wseduc.transformer.to.ContentTransformerResponse;
import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public interface ConversationService {
	/** Maximum number of results a paginated query can return at once. */
	static final int LIST_LIMIT = 25;
	/** Maximum number of folders a user can create, at root or in another folder. */
	public static final int MAX_FOLDER_NUMBER = 50;
	/** This is the maximum depth for a subfolder (1 being a root folder). */
	public static final int MAX_FOLDER_DEPTH = 3;
	/**
	 * Maximum number of levels of folders that can be listed in one request,
	 * limiting the number of returned folders as a power of MAX_FOLDER_NUMBER.
	 * 
	 * For example, listing folders with a depth of 2 => returning MAX_FOLDER_NUMBER^2 =2500 folders at once in the worst case.
	 */
	public static final int MAX_FOLDERS_LEVEL = 3;

	enum State { DRAFT, SENT, RECALL }

	static final String[] SYSTEM_FOLDER_NAMES = {"INBOX", "OUTBOX", "DRAFT", "TRASH"};
	static public boolean isSystemFolder(final String folder) {
		return folder!=null && Stream.of(SYSTEM_FOLDER_NAMES).anyMatch(sysFolder -> folder.equalsIgnoreCase(sysFolder));
	}

	List<String> MESSAGE_FIELDS = Arrays.asList("id", "subject", "body", "from", "to", "cc", "date", "state",
			"displayNames");

	List<String> DRAFT_REQUIRED_FIELDS = Arrays.asList("id", "from", "date", "state");

	List<String> UPDATE_DRAFT_FIELDS = Arrays.asList("subject", "body", "to", "cc", "date", "displayNames");

	List<String> UPDATE_DRAFT_REQUIRED_FIELDS = Arrays.asList("date");

	/**
	 * Enregistre un nouveau brouillon de message dans la base de données.
	 *
	 * @param parentMessageId L'identifiant du message parent, ou {@code null} si ce message n'est pas une réponse.
	 * @param threadId        L'identifiant du fil de discussion, ou {@code null} pour en créer un nouveau.
	 * @param message         Le contenu du message à enregistrer (sous forme de {@link JsonObject}).
	 * @param user            Les informations de l'utilisateur courant.
	 * @param result          Le handler appelé en retour, contenant soit le message sauvegardé, soit une erreur.
	 * @param request         La requête HTTP associée (pour la gestion du contexte ou des permissions).
	 */
	void saveDraft(String parentMessageId, String threadId, JsonObject message, UserInfos user,
			Handler<Either<String, JsonObject>> result, HttpServerRequest request);

	/**
	 * Same as saveDraft, but to be used right before sending the draft.
	 */
	void saveDraftAsMessage(String parentMessageId, String threadId, JsonObject message, UserInfos user,
			Handler<Either<String, JsonObject>> result, HttpServerRequest request);

	/**
	 * Met à jour un brouillon de message existant.
	 *
	 * @param messageId L'identifiant du message à mettre à jour.
	 * @param message   Le contenu mis à jour du message (sous forme de {@link JsonObject}).
	 * @param user      Les informations de l'utilisateur courant.
	 * @param result    Le handler appelé en retour, contenant soit le message mis à jour, soit une erreur.
	 * @param request   La requête HTTP associée (pour la gestion du contexte ou des permissions).
	 */
	void updateDraft(String messageId, JsonObject message, UserInfos user,
			Handler<Either<String, JsonObject>> result, HttpServerRequest request);
	/**
	 * Same as updateDraft, but to be used right before sending the draft.
	 */
	void updateDraftAsMessage(String messageId, JsonObject message, UserInfos user,
			Handler<Either<String, JsonObject>> result, HttpServerRequest request);

	/**
	 * Reply if parentMessageId isn't null.
	 */
	void send(String parentMessageId, String draftId, JsonObject message, UserInfos user,
		Handler<Either<String, JsonObject>> result);

	/** 
	 * Recall a message by id iif 
	 * its state=SENT, 
	 * and its sender is the current user, 
	 * and it is not older than `recallDelayInMinutes`
	 */
	Future<Void> recallMessage(String id, UserInfos user);

	/**
	 * List messages from any folder 
	 * @param folder Any UserFolder ID, or any case-insensitive SystemFolder name.
	 * @param unread Truthy when only unread messages must be returned.
	 * @param user connected user
	 * @param page page number
	 * @param page_size number of messages per page
	 * @param searchWords optional text filter
	 * @param states List messages having this state. Not applied when `folder` is either DRAFT or TRASH.
	 */
	void list(String folder, Boolean unread, UserInfos user, int page, int page_size, String searchWords, EnumSet<State> states, Handler<Either<String, JsonArray>> results);
	/** Legacy */
	void list(String folder, String restrain, Boolean unread, UserInfos user, int page, String searchWords, Handler<Either<String, JsonArray>> results);

	Future<JsonArray> listAndFormat(String folderId, Boolean unread, UserInfos userInfos, int page, int page_size, String search, String lang);

	void listThreads(UserInfos user, int page, Handler<Either<String, JsonArray>> results);

	void listThreadMessages(String threadId, int page, UserInfos user, Handler<Either<String, JsonArray>> results);

	void listThreadMessagesNavigation(String messageId, boolean previous, UserInfos user, Handler<Either<String, JsonArray>> results);

	void trash(List<String> messagesId, UserInfos user, Handler<Either<String, JsonObject>> result);

	void trashThread(List<String> threadIds, UserInfos user, Handler<Either<String, JsonObject>> result);

	void restore(List<String> messagesId, UserInfos user, Handler<Either<String, JsonObject>> result);

	void delete(List<String> messagesId, Boolean deleteAll, UserInfos user, Handler<Either<String, JsonArray>> result);

	void get(String messageId, UserInfos user, Handler<Either<String, JsonObject>> result);

	void get(String messageId, UserInfos user, int apiVersion, Handler<Either<String, JsonObject>> result);

	Future<JsonObject> getAndFormat(String id, UserInfos userInfos, String lang, boolean originalFormat, HttpServerRequest request);

	Future<String> getOriginalMessageContent(String messageId);

	Future<ContentTransformerResponse> transformMessageContent(String originalMessageContent, String messageId, boolean isDraft, HttpServerRequest request);

	Future<Void> updateMessageContent(String messageId, String body, int contentVersion);

	void count(String folder, String restrain, Boolean unread, UserInfos user, Handler<Either<String, JsonObject>> result);

	void findVisibleRecipients(String parentMessageId, UserInfos user,
		String acceptLanguage, String search, Handler<Either<String, JsonObject>> result);

	void toggleUnread(List<String> messagesId, boolean unread, UserInfos user, Handler<Either<String, JsonObject>> result);

	void toggleUnreadThread(List<String> threadIds, boolean unread, UserInfos user, Handler<Either<String, JsonObject>> result);

	//Folders
	void getFolderTree(UserInfos user, int depth, Optional<String> parentId, Handler<Either<String, JsonArray>> result);
	void createFolder(String folderName, String parentFolderId, UserInfos user, Handler<Either<String, JsonObject>> result);
	void updateFolder(String folderId, JsonObject data, UserInfos user, Handler<Either<String, JsonObject>> result);
	void listFolders(String parentId, UserInfos user, Handler<Either<String, JsonArray>> result);
	void listUserFolders(Optional<String> parentId, UserInfos user, Boolean unread, Handler<Either<String, JsonArray>> result);
	void listTrashedFolders(UserInfos user, Handler<Either<String, JsonArray>> result);
	void moveToFolder(List<String> messageIds, String folderId, UserInfos user, Handler<Either<String, JsonObject>> result);
	void backToSystemFolder(List<String> messageIds, UserInfos user,Handler<Either<String, JsonObject>> result);
	void trashFolder(String folderId, UserInfos user, Handler<Either<String, JsonObject>> result);
	void restoreFolder(String folderId, UserInfos user, Handler<Either<String, JsonObject>> result);
	void deleteFolder(String folderId, Boolean deleteAll, UserInfos user, Handler<Either<String, JsonArray>> result);

	/**
	 * Deletes the specified folders (and subfolders, recursively) for a user,
	 * and put any message they contain into the trash folder.
	 *
	 * @param folderIds A list of folder IDs to be deleted.
	 * @param user The user information.
	 * @return A Future JsonObject with the following props :
	 *   `trashedMessageIds`: array of message IDs put into the trash folder.
	 */
	Future<JsonObject> deleteFoldersAndTrashMessages(List<String> folderIds, UserInfos user);

	//Attachments
	void addAttachment(String messageId, UserInfos user, JsonObject uploaded, Handler<Either<String, JsonObject>> result);
	void getAttachment(String messageId, String attachmentId, UserInfos user, Handler<Either<String, JsonObject>> result);
	void getAllAttachments(String messageId, UserInfos user, Handler<Either<String, JsonArray>> result);
	void removeAttachment(String messageId, String attachmentId, UserInfos user, final Handler<Either<String, JsonObject>> result);
	void forwardAttachments(String forwardId, String messageId, UserInfos user, Handler<Either<String, JsonObject>> result);
}
