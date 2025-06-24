package org.entcore.common.resources;

import com.mongodb.client.model.Filters;
import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.mongodb.MongoQueryBuilder;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.bson.conversions.Bson;
import org.entcore.broker.api.dto.resources.ResourceInfoDTO;
import org.entcore.common.utils.DateUtils;

import java.util.*;

/**
 * MongoDB implementation of ResourceBrokerListener.
 * Retrieves resource information from MongoDB collection.
 */
public abstract class MongoResourceBrokerListenerImpl extends AbstractResourceBrokerListener {
    
    private final String collection;
    private final MongoDb mongo;
    
    /**
     * Create a new MongoDB implementation of ResourceBrokerListener.
     *
     * @param collection The MongoDB collection name containing resources
     */
    public MongoResourceBrokerListenerImpl(final String collection) {
        this.collection = collection;
        this.mongo = MongoDb.getInstance();
    }
    
    /**
     * Fetch resources from MongoDB by their IDs.
     *
     * @param resourceIds List of resource IDs to fetch
     * @return Future with list of resource objects
     */
    @Override
    protected Future<List<JsonObject>> fetchResourcesByIds(List<String> resourceIds) {
        Promise<List<JsonObject>> promise = Promise.promise();
        if(resourceIds == null || resourceIds.isEmpty()) {
            promise.complete(new ArrayList<>());
            return promise.future();
        }
        try {
            // Create query using MongoDB driver's Filters
            final Bson bsonQuery = Filters.in("_id", resourceIds);
            // Create query to find resources by IDs
            final JsonObject query = MongoQueryBuilder.build(bsonQuery);
            
            // Query MongoDB
            mongo.find(collection, query, result -> {
                if ("ok".equals(result.body().getString("status"))) {
                    final JsonArray results = result.body().getJsonArray("results", new JsonArray());
                    final List<JsonObject> resources = new ArrayList<>();
                    
                    for (int i = 0; i < results.size(); i++) {
                        resources.add(results.getJsonObject(i));
                    }
                    
                    promise.complete(resources);
                } else {
                    log.error("Error fetching resources from MongoDB: {0}", result.body().getString("message", "Unknown error"));
                    promise.fail(result.body().getString("message", "db.query.error"));
                }
            });
        } catch (Exception e) {
            log.error("Exception during MongoDB resource fetch", e);
            promise.fail(e);
        }
        
        return promise.future();
    }
    
    /**
     * This method can be used by subclasses to parse data from MongoDB documents
     */
    protected Date parseDate(Object date) {
        return DateUtils.parseDate(date);
    }
}