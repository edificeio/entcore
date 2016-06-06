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
import org.entcore.common.service.impl.MongoDbSearchService;
import org.entcore.common.utils.DateUtils;
import org.entcore.common.utils.StringUtils;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

import java.text.ParseException;
import java.util.*;
import java.util.regex.Pattern;

import static org.entcore.common.mongodb.MongoDbResult.validResults;

/**
 * Created by dbreyton on 03/06/2016.
 */
public class WorkspaceSearchingEvents implements SearchingEvents {
    private static final Logger log = LoggerFactory.getLogger(WorkspaceSearchingEvents.class);
    private final MongoDb mongo;
    private final String collection;
    private static final I18n i18n = I18n.getInstance();
    private static String PATTERN = "yyyy-MM-dd HH:mm.ss.sss";

    public WorkspaceSearchingEvents(String collection) {
        this.collection = collection;
        this.mongo = MongoDb.getInstance();
    }

    @Override
    public void searchResource(List<String> appFilters, final String userId, JsonArray groupIds, JsonArray searchWords,
                               Integer page, Integer limit, final JsonArray columnsHeader,
                               final String locale, final Handler<Either<String, JsonArray>> handler) {
        if (appFilters.contains(WorkspaceSearchingEvents.class.getSimpleName())) {
            final List<String> returnFields = new ArrayList<String>();
            returnFields.add("name");
            returnFields.add("modified");
            returnFields.add("folder");
            returnFields.add("owner");
            returnFields.add("ownerName");
            returnFields.add("comments");

            final int skip = (0 == page) ? -1 : page * limit;
            final List<String> groupAndUserids = groupIds.toList();
            final List<String> searchWordsLst = searchWords.toList();
            final List<DBObject> groups = new ArrayList<DBObject>();

            groups.add(QueryBuilder.start("userId").is(userId).get());
            for (String gpId: groupAndUserids) {
                groups.add(QueryBuilder.start("groupId").is(gpId).get());
            }

            //search on main name and folder
            final String name = "name";
            final List<DBObject> listMainNameField = new ArrayList<DBObject>();
            final String folder= "folder";
            final List<DBObject> listMainFolderField = new ArrayList<DBObject>();

            //search on comments
            final String comment = "comment";
            final List<DBObject> elemsMatchComment = new ArrayList<DBObject>();

            for (String word : searchWordsLst) {
                final DBObject dbObjectName = QueryBuilder.start(name).regex(Pattern.compile(".*" +
                        MongoDbSearchService.accentTreating(word) + ".*", Pattern.CASE_INSENSITIVE)).get();
                final DBObject dbObjectFolder = QueryBuilder.start(folder).regex(Pattern.compile(".*" +
                        MongoDbSearchService.accentTreating(word) + ".*", Pattern.CASE_INSENSITIVE)).get();
                final DBObject dbObjectComment = QueryBuilder.start(comment).regex(Pattern.compile(".*" +
                        MongoDbSearchService.accentTreating(word) + ".*", Pattern.CASE_INSENSITIVE)).get();
                elemsMatchComment.add(QueryBuilder.start("comments").elemMatch(dbObjectComment).get());
                listMainNameField.add(dbObjectName);
                listMainFolderField.add(dbObjectFolder);
            }

            final QueryBuilder worldsOrQuery = new QueryBuilder();
            worldsOrQuery.or(new QueryBuilder().and(elemsMatchComment.toArray(new DBObject[elemsMatchComment.size()])).get());
            worldsOrQuery.or(new QueryBuilder().and(listMainNameField.toArray(new DBObject[listMainNameField.size()])).get());
            worldsOrQuery.or(new QueryBuilder().and(listMainFolderField.toArray(new DBObject[listMainFolderField.size()])).get());

            final QueryBuilder rightsOrQuery = new QueryBuilder().or(
                    QueryBuilder.start("owner").is(userId).get(),
                    QueryBuilder.start("shared").elemMatch(
                            new QueryBuilder().or(groups.toArray(new DBObject[groups.size()])).get()
                    ).get());

            final QueryBuilder query = new QueryBuilder().and(worldsOrQuery.get(),rightsOrQuery.get());

            JsonObject sort = new JsonObject().putNumber("modified", -1);
            final JsonObject projection = new JsonObject();
            for (String field : returnFields) {
                projection.putNumber(field, 1);
            }
            //main searching
            mongo.find(collection, MongoQueryBuilder.build(query), sort,
                    projection, skip, limit, Integer.MAX_VALUE, new Handler<Message<JsonObject>>() {
                        @Override
                        public void handle(Message<JsonObject> event) {
                            final Either<String, JsonArray> ei = validResults(event);
                            if (ei.isRight()) {
                                final JsonArray globalJa = ei.right().getValue();
                                final QueryBuilder foldersOrQuery = new QueryBuilder();
                                final Set<String> alreadySetFolder = new HashSet<String>();
                                // fill the map key : folder name, value : folder id
                                for (int i=0;i<globalJa.size();i++) {
                                    final JsonObject j = globalJa.get(i);
                                    // it's only if folder isn't already in searched scope
                                    if (j != null && !StringUtils.isEmpty(j.getString(folder)) && !alreadySetFolder.contains(j.getString(folder))) {
                                        final String folderName = j.getString(folder);
                                        alreadySetFolder.add(folderName);
                                        final QueryBuilder andQuery = new QueryBuilder();
                                        // subdirectory management "_" allow to create a tree
                                        andQuery.and(new QueryBuilder().start(folder).is(folderName).get());
                                        final List<String> realNameFolderLst = StringUtils.split(folderName, "_");
                                        final String realNameFolder = realNameFolderLst.get(realNameFolderLst.size() -1);
                                        andQuery.and(new QueryBuilder().start("name").is(realNameFolder).get());
                                        andQuery.and(new QueryBuilder().start("owner").is(j.getString("owner")).get());
                                        foldersOrQuery.or(andQuery.get());
                                    }
                                }

                                final JsonObject projection = new JsonObject();
                                projection.putNumber("folder", 1);
                                //search all folder of main result set
                                mongo.find(collection, MongoQueryBuilder.build(foldersOrQuery), null,
                                        projection, new Handler<Message<JsonObject>>() {
                                            @Override
                                            public void handle(Message<JsonObject> event) {
                                                final Either<String, JsonArray> ei = validResults(event);
                                                if (ei.isRight()) {
                                                    final JsonArray folderJa = ei.right().getValue();
                                                    final Map<String, String> mapNameFolderId = new HashMap<String, String>();
                                                    // fill the map key : folder name, value : folder id
                                                    for (int i=0;i < folderJa.size();i++) {
                                                        final JsonObject j = folderJa.get(i);
                                                        if (j != null) {
                                                            mapNameFolderId.put(j.getString("folder"), j.getString("_id"));
                                                        }
                                                    }
                                                    final JsonArray res = formatSearchResult(globalJa, columnsHeader, searchWordsLst,
                                                            locale, userId, mapNameFolderId);
                                                    handler.handle(new Either.Right<String, JsonArray>(res));
                                                } else {
                                                    handler.handle(new Either.Left<String, JsonArray>(ei.left().getValue()));
                                                }
                                            }
                                        });
                            } else {
                                handler.handle(new Either.Left<String, JsonArray>(ei.left().getValue()));
                            }
                            if (log.isDebugEnabled()) {
                                log.debug("[WorkspaceSearchingEvents][searchResource] The resources searched by user are finded");
                            }
                        }
                    });
        } else {
            handler.handle(new Either.Right<String, JsonArray>(new JsonArray()));
        }
    }

    private JsonArray formatSearchResult(final JsonArray results, final JsonArray columnsHeader, final List<String> words,
                                         final String locale, final String userId, final Map<String, String> mapNameFolderId) {
        final List<String> aHeader = columnsHeader.toList();
        final JsonArray traity = new JsonArray();

        for (int i=0;i<results.size();i++) {
            final JsonObject j = results.get(i);
            final JsonObject jr = new JsonObject();

            if (j != null) {
                final List<String> realNameFolderLst = StringUtils.split(j.getString("folder",""), "_");
                final String realNameFolder = (realNameFolderLst.size() > 0) ? realNameFolderLst.get(realNameFolderLst.size() -1) : "";
                //don't return folder
                if (!realNameFolder.equals(j.getString("name",""))) {
                    final String folder = j.getString("folder", "");
                    //it's a result to return
                    Date modified = new Date();
                    try {
                        modified = DateUtils.parse(j.getString("modified"), PATTERN);
                    } catch (ParseException e) {
                        log.error("Can't parse date from modified", e);
                    }
                    final Map<String, Object> map = formatDescription(j.getArray("comments", new JsonArray()),
                            words, modified, locale);
                    jr.putString(aHeader.get(0), j.getString("name"));
                    jr.putString(aHeader.get(1), map.get("description").toString());
                    jr.putObject(aHeader.get(2), new JsonObject().putValue("$date", ((Date) map.get("modified")).getTime()));
                    jr.putString(aHeader.get(3), j.getString("ownerName", ""));
                    jr.putString(aHeader.get(4), j.getString("owner", ""));
                    String resourceURI = "/workspace/workspace";

                    if (userId.equals(j.getString("owner", ""))) {
                        if (!StringUtils.isEmpty(folder)) {
                            resourceURI += "#/folder/" + mapNameFolderId.get(folder);
                        }
                    } else {
                        if (!StringUtils.isEmpty(folder)) {
                            resourceURI += "#/shared/folder/" + mapNameFolderId.get(folder);
                        } else {
                            resourceURI += "#/shared";
                        }
                    }
                    jr.putString(aHeader.get(5), resourceURI);
                    traity.add(jr);
                }
            }
        }
        return traity;
    }

    private Map<String, Object> formatDescription(JsonArray ja, final List<String> words, Date defaultDate, String locale) {
        final Map<String, Object> map = new HashMap<String, Object>();

        Integer countMatchComment = 0;
        Date modifiedRes = null;
        Date modifiedMarker = null;
        String comment = "";

        final List<String> unaccentWords = new ArrayList<String>();
        for (final String word : words) {
            unaccentWords.add(StringUtils.stripAccentsToLowerCase(word));
        }

        //get the last modified comment that match with searched words for create the description
        for(int i=0;i<ja.size();i++) {
            final JsonObject jO = ja.get(i);

            final String commentTmp = jO.getString("comment", "");
            Date currentDate = null;
            try {
                currentDate = DateUtils.parse(jO.getString("posted"), PATTERN);
            } catch (ParseException e) {
                log.error("Can't parse date from posted", e);
            }

            boolean match = false;
            for (final String word : unaccentWords) {
                if (StringUtils.stripAccentsToLowerCase(commentTmp).contains(word)) {
                    match = true;
                    break;
                }
            }
            if (countMatchComment == 0 && match) {
                modifiedRes = currentDate;
            } else if (countMatchComment > 0 && match && currentDate != null && modifiedMarker != null &&
                    modifiedMarker.before(currentDate)) {
                modifiedMarker = currentDate;
                modifiedRes = currentDate;
            }
            if (match) {
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
            map.put("description", i18n.translate("workspace.search.description.one", locale, comment));
        } else {
            map.put("modified", modifiedRes);
            map.put("description", i18n.translate("workspace.search.description.several", locale,
                    countMatchComment.toString(), comment));
        }

        return  map;
    }
}