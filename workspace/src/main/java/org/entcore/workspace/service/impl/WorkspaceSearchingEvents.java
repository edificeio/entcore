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

package org.entcore.workspace.service.impl;

import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.entcore.common.folders.ElementQuery;
import org.entcore.common.folders.ElementQuery.ElementSort;
import org.entcore.common.folders.FolderManager;
import org.entcore.common.folders.impl.DocumentHelper;
import org.entcore.common.search.SearchingEvents;
import org.entcore.common.user.UserInfos;
import org.entcore.common.utils.DateUtils;
import org.entcore.common.utils.StringUtils;

import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Created by dbreyton on 03/06/2016.
 */
public class WorkspaceSearchingEvents implements SearchingEvents {
	private static final Logger log = LoggerFactory.getLogger(WorkspaceSearchingEvents.class);
	private final FolderManager folderManager;
	private static final I18n i18n = I18n.getInstance();
	private static final String PATTERN = "yyyy-MM-dd HH:mm.ss.sss";

	public WorkspaceSearchingEvents(FolderManager folderManager) {
		this.folderManager = folderManager;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void searchResource(List<String> appFilters, final String userId, JsonArray groupIds, JsonArray searchWords,
			Integer page, Integer limit, final JsonArray columnsHeader, final String locale,
			final Handler<Either<String, JsonArray>> handler) {
		if (appFilters.contains(WorkspaceSearchingEvents.class.getSimpleName())) {
			//
			// fetch files matching words
			//
			ElementQuery queryFiles = new ElementQuery(true);
			UserInfos user = new UserInfos();
			user.setUserId(userId);
			user.setGroupsIds(groupIds.getList());
			queryFiles.setFullTextSearch(searchWords.getList());
			// searching only the file entry (not folder), if folder match and is returned,
			// the paginate system is impacted
			queryFiles.setType(FolderManager.FILE_TYPE);
			//
			queryFiles.addSort("modified", ElementSort.Desc);
			queryFiles.addProjection("name");
			queryFiles.addProjection("modified");
			queryFiles.addProjection("folder");
			queryFiles.addProjection("owner");
			queryFiles.addProjection("ownerName");
			queryFiles.addProjection("comments");
			queryFiles.addProjection("isShared");
			queryFiles.addProjection("eParent");
			//
			final int skip = (0 == page) ? -1 : page * limit;
			queryFiles.setLimit(limit);
			queryFiles.setSkip(skip);

			//
			// start fetch
			//
			Future<JsonArray> futureQuery = Future.future();
			folderManager.findByQuery(queryFiles, user, futureQuery.completer());
			futureQuery.map(files -> {
				final List<String> aHeader = columnsHeader.getList();
				return files.stream().map(o -> (JsonObject) o).map(file -> {
					//
					// format results
					//
					final JsonObject formatted = new JsonObject();
					Optional<Date> modified = DocumentHelper.getModified(file);
					// default front route (no folder and the file belongs to the owner)
					String resourceURI = "/workspace/workspace";
					// RESOURCE URI
					String parent = DocumentHelper.getParent(file);
					// if parent folder is visible display it
					if (DocumentHelper.hasParent(file)) {
						if (DocumentHelper.isShared(file)) {
							// i can see the folder but i am not the owner so it is a folder shared
							resourceURI += "#/shared/folder/" + parent;
						} else {
							// i am the owner
							resourceURI += "#/folder/" + parent;
						}
					} else if (DocumentHelper.isShared(file)) {
						// only the file is shared
						resourceURI += "#/shared";
					}
					//
					// format description
					//
					final Set<String> unaccentWords = ((List<String>) searchWords.getList()).stream()
							.map(word -> StringUtils.stripAccentsToLowerCase(word)).collect(Collectors.toSet());
					JsonArray comments = file.getJsonArray("comments", new JsonArray());
					// get the last modified comment that match with searched words to create the
					// description
					List<JsonObject> commentsMatching = comments.stream().map(comment -> (JsonObject) comment)
							.filter(comment -> {
								// keep only comments matching all words
								String text = comment.getString("comment", "");
								String strippedText = StringUtils.stripAccentsToLowerCase(text);
								long countMatch = unaccentWords.stream().filter(word -> strippedText.contains(word))
										.count();
								return countMatch == unaccentWords.size();
							}).map(comment -> {
								// return modified dates and comments
								try {
									Date m = DateUtils.parse(comment.getString("posted"), PATTERN);
									comment.put("modifiedDate", m.getTime());
									return comment;
								} catch (ParseException e) {
									log.error("Can't parse date from posted", e);
									return null;
								}

							}).filter(v -> v != null).collect(Collectors.toList());
					// get most recent date
					Date modifiedDate = modified.orElse(null);
					String description = null;
					for (JsonObject comment : commentsMatching) {
						Long modifiedLong = comment.getLong("modifiedDate");
						Date d1 = modifiedLong == null ? null : new Date(modifiedLong);
						String text = comment.getString("comment", "");
						// set description at least with first comment
						description = description == null ? text : description;
						if (modifiedDate == null || (d1 != null && modifiedDate.before(d1))) {
							modifiedDate = d1;
							description = text;
						}
					}
					// fix i18 description
					if (commentsMatching.size() == 1) {
						description = i18n.translate("workspace.search.description.one", I18n.DEFAULT_DOMAIN, locale,
								description);
					} else if (commentsMatching.size() > 1) {
						description = i18n.translate("workspace.search.description.several", I18n.DEFAULT_DOMAIN,
								locale, commentsMatching.size() + "", description);
					}
					//
					// set formatted attributes
					//
					formatted.put(aHeader.get(0), DocumentHelper.getName(file));
					formatted.put(aHeader.get(1), description == null ? "" : description);
					Date modifiedNotNull = modifiedDate == null ? new Date() : modifiedDate;
					formatted.put(aHeader.get(2), new JsonObject().put("$date", modifiedNotNull.getTime()));
					formatted.put(aHeader.get(3), DocumentHelper.getOwnerName(file));
					formatted.put(aHeader.get(4), DocumentHelper.getOwner(file));
					formatted.put(aHeader.get(5), resourceURI);
					//
					return formatted;
				});

			}).setHandler(res -> {
				if (res.succeeded()) {
					JsonArray result = res.result().collect(JsonArray::new, JsonArray::add, JsonArray::addAll);
					handler.handle(new Either.Right<String, JsonArray>(result));
				} else {
					handler.handle(new Either.Left<String, JsonArray>(res.cause().getMessage()));
				}
			});
		} else {
			handler.handle(new Either.Right<String, JsonArray>(new fr.wseduc.webutils.collections.JsonArray()));
		}
	}
}