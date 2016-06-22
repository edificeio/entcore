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
import org.entcore.common.service.SearchService;
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
    private final SearchService searchService;
    private final String collection;
    private static final I18n i18n = I18n.getInstance();
    private static String PATTERN = "yyyy-MM-dd HH:mm.ss.sss";

    public WorkspaceSearchingEvents(String collection, SearchService searchService) {
        this.collection = collection;
        this.searchService = searchService;
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

            final List<String> searchWordsLst = searchWords.toList();

            //main searching
            searchService.search(userId, groupIds.toList(), returnFields, searchWordsLst, page, limit, new Handler<Either<String, JsonArray>>() {
                @Override
                public void handle(Either<String, JsonArray> event) {
                    if (event.isRight()) {
                        final JsonArray globalJa = event.right().getValue();
                        final QueryBuilder foldersOrQuery = new QueryBuilder();
                        //different owner can have the same directory name
                        final HashMap<String, List<String>> alreadyMapFolder = new HashMap<String, List<String>>();
                        final String folder = "folder";

                        for (int i = 0; i < globalJa.size(); i++) {
                            final JsonObject j = globalJa.get(i);

                            // it's only if folder owner isn't already in searched scope
                            if (j != null && !StringUtils.isEmpty(j.getString(folder)) && (alreadyMapFolder.get(j.getString("owner")) != null &&
                                    !alreadyMapFolder.get(j.getString("owner")).contains(j.getString(folder)))) {
                                final String folderName = j.getString(folder);
                                final String owner = j.getString("owner");
                                if (alreadyMapFolder.containsKey(owner)) {
                                    alreadyMapFolder.get(owner).add(folderName);
                                } else {
                                    alreadyMapFolder.put(owner, Arrays.asList(folderName));
                                }

                                final QueryBuilder andQuery = new QueryBuilder();
                                // subdirectory management "_" allow to create a tree
                                andQuery.and(new QueryBuilder().start(folder).is(folderName).get());
                                final List<String> realNameFolderLst = StringUtils.split(folderName, "_");
                                final String realNameFolder = realNameFolderLst.get(realNameFolderLst.size() - 1);
                                andQuery.and(new QueryBuilder().start("name").is(realNameFolder).get());
                                andQuery.and(new QueryBuilder().start("owner").is(owner).get());
                                foldersOrQuery.or(andQuery.get());
                            }
                        }

                        final JsonObject projection = new JsonObject();
                        projection.putNumber("folder", 1);
                        projection.putNumber("owner", 1);
                        //search all folder of main result set
                        mongo.find(collection, MongoQueryBuilder.build(foldersOrQuery), null,
                                projection, new Handler<Message<JsonObject>>() {
                                    @Override
                                    public void handle(Message<JsonObject> event) {
                                        final Either<String, JsonArray> ei = validResults(event);
                                        if (ei.isRight()) {
                                            final JsonArray folderJa = ei.right().getValue();
                                            final Map<String, Map<String, String>> mapOwnerMapNameFolderId = new HashMap<String, Map<String, String>>();
                                            // fill the map owner key with map folder name key, folder id value
                                            for (int i = 0; i < folderJa.size(); i++) {
                                                final JsonObject j = folderJa.get(i);
                                                if (j != null) {
                                                    final String owner = j.getString("owner", "");
                                                    if (mapOwnerMapNameFolderId.containsKey(owner)) {
                                                        mapOwnerMapNameFolderId.get(owner).put(j.getString("folder"), j.getString("_id"));
                                                    } else {
                                                        final Map<String, String> mapFolderId =
                                                                new HashMap<String, String>();
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
                    } else {
                        handler.handle(new Either.Left<String, JsonArray>(event.left().getValue()));
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
                                         final String locale, final String userId, final Map<String, Map<String, String>> mapOwnerMapNameFolderId) {
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
                    final String owner = j.getString("owner", "");
                    final Map<String, Object> map = formatDescription(j.getArray("comments", new JsonArray()),
                            words, modified, locale);
                    jr.putString(aHeader.get(0), j.getString("name"));
                    jr.putString(aHeader.get(1), map.get("description").toString());
                    jr.putObject(aHeader.get(2), new JsonObject().putValue("$date", ((Date) map.get("modified")).getTime()));
                    jr.putString(aHeader.get(3), j.getString("ownerName", ""));
                    jr.putString(aHeader.get(4), owner);
                    String resourceURI = "/workspace/workspace";

                    if (userId.equals(owner)) {
                        if (!StringUtils.isEmpty(folder)) {
                            resourceURI += "#/folder/" + mapOwnerMapNameFolderId.get(owner).get(folder);
                        }
                    } else {
                        if (!StringUtils.isEmpty(folder)) {
                            resourceURI += "#/shared/folder/" + mapOwnerMapNameFolderId.get(owner).get(folder);
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
            map.put("description", i18n.translate("workspace.search.description.one", locale, comment));
        } else {
            map.put("modified", modifiedRes);
            map.put("description", i18n.translate("workspace.search.description.several", locale,
                    countMatchComment.toString(), comment));
        }

        return  map;
    }
}