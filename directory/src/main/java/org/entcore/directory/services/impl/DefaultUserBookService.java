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

package org.entcore.directory.services.impl;

import static fr.wseduc.webutils.Utils.isNotEmpty;
import static org.entcore.common.neo4j.Neo4jResult.*;
import static org.entcore.common.neo4j.Neo4jUtils.nodeSetPropertiesFromJson;
import static org.entcore.common.user.SessionAttributes.PERSON_ATTRIBUTE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import io.vertx.core.eventbus.EventBus;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.bus.WorkspaceHelper;
import org.entcore.common.neo4j.Neo;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.StatementsBuilder;
import org.entcore.common.storage.Storage;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.entcore.common.utils.FileUtils;
import org.entcore.common.utils.StringUtils;
import org.entcore.directory.services.UserBookService;

import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.Utils;
import fr.wseduc.webutils.http.ETag;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class DefaultUserBookService implements UserBookService {
	private final EventBus eb;
	private final Storage avatarStorage;
	private final JsonObject userBookData;
	private final WorkspaceHelper wsHelper;
	private final Neo4j neo = Neo4j.getInstance();
	private static final Logger log = LoggerFactory.getLogger(DefaultUserBookService.class);

	public DefaultUserBookService(EventBus eb, Storage avatarStorage, WorkspaceHelper wsHelper, JsonObject userBookData) {
		super();
		this.eb = eb;
		this.wsHelper = wsHelper;
		this.avatarStorage = avatarStorage;
		this.userBookData = userBookData;
	}

	private Optional<String> getPictureIdForUserbook(JsonObject userBook) {
		String picturePath = userBook.getString("picture");
		if (StringUtils.isEmpty(picturePath)) {
			return Optional.empty();
		}
		//it is not picture id but user id
		if(picturePath.startsWith("/userbook/avatar/")) {
			return Optional.empty();
		}
		String[] picturePaths = picturePath.split("/");
		String pictureId = picturePaths[picturePaths.length - 1];
		return Optional.ofNullable(pictureId);
	}

	private String avatarFileNameFromUserId(String fileId, Optional<String> size) {
		// Filename ends with fileId to keep thumbs in the same folder
		return size.isPresent() ? String.format("%s-%s", size.get(), fileId) : fileId;
	}

	public void cleanAvatarCache(List<String> usersId, final Handler<Boolean> handler) {
		@SuppressWarnings("rawtypes")
		List<Future> futures = new ArrayList<>();
		for (String u : usersId) {
			Future<Boolean> future = Future.future();
			futures.add(future);
			futures.add(cleanAvatarCache(u));
		}
		CompositeFuture.all(futures).setHandler(finishRes -> handler.handle(finishRes.succeeded()));
	}

	private Future<Boolean> cleanAvatarCache(String userId) {
		Future<Boolean> future = Future.future();
		this.avatarStorage.findByFilenameEndingWith(userId, res -> {
			if (res.succeeded() && res.result().size() > 0) {
				this.avatarStorage.removeFiles(res.result(), removeRes -> {
					future.complete(true);
				});
			} else {
				future.complete(false);
			}
		});
		return future;
	}

	private Future<Boolean> cacheAvatarFromUserBook(String userId, Optional<String> pictureId, Boolean remove) {
		// clean avatar when changing or when removing
		Future<Boolean> futureClean = (pictureId.isPresent() || remove) ? cleanAvatarCache(userId)
				: Future.succeededFuture();
		return futureClean.compose(res -> {
			if (!pictureId.isPresent()) {
				return Future.succeededFuture();
			}
			Future<Boolean> futureCopy = Future.future();
			this.wsHelper.getDocument(pictureId.get(), resDoc -> {
				if (resDoc.succeeded() && "ok".equals(resDoc.result().body().getString("status"))) {
					JsonObject document = resDoc.result().body().getJsonObject("result");
					String fileId = document.getString("file");
					// Extensions are not used by storage
					String defaultFilename = avatarFileNameFromUserId(userId, Optional.empty());
					//
					JsonObject thumbnails = document.getJsonObject("thumbnails", new JsonObject());
					Map<String, String> filenamesByIds = new HashMap<>();
					filenamesByIds.put(fileId, defaultFilename);

					for (String size : thumbnails.fieldNames()) {
						filenamesByIds.put(thumbnails.getString(size),
								avatarFileNameFromUserId(userId, Optional.of(size)));
					}
					// TODO avoid buffer to improve performances and avoid cache every time
					List<Future> futures = new ArrayList<>();
					for (Entry<String, String> entry : filenamesByIds.entrySet()) {
						String cFileId = entry.getKey();
						String cFilename = entry.getValue();
						Future<JsonObject> future = Future.future();
						futures.add(future);
						this.wsHelper.readFile(cFileId, buffer -> {
							if (buffer != null) {
								this.avatarStorage.writeBuffer(FileUtils.stripExtension(cFilename), buffer, "",
										cFilename, wRes -> {
											future.complete(wRes);
										});
							} else {
								future.fail("Cannot read file from workspace storage. ID =: " + cFileId);
							}
						});
					}
					//
					CompositeFuture.all(futures).setHandler(finishRes -> futureCopy.complete(finishRes.succeeded()));
				}
			});
			return futureCopy;
		});

	}

	@Override
	public void update(String userId, JsonObject userBookData, final Handler<Either<String, JsonObject>> result) {
		final JsonObject userbook = Utils.validAndGet(userBookData, UPDATE_USERBOOK_FIELDS, Collections.<String>emptyList());
		if (Utils.defaultValidationError(userbook, result, userId))
			return;
		// OVERRIDE AVATAR URL
		final Optional<String> pictureId = getPictureIdForUserbook(userBookData);
		if (pictureId.isPresent()) {
			final String fileId = avatarFileNameFromUserId(userId, Optional.empty());
			userbook.put("picture", "/userbook/avatar/" + fileId);
		}
		final StatementsBuilder queries = new StatementsBuilder();
		//update userbook
		final boolean updateUserBook = userbook.size() > 0;
		if (updateUserBook) {
			final StringBuilder query = new StringBuilder("MATCH (u:`User` { id : {id}})-[:USERBOOK]->(ub:UserBook) ");
			query.append(" WITH ub.picture as oldpic,ub,u SET " + nodeSetPropertiesFromJson("ub", userbook));
			query.append(" RETURN oldpic,ub.picture as picture");
			queries.add(query.toString(), userbook.put("id", userId));
		}
		{//update hobbies
			final JsonArray hobbies = userBookData.getJsonArray("hobbies", new JsonArray());
			final List<String> updateClauses = new ArrayList<>();
			final JsonObject params = new JsonObject().put("id", userId);
			for (Object hobbyO : hobbies) {
				if (hobbyO instanceof JsonObject){
					final JsonObject hobbyJson = (JsonObject) hobbyO;
					final String visibility = hobbyJson.getString("visibility", PRIVE);
					final String category = hobbyJson.getString("category");
					final String values = hobbyJson.getString("values","");
					if(!StringUtils.isEmpty(category)){
						updateClauses.add(String.format("ub.hobby_%s = {%s}", category, category));
						params.put(category, new JsonArray().add(visibility).add(values));
					}
				}
			}
			final String query = "MATCH (u:User { id : {id}})-[:USERBOOK]->(ub:UserBook) SET "+StringUtils.join(updateClauses, ",");
			if(updateClauses.size() > 0){
				queries.add(query, params);
			}
		}
		neo.executeTransaction(queries.build(), null, true, r -> {
			if ("ok".equals(r.body().getString("status"))) {
				if (updateUserBook) {
					final JsonArray results = r.body().getJsonArray("results", new JsonArray());
					final JsonArray firstStatement = results.getJsonArray(0);
					if (firstStatement != null && firstStatement.size() > 0) {
						final JsonObject object = firstStatement.getJsonObject(0);
						final String picture = object.getString("picture", "");
						cacheAvatarFromUserBook(userId, pictureId, StringUtils.isEmpty(picture)).setHandler(e -> {
							if (e.succeeded()) {
								result.handle(new Either.Right<>(new JsonObject()));
							} else {
								result.handle(new Either.Left<>(r.body().getString("message", "update.error")));
							}
						});
					}
				} else {
					result.handle(new Either.Right<>(new JsonObject()));
				}
			} else {
				result.handle(new Either.Left<>(r.body().getString("message", "update.error")));
			}
		});
	}

	private Future<Boolean> sendAvatar(HttpServerRequest request, String fileId) {
		Future<Boolean> future = Future.future();
		// file storage doesnt keep extension
		JsonObject meta = new JsonObject().put("content-type", "image/*");
		this.avatarStorage.fileStats(fileId, stats -> {
			if (stats.succeeded()) {
				Date modified = stats.result().getLastModified();
				boolean hasBeenModified = HttpHeaderUtils.checkIfModifiedSince(request.headers(), modified);
				boolean hasChangedEtag = !ETag.check(request, fileId);
				HttpHeaderUtils.addHeaderLastModified(request.response().headers(), modified);
				// check if file is modified or fileid has changed
				if (hasBeenModified || hasChangedEtag) {
					// TODO send file renvoie tout le chemin de fichier dans l ETAG?
					this.avatarStorage.sendFile(fileId, fileId, request, true, meta);
					future.complete(true);
				} else {
					Renders.notModified(request);
					future.complete(true);
				}
			} else {
				future.complete(false);
			}
		});
		return future;
	}

	@Override
	public void getAvatar(String userId, Optional<String> size, String defaultAvatarDirty, HttpServerRequest request) {
		String fileIdSized = avatarFileNameFromUserId(userId, size);
		sendAvatar(request, fileIdSized)// try with size
				.compose(success -> {// try without size
					if (success) {
						return Future.succeededFuture(true);
					} else {
						if (size.isPresent()) {// try without size
							String fileIdUnsized = avatarFileNameFromUserId(userId, Optional.empty());
							return sendAvatar(request, fileIdUnsized);
						} else {// without size already tried
							return Future.succeededFuture(false);
						}
					}
				}).compose(success -> {// try default
					if (success) {
						return Future.succeededFuture(true);
					} else {
						String fidIdDefault = FileUtils.stripExtension(defaultAvatarDirty);
						return sendAvatar(request, fidIdDefault);
					}
				}).setHandler(res -> {
					if (res.failed() || !res.result()) {// could not found any img
						Renders.notFound(request);
					}
				});
	}

	@Override
	public void get(String userId, Handler<Either<String, JsonObject>> handler) {
		String query = "MATCH (u:`User` { id : {id}})-[:USERBOOK]->(ub: UserBook) RETURN ub ";
		neo.execute(query, new JsonObject().put("id", userId), fullNodeMergeHandler("ub", res -> {
			if(res.isRight()){
				final JsonObject result = res.right().getValue();
				result.put("hobbies", UserBookService.extractHobbies(userBookData, result, true));
				handler.handle(new Either.Right<>(result));
			}else{
				handler.handle(res);
			}
		}));
	}

	private void queryPerson(final String userId, final boolean publicOnly, final Handler<Either<String, JsonArray>> handler){
		final StringBuilder query = new StringBuilder();
		query.append("MATCH (user:User) ");
		query.append("WHERE user.id = {userId} ");
		query.append("OPTIONAL MATCH (user)-[:IN]->(:ProfileGroup)-[:DEPENDS]->(struct:Structure) ");
		query.append("OPTIONAL MATCH (user)-[:IN]->(:ProfileGroup)-[:DEPENDS]->(clazz:Class)-[:BELONGS]->(struct) ");
		query.append("OPTIONAL MATCH (user)-[:USERBOOK]->(ub) ");
		query.append("OPTIONAL MATCH (user)-[:RELATED]-(relative) ");
		query.append("WITH DISTINCT struct, user, ub, relative, COLLECT(DISTINCT clazz.name) as classes ");
		query.append("WITH user, ub, relative, COLLECT(DISTINCT {name: struct.name, id: struct.id, classes: classes}) as schools ");
		query.append("RETURN DISTINCT ");
		query.append("user.id as id, ");
		query.append("user.login as login, ");
		query.append("user.displayName as displayName, ");
		query.append("[HEAD(user.profiles)] as type, ");
		query.append("COALESCE(ub.visibleInfos, []) as visibleInfos, ");
		query.append("schools, ");
		query.append("relative.displayName as relatedName, ");
		query.append("relative.id as relatedId, ");
		query.append("relative.type as relatedType, ");
		query.append("ub.userid as userId, ");
		query.append("ub.motto as motto, ");
		query.append("COALESCE(ub.picture, 'no-avatar.jpg') as photo, ");
		query.append("COALESCE(ub.mood, 'default') as mood, ");
		query.append("ub.health as health, ");
		query.append("user.address as address, ");
		query.append("user.email as email, ");
		query.append("user.homePhone as tel, ");
		query.append("user.mobile as mobile, ");
		query.append("user.birthDate as birthdate, ");
		query.append(UserBookService.selectHobbies(userBookData,"ub"));
		//params
		final Map<String, Object> params = new HashMap<>();
		params.put("userId", userId);
		params.put("defaultAvatar", userBookData.getString("default-avatar"));
		params.put("defaultMood", userBookData.getString("default-mood"));
		neo.execute(query.toString(), params, validResultHandler(res->{
			if(res.isRight()){
				final JsonArray results = res.right().getValue();
				for(Object resultO : results){
					final JsonObject result = (JsonObject)resultO;
					//hobbies
					final JsonArray newHobbies = UserBookService.extractHobbies(userBookData, result, true);
					result.put("hobbies", newHobbies);
					if(publicOnly){
						//filter hobbies
						final JsonArray filteredHobbies = new JsonArray();
						for(int i = 0 ; i < newHobbies.size(); i++){
							final JsonObject hobby = newHobbies.getJsonObject(i);
							if(PUBLIC.equals(hobby.getString("visibility"))){
								filteredHobbies.add(hobby);
							}
						}
						result.put("hobbies", filteredHobbies);
						//filter confidential data
						final JsonArray visibleInfos = result.getJsonArray("visibleInfos", new JsonArray());
						//TODO SHOW_MAIL should be renamed to SHOW_ADDRESS?
						if(!visibleInfos.contains("SHOW_MAIL")) result.put("address", "");
						if(!visibleInfos.contains("SHOW_EMAIL")) result.put("email", "");
						if(!visibleInfos.contains("SHOW_PHONE")) result.put("tel", "");
						if(!visibleInfos.contains("SHOW_MOBILE")) result.put("mobile", "");
						if(!visibleInfos.contains("SHOW_BIRTHDATE")) result.put("birthdate", "");
						if(!visibleInfos.contains("SHOW_HEALTH")) result.put("health", "");
					}
				}
				handler.handle(new Either.Right<>(results));
			} else {
				handler.handle(res.left());
			}
		}));
	}
	private Either<String, JsonObject> adaptPersonResult(Either<String, JsonArray> res){
		if(res.isRight()){
			final JsonObject transformed = new JsonObject().put("status", "ok").put("result", (res.right().getValue()));
			return (new Either.Right<>(transformed));
		}else{
			return (new Either.Left<>(res.left().getValue()));
		}
	}
	public void getCurrentUserInfos(UserInfos user, Handler<Either<String, JsonObject>> result) {
		Object person = user.getAttribute(PERSON_ATTRIBUTE);
		if (person != null) {
			result.handle(new Either.Right<>(new JsonObject(person.toString())));
			return;
		}
		queryPerson(user.getUserId(), false, res->{
			final Either<String, JsonObject> adapted = adaptPersonResult(res.right());
			if(adapted.isRight()){
				UserUtils.addSessionAttribute(eb, user.getUserId(), PERSON_ATTRIBUTE, adapted.right().getValue().encode(), null);
				result.handle(adapted);
			} else {
				result.handle(adapted);
			}
		});
	}

	public void getPersonInfos(String personId, Handler<Either<String, JsonObject>> handler) {
		queryPerson(personId, true, result->{
			handler.handle(adaptPersonResult(result));
		});
	}

	@Override
	public void initUserbook(final String userId, final String theme, final JsonObject uacLanguage) {
		final StatementsBuilder queries = new StatementsBuilder();
		{//create userbook
			final JsonObject params = new JsonObject();
			params.put("userId", userId);
			params.put("avatar", userBookData.getString("default-avatar"));
			params.put("theme", userBookData.getString("default-theme", ""));
			final StringBuilder query = new StringBuilder();
			query.append("MERGE (m:UserBook { userid : {userId}}) ");
			query.append("SET m.type = 'USERBOOK', m.picture = {avatar}, m.motto = '', m.health = '', m.mood = 'default', m.theme =  {theme} ");
			query.append("WITH m MATCH (n:User {id : {userId}}) CREATE UNIQUE n-[:USERBOOK]->m");
			queries.add(query.toString(), params);
		}
		final JsonArray listOfHobbies = userBookData.getJsonArray("hobbies", new JsonArray());
		if(listOfHobbies.size() > 0){//create hobbies
			final List<String> updateClauses = new ArrayList<>();
			final JsonObject params = new JsonObject().put("userId", userId);
			for (int i = 0 ; i < listOfHobbies.size(); i ++) {
				final String hobby =listOfHobbies.getString(i);
				updateClauses.add(String.format("m.hobby_%s = {%s} ", hobby, hobby));
				params.put(hobby, new JsonArray().add(PRIVE).add(""));
			}
			final String query2 = "MATCH (n:User)-[:USERBOOK]->m WHERE n.id = {userId} SET "+StringUtils.join(updateClauses, ",");
			queries.add(query2, params);
		}
		{//create appconfig
			String query3 = "MATCH (u:User {id:{userId}}) MERGE (u)-[:PREFERS]->(uac:UserAppConf) SET uac.language = {language}";
			final JsonObject params = new JsonObject().put("userId", userId).put("language", uacLanguage.encode());
			if (isNotEmpty(theme)) {
				query3 += ", uac.theme = {theme}";
				params.put("theme", theme);
			}
			queries.add(query3, params);
		}
		neo.executeTransaction(queries.build(),null, true, validResultsHandler(res->{
			if(res.isLeft()){
				log.error("[DefaultUserBookService.initUserbook] failed to init userbook: "+ res.left().getValue());
			}
		}));
	}

	@Override
	public void setHobbyVisibility(final UserInfos user, final String category, final String visibilityValue, final Handler<Either<String, JsonObject>> handler) {
		final String visibility = PUBLIC.equals(visibilityValue) ? PUBLIC : PRIVE;
		final String reverseVisibility = PUBLIC.equals(visibilityValue) ? PRIVE : PUBLIC;
		final Map<String, Object> params = new HashMap<>();
		params.put("id", user.getUserId());
		params.put("visibility", visibility);
		final StringBuilder query = new StringBuilder();
		query.append(String.format("MATCH (u:User)-[:USERBOOK]->(ub) WHERE u.id = {id} AND EXISTS(ub.hobby_%s) ", category));
		query.append(String.format("SET ub.hobby_%s =  [{visibility}, ub.hobby_%s[1]] ", category, category));
		query.append(String.format("RETURN ub.hobby_%s ", category));
		UserUtils.removeSessionAttribute(eb, user.getUserId(), PERSON_ATTRIBUTE, null);
		neo.execute(query.toString(), params, validUniqueResultHandler(handler));
	}

	@Override
	public void setInfosVisibility(final UserInfos user, final String state, final String info, final Handler<Either<String, JsonObject>> handler) {
		final String relationship = "SHOW_" + info.toUpperCase();
		final Map<String, Object> params = new HashMap<>();
		params.put("id", user.getUserId());
		params.put("relation", relationship);
		final StringBuilder query = new StringBuilder();
		query.append("MATCH (u:User)-[:USERBOOK]->(ub) WHERE u.id={id} ");
		query.append("WITH ub, FILTER(x IN COALESCE(ub.visibleInfos, []) WHERE x <> {relation}) as newVisibilities ");
		if ("public".equals(state)) {
			query.append("WITH ub, (newVisibilities + {relation}) as newVisibilities2 ");
			query.append("SET ub.visibleInfos = newVisibilities2 ");
		} else {
			query.append("SET ub.visibleInfos = newVisibilities ");
		}
		query.append("RETURN ub.visibleInfos as visibleInfos; ");
		UserUtils.removeSessionAttribute(eb, user.getUserId(), PERSON_ATTRIBUTE, null);
		neo.execute(query.toString(), params, validUniqueResultHandler(handler));

	}
}
