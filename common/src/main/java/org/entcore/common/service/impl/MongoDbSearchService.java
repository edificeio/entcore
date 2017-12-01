/*
 * Copyright © WebServices pour l'Éducation, 2014
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
 */

package org.entcore.common.service.impl;

import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.mongodb.MongoQueryBuilder;
import fr.wseduc.webutils.Either;
import org.entcore.common.service.SearchService;
import org.entcore.common.service.VisibilityFilter;
import org.entcore.common.utils.StringUtils;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

import static org.entcore.common.mongodb.MongoDbResult.validResultsHandler;

/**
 * Created by dbreyton on 23/02/2016.
 */
public class MongoDbSearchService implements SearchService {
    protected final MongoDb mongo;
    protected final String collection;
    private final String ownerUserId;

    public MongoDbSearchService(String collection) {
        this(collection, null);
    }

    public MongoDbSearchService(String collection, String ownerUserId) {
        this.collection = collection;
        this.mongo = MongoDb.getInstance();

        this.ownerUserId = StringUtils.isEmpty(ownerUserId) ? "owner.userId" : ownerUserId;
    }


    @Override
    public void search(String userId, List<String> groupIds, List<String> returnFields, List<String> searchWords,
                       Integer page, Integer limit, Handler<Either<String, JsonArray>> handler) {
        final int skip = (0 == page) ? -1 : page * limit;

        final List<DBObject> groups = new ArrayList<DBObject>();
        groups.add(QueryBuilder.start("userId").is(userId).get());
        for (String gpId: groupIds) {
           groups.add(QueryBuilder.start("groupId").is(gpId).get());
        }

        final QueryBuilder worldsQuery = new QueryBuilder();
        //no stemming (in fact, stemming works only with words and for a given language) and no list of stop words
        worldsQuery.text(textSearchedComposition(searchWords));

        final QueryBuilder rightsOrQuery = new QueryBuilder().or(
                QueryBuilder.start("visibility").is(VisibilityFilter.PUBLIC.name()).get(),
                QueryBuilder.start("visibility").is(VisibilityFilter.PROTECTED.name()).get(),
                QueryBuilder.start(this.ownerUserId).is(userId).get(),
                QueryBuilder.start("shared").elemMatch(
                        new QueryBuilder().or(groups.toArray(new DBObject[groups.size()])).get()
                ).get());

        final QueryBuilder query = new QueryBuilder().and(worldsQuery.get(),rightsOrQuery.get());

        JsonObject sort = new JsonObject().put("modified", -1);
        final JsonObject projection = new JsonObject();
        for (String field : returnFields) {
            projection.put(field, 1);
        }

        mongo.find(collection, MongoQueryBuilder.build(query), sort,
                projection, skip, limit, Integer.MAX_VALUE, validResultsHandler(handler));
    }

    public static String textSearchedComposition(List<String> wordsLst) {
        final StringBuilder words = new StringBuilder();
        for (String word : wordsLst) {
            //For conjunction : use phrase and not word
            words.append("\"").append(word).append("\" ");
        }
        words.delete(words.length() - 1, words.length());
        return words.toString();
    }
}