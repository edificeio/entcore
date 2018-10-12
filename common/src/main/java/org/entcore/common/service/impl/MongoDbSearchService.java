/*
 * Copyright © "Open Digital Education", 2014
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