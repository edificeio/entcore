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
            returnFields.add("owner");
            returnFields.add("ownerName");
            returnFields.add("comments");

            final List<String> searchFieldsInComments = new ArrayList<String>();
            searchFieldsInComments.add("comment");

            final int skip = (0 == page) ? -1 : page * limit;
            final List<String> groupAndUserids = groupIds.toList();
            final List<String> searchWordsLst = searchWords.toList();
            final List<DBObject> groups = new ArrayList<DBObject>();

            groups.add(QueryBuilder.start("userId").is(userId).get());
            for (String gpId: groupAndUserids) {
                groups.add(QueryBuilder.start("groupId").is(gpId).get());
            }

            //search on comments
            final Map<String,List<DBObject>> fieldsMap = new HashMap<String, List<DBObject>>();

            for (String field : searchFieldsInComments) {
                final List<DBObject> elemsMatch = new ArrayList<DBObject>();
                for (String word : searchWordsLst) {
                    final DBObject dbObject = QueryBuilder.start(field).regex(Pattern.compile(".*" +
                            MongoDbSearchService.accentTreating(word) + ".*", Pattern.CASE_INSENSITIVE)).get();
                    elemsMatch.add(QueryBuilder.start("comments").elemMatch(dbObject).get());
                }
                fieldsMap.put(field, elemsMatch);
            }

            final QueryBuilder worldsOrQuery = new QueryBuilder();

            for (final List<DBObject> field : fieldsMap.values()) {
                worldsOrQuery.or(new QueryBuilder().and(field.toArray(new DBObject[field.size()])).get());
            }

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

            mongo.find(collection, MongoQueryBuilder.build(query), sort,
                    projection, skip, limit, Integer.MAX_VALUE, new Handler<Message<JsonObject>>() {
                        @Override
                        public void handle(Message<JsonObject> event) {
                            final Either<String, JsonArray> ei = validResults(event);
                            if (ei.isRight()) {
                                final JsonArray res = formatSearchResult(ei.right().getValue(), columnsHeader, searchWordsLst,
                                        locale, userId);
                                handler.handle(new Either.Right<String, JsonArray>(res));
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
                                         final String locale, final String userId) {
        final List<String> aHeader = columnsHeader.toList();
        final JsonArray traity = new JsonArray();

        for (int i=0;i<results.size();i++) {
            final JsonObject j = results.get(i);
            final JsonObject jr = new JsonObject();
            if (j != null) {
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
                final String resourceURI = (userId.equals(j.getString("owner", "")) ? "/workspace/workspace" : "/workspace/workspace#/shared");
                jr.putString(aHeader.get(5), resourceURI);
                traity.add(jr);
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