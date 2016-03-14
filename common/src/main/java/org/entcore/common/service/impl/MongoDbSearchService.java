package org.entcore.common.service.impl;

import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.mongodb.MongoQueryBuilder;
import fr.wseduc.webutils.Either;
import org.entcore.common.service.SearchService;
import org.entcore.common.service.VisibilityFilter;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.*;
import java.util.regex.Pattern;

import static org.entcore.common.mongodb.MongoDbResult.validResultsHandler;

/**
 * Created by dbreyton on 23/02/2016.
 */
public class MongoDbSearchService implements SearchService {
    protected final MongoDb mongo;
    protected final String collection;

    public MongoDbSearchService(String collection) {
        this.collection = collection;
        this.mongo = MongoDb.getInstance();
    }

    @Override
    public void search(String userId, List<String> groupIds, List<String> returnFields, List<String> searchWords, List<String> searchFields,
                       Integer page, Integer limit, Handler<Either<String, JsonArray>> handler) {
        final int skip = (0 == page) ? -1 : page * limit;

        final List<DBObject> groups = new ArrayList<DBObject>();
        groups.add(QueryBuilder.start("userId").is(userId).get());
        for (String gpId: groupIds) {
           groups.add(QueryBuilder.start("groupId").is(gpId).get());
        }

        final Map<String,List<DBObject>> wordsMap = new HashMap<String, List<DBObject>>();
        for (String field : searchFields) {
            final List<DBObject> listDb = new ArrayList<DBObject>();
            for (String word : searchWords) {
                listDb.add(QueryBuilder.start(field).regex(Pattern.compile(".*" + word + ".*", Pattern.CASE_INSENSITIVE)).get());
            }
            wordsMap.put(field, listDb);
        }

        final QueryBuilder worldsOrQuery = new QueryBuilder();
        for (final List<DBObject> words : wordsMap.values()) {
            worldsOrQuery.or(new QueryBuilder().and(words.toArray(new DBObject[words.size()])).get());
        }

        final QueryBuilder rightsOrQuery = new QueryBuilder().or(
                QueryBuilder.start("visibility").is(VisibilityFilter.PUBLIC.name()).get(),
                QueryBuilder.start("visibility").is(VisibilityFilter.PROTECTED.name()).get(),
                QueryBuilder.start("owner.userId").is(userId).get(),
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
                projection, skip, limit, Integer.MAX_VALUE, validResultsHandler(handler));
    }
}