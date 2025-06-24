package org.entcore.common.resources;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.broker.api.dto.resources.ResourceInfoDTO;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.utils.DateUtils;
import java.util.*;

/**
 * PostgreSQL implementation of ResourceBrokerListener.
 * Retrieves resource information from PostgreSQL table.
 */
public abstract class PostgresResourceBrokerListenerImpl extends AbstractResourceBrokerListener {
    
    private final String table;
    private final Sql sql;
    
    /**
     * Create a new PostgreSQL implementation of ResourceBrokerListener.
     *
     * @param table The PostgreSQL table name containing resources
     */
    public PostgresResourceBrokerListenerImpl(final String table) {
        this.table = table;
        this.sql = Sql.getInstance();
    }
    
    /**
     * Fetch resources from PostgreSQL by their IDs.
     *
     * @param resourceIds List of resource IDs to fetch
     * @return Future with list of resource objects
     */
    @Override
    protected Future<List<JsonObject>> fetchResourcesByIds(List<String> resourceIds) {
        final Promise<List<JsonObject>> promise = Promise.promise();
        if(resourceIds == null || resourceIds.isEmpty()) {
            promise.complete(new ArrayList<>());
            return promise.future();
        }
        try {
            // Create query parameters
            final JsonArray params = new JsonArray();
            final StringBuilder placeholders = new StringBuilder();
            
            // Build placeholders for prepared statement
            for (int i = 0; i < resourceIds.size(); i++) {
                if (i > 0) placeholders.append(",");
                placeholders.append("?");
                params.add(resourceIds.get(i));
            }
            
            // Build SQL query
            final String query = "SELECT * FROM " + table + " r " +
                    "WHERE r.id IN (" + placeholders + ")";
            
            // Execute query
            sql.prepared(query, params, SqlResult.validResultHandler(result -> {
                if (result.isRight()) {
                    promise.complete(result.right().getValue().getList());
                } else {
                    log.error("Error fetching resources from PostgreSQL: {}", result.left().getValue());
                    promise.fail(result.left().getValue());
                }
            }));
        } catch (Exception e) {
            log.error("Exception during PostgreSQL resource fetch", e);
            promise.fail(e);
        }
        
        return promise.future();
    }
    
    protected Date parsePostgresDate(String dateString) {
        return DateUtils.parseIsoDateWithMillis(dateString);
    }
}