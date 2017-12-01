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

package org.entcore.workspace.service.impl;

import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.mongodb.MongoQueryBuilder;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import org.entcore.common.search.SearchingEvents;
import org.entcore.common.service.VisibilityFilter;
import org.entcore.common.service.impl.MongoDbSearchService;
import org.entcore.common.utils.DateUtils;
import org.entcore.common.utils.StringUtils;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.text.ParseException;
import java.util.*;

import static org.entcore.common.mongodb.MongoDbResult.validResults;
import static org.entcore.common.mongodb.MongoDbResult.validResultsHandler;

/**
 * Created by dbreyton on 03/06/2016.
 */
public class WorkspaceSearchingEvents implements SearchingEvents {
    private static final Logger log = LoggerFactory.getLogger(WorkspaceSearchingEvents.class);
    private final MongoDb mongo;
    private final String collection;
    private static final I18n i18n = I18n.getInstance();
    private static final String PATTERN = "yyyy-MM-dd HH:mm.ss.sss";

    public WorkspaceSearchingEvents(String collection) {
        this.collection = collection;
        this.mongo = MongoDb.getInstance();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void searchResource(List<String> appFilters, final String userId, JsonArray groupIds, JsonArray searchWords,
                               Integer page, Integer limit, final JsonArray columnsHeader,
                               final String locale, final Handler<Either<String, JsonArray>> handler) {
        if (appFilters.contains(WorkspaceSearchingEvents.class.getSimpleName())) {

            final List<String> searchWordsLst = searchWords.getList();

            final List<String> groupIdsLst = groupIds.getList();
            final List<DBObject> groups = new ArrayList<>();
            groups.add(QueryBuilder.start("userId").is(userId).get());
            for (String gpId: groupIdsLst) {
                groups.add(QueryBuilder.start("groupId").is(gpId).get());
            }

            final QueryBuilder rightsQuery = new QueryBuilder().or(
                    QueryBuilder.start("visibility").is(VisibilityFilter.PUBLIC.name()).get(),
                    QueryBuilder.start("visibility").is(VisibilityFilter.PROTECTED.name()).get(),
                    QueryBuilder.start("visibility").is(VisibilityFilter.PROTECTED.name()).get(),
                    QueryBuilder.start("owner").is(userId).get(),
                    QueryBuilder.start("shared").elemMatch(
                            new QueryBuilder().or(groups.toArray(new DBObject[groups.size()])).get()
                    ).get());

            final QueryBuilder worldsQuery = new QueryBuilder();
            worldsQuery.text(MongoDbSearchService.textSearchedComposition(searchWordsLst));

            //searching only the file entry (not folder), if folder match and is returned, the paginate system is impacted
            final QueryBuilder fileQuery = QueryBuilder.start("file").exists(true);

            final QueryBuilder query = new QueryBuilder().and(fileQuery.get(), rightsQuery.get(), worldsQuery.get());

            JsonObject sort = new JsonObject().put("modified", -1);
            final JsonObject projection = new JsonObject();
            projection.put("name", 1);
            projection.put("modified", 1);
            projection.put("folder", 1);
            projection.put("owner", 1);
            projection.put("ownerName", 1);
            projection.put("comments", 1);

            final int skip = (0 == page) ? -1 : page * limit;

            //main search on file
            mongo.find(this.collection, MongoQueryBuilder.build(query), sort,
                    projection, skip, limit, Integer.MAX_VALUE, validResultsHandler(new Handler<Either<String, JsonArray>>() {
                        @Override
                        public void handle(Either<String, JsonArray> event) {

                    if (event.isRight()) {
                        final JsonArray globalJa = event.right().getValue();
                        //different owner can have the same folder name
                        //if the folder exists, we must find id of folder according to the directory name and the owner,
                        //it's necessary to use the front route for the resource link
                        final QueryBuilder foldersOwnersOrQuery = new QueryBuilder();
                        Boolean isFolderProcessing = false;

                        for (int i = 0; i < globalJa.size(); i++) {
                            final JsonObject j = globalJa.getJsonObject(i);

                            // processing only files that have a folder
                            if (j != null && !StringUtils.isEmpty(j.getString("folder"))) {
                                isFolderProcessing = true;
                                final QueryBuilder folderOwnerQuery = new QueryBuilder();
                                folderOwnerQuery.and(QueryBuilder.start("folder").is(j.getString("folder")).get(),
                                        QueryBuilder.start("owner").is(j.getString("owner")).get());
                                foldersOwnersOrQuery.or(folderOwnerQuery.get());
                            }
                        }

                        final Map<String, Map<String, String>> mapOwnerMapNameFolderId = new HashMap<>();
                        //finding ids of folder found.
                        if (isFolderProcessing) {
                            //find only authorized folder entry and not file
                            final QueryBuilder queryFindFolderIds =
                                    new QueryBuilder().and(QueryBuilder.start("file").exists(false).get(), rightsQuery.get(),
                                            foldersOwnersOrQuery.get()) ;
                            //search all folder of main result set and format the search result
                           findFoldersIdAndFormatResult(globalJa, queryFindFolderIds, columnsHeader, searchWordsLst, locale, userId, handler);
                        } else {
                            //all files without folder
                            final JsonArray res = formatSearchResult(globalJa, columnsHeader, searchWordsLst,
                                    locale, userId, mapOwnerMapNameFolderId);
                            handler.handle(new Either.Right<String, JsonArray>(res));
                        }
                    } else {
                        handler.handle(new Either.Left<String, JsonArray>(event.left().getValue()));
                    }
                    if (log.isDebugEnabled()) {
                        log.debug("[WorkspaceSearchingEvents][searchResource] The resources searched by user are finded");
                    }
                }
            }));
        } else {
            handler.handle(new Either.Right<String, JsonArray>(new JsonArray()));
        }
    }

    private void findFoldersIdAndFormatResult(final JsonArray globalJa, QueryBuilder queryFindFolderIds, final JsonArray columnsHeader,
                                              final List<String> searchWordsLst, final String locale, final String userId,
                                              final Handler<Either<String, JsonArray>> handler) {
        final JsonObject projection = new JsonObject();
        projection.put("folder", 1);
        projection.put("owner", 1);
        mongo.find(collection, MongoQueryBuilder.build(queryFindFolderIds), null,
                projection, new Handler<Message<JsonObject>>() {
                    @Override
                    public void handle(Message<JsonObject> event) {
                        final Either<String, JsonArray> ei = validResults(event);
                        if (ei.isRight()) {
                            final JsonArray folderJa = ei.right().getValue();
                            final Map<String, Map<String, String>> mapOwnerMapNameFolderId = new HashMap<>();
                            // fill the map owner key with map folder name key, folder id value
                            for (int i = 0; i < folderJa.size(); i++) {
                                final JsonObject j = folderJa.getJsonObject(i);
                                if (j != null) {
                                    final String owner = j.getString("owner", "");
                                    if (mapOwnerMapNameFolderId.containsKey(owner)) {
                                        mapOwnerMapNameFolderId.get(owner).put(j.getString("folder"), j.getString("_id"));
                                    } else {
                                        final Map<String, String> mapFolderId =
                                                new HashMap<>();
                                        mapFolderId.put(j.getString("folder"), j.getString("_id"));
                                        mapOwnerMapNameFolderId.put(owner, mapFolderId);
                                    }
                                }
                            }
                            final JsonArray res = formatSearchResult(globalJa, columnsHeader, searchWordsLst,
                                    locale, userId, mapOwnerMapNameFolderId);
                            handler.handle(new Either.Right<String, JsonArray>(res));
                        } else {
                            handler.handle(new Either.Left<String, JsonArray>(ei.left().getValue()));
                        }
                    }
                });
    }

    @SuppressWarnings("unchecked")
    private JsonArray formatSearchResult(final JsonArray results, final JsonArray columnsHeader, final List<String> words,
                                         final String locale, final String userId, final Map<String, Map<String, String>> mapOwnerMapNameFolderId) {
        final List<String> aHeader = columnsHeader.getList();
        final JsonArray traity = new JsonArray();

        for (int i=0;i<results.size();i++) {
            final JsonObject j = results.getJsonObject(i);
            final JsonObject jr = new JsonObject();

            if (j != null) {
                final String folder = j.getString("folder", "");

                Date modified = new Date();
                try {
                    modified = DateUtils.parse(j.getString("modified"), PATTERN);
                } catch (ParseException e) {
                    log.error("Can't parse date from modified", e);
                }
                final String owner = j.getString("owner", "");
                final Map<String, Object> map = formatDescription(j.getJsonArray("comments", new JsonArray()),
                        words, modified, locale);
                jr.put(aHeader.get(0), j.getString("name"));
                jr.put(aHeader.get(1), map.get("description").toString());
                jr.put(aHeader.get(2), new JsonObject().put("$date", ((Date) map.get("modified")).getTime()));
                jr.put(aHeader.get(3), j.getString("ownerName", ""));
                jr.put(aHeader.get(4), owner);
                //default front route (no folder and the file belongs to the owner)
                String resourceURI = "/workspace/workspace";

                if (userId.equals(owner)) {
                    if (!StringUtils.isEmpty(folder)) {
                        resourceURI += "#/folder/" + mapOwnerMapNameFolderId.get(owner).get(folder);
                    }
                } else {
                    //if there is a folder on entry file and this folder is shared
                    if (!StringUtils.isEmpty(folder) && mapOwnerMapNameFolderId.containsKey(owner) &&
                            mapOwnerMapNameFolderId.get(owner).containsKey(folder)) {
                        resourceURI += "#/shared/folder/" + mapOwnerMapNameFolderId.get(owner).get(folder);
                    } else {
                        //only the file is shared
                        resourceURI += "#/shared";
                    }
                }
                jr.put(aHeader.get(5), resourceURI);
                traity.add(jr);
            }
        }
        return traity;
    }

    private Map<String, Object> formatDescription(JsonArray ja, final List<String> words, Date defaultDate, String locale) {
        final Map<String, Object> map = new HashMap<>();

        Integer countMatchComment = 0;
        Date modifiedRes = null;
        Date modifiedMarker = null;
        String comment = "";

        final List<String> unaccentWords = new ArrayList<>();
        for (final String word : words) {
            unaccentWords.add(StringUtils.stripAccentsToLowerCase(word));
        }

        //get the last modified comment that match with searched words for create the description
        for(int i=0;i<ja.size();i++) {
            final JsonObject jO = ja.getJsonObject(i);

            final String commentTmp = jO.getString("comment", "");
            Date currentDate = null;
            try {
                currentDate = DateUtils.parse(jO.getString("posted"), PATTERN);
            } catch (ParseException e) {
                log.error("Can't parse date from posted", e);
            }

            int match = unaccentWords.size();
            for (final String word : unaccentWords) {
                if (StringUtils.stripAccentsToLowerCase(commentTmp).contains(word)) {
                    match--;
                }
            }
            if (countMatchComment == 0 && match == 0) {
                modifiedRes = currentDate;
            } else if (countMatchComment > 0 && match == 0 && currentDate != null && modifiedMarker != null &&
                    modifiedMarker.before(currentDate)) {
                modifiedMarker = currentDate;
                modifiedRes = currentDate;
            }
            if (match == 0) {
                comment = commentTmp;
                if (currentDate != null) {
                    modifiedMarker = currentDate;
                }
                countMatchComment++;
            }
        }

        if (countMatchComment == 0) {
            map.put("modified", defaultDate);
            map.put("description", "");
        } else if (countMatchComment == 1) {
            map.put("modified", modifiedRes);
            map.put("description", i18n.translate("workspace.search.description.one", I18n.DEFAULT_DOMAIN, locale, comment));
        } else {
            map.put("modified", modifiedRes);
            map.put("description", i18n.translate("workspace.search.description.several", I18n.DEFAULT_DOMAIN, locale,
                    countMatchComment.toString(), comment));
        }

        return  map;
    }
}