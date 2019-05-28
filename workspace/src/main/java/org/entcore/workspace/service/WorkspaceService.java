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

package org.entcore.workspace.service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.entcore.common.folders.FolderManager;
import org.entcore.common.user.UserInfos;

import fr.wseduc.webutils.Either;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Created by sinthu on 24/08/18.
 */
public interface WorkspaceService extends FolderManager {

	public static final String WORKSPACE_NAME = "WORKSPACE";

	public void addDocument(final UserInfos user, final float quality, final String name, final String application,
			final List<String> thumbnail, final JsonObject doc, final JsonObject uploaded,
			final Handler<AsyncResult<JsonObject>> handler);

	public void updateDocument(final String id, final float quality, final String name, final List<String> thumbnail,
			final JsonObject uploaded, UserInfos user, final Handler<Message<JsonObject>> handler);

	public void addAfterUpload(final JsonObject uploaded, final JsonObject doc, String name, String application,
			final List<String> thumbs, final String ownerId, final String ownerName,
			final Handler<AsyncResult<JsonObject>> handler);

	public void updateAfterUpload(final String id, final String name, final JsonObject uploaded, final List<String> t,
			final UserInfos user, final Handler<Message<JsonObject>> handler);

	public void documentProperties(final String id, final Handler<JsonObject> handler);

	public void addComment(final String id, final String comment, final UserInfos user,
			final Handler<JsonObject> handler);

	public void deleteComment(final String id, final String comment, final Handler<JsonObject> handler);

	public void listRevisions(final String id, final Handler<Either<String, JsonArray>> handler);

	public void getRevision(final String documentId, final String revisionId,
			final Handler<Either<String, JsonObject>> handler);

	public Future<JsonObject> getRevision(final String revisionId);

	public void deleteRevision(final String documentId, final String revisionId, final List<String> thumbs,
			final Handler<Either<String, JsonObject>> handler);

	public void emptySize(final String userId, final Handler<Long> emptySizeHandler);

	public void emptySize(final UserInfos userInfos, final Handler<Long> emptySizeHandler);

	public void incrementStorage(JsonObject added);

	public void decrementStorage(JsonObject removed);

	public void decrementStorage(JsonObject removed, Handler<Either<String, JsonObject>> handler);

	public void incrementStorage(JsonArray added);

	public void decrementStorage(JsonArray removed);

	public void updateStorage(JsonObject added, JsonObject removed);

	public void updateStorage(JsonArray addeds, JsonArray removeds);

	public void updateStorage(JsonArray addeds, JsonArray removeds, final Handler<Either<String, JsonObject>> handler);

	public void findById(String id, final Handler<JsonObject> handler);

	public void findById(String id, String onwer, final Handler<JsonObject> handler);

	public void findById(String id, JsonObject keys, final Handler<JsonObject> handler);

	public void findById(String id, String onwer, boolean publicOnly, final Handler<JsonObject> handler);

	public void getQuotaAndUsage(String userId, Handler<Either<String, JsonObject>> handler);

	public void getShareInfos(final String userId, String resourceId, final String acceptLanguage, final String search,
			final Handler<Either<String, JsonObject>> handler);

	public Future<Set<String>> getNotifyContributorDest(Optional<String> id, UserInfos user, Set<String> docIds);

	public void changeVisibility(final JsonArray documentIds, String visibility, final Handler<Message<JsonObject>> handler);

}
