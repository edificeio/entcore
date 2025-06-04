package org.entcore.common.resources;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.broker.api.dto.resources.ResourceInfoDTO;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * PostgreSQL implementation of ResourceBrokerListener.
 * Retrieves resource information from PostgreSQL table.
 */
public class PostgresResourceBrokerListenerImpl extends AbstractResourceBrokerListener {
    
    private final String table;
    private final Sql sql;
    private final SimpleDateFormat pgDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    
    /**
     * Create a new PostgreSQL implementation of ResourceBrokerListener.
     *
     * @param table The PostgreSQL table name containing resources
     */
    public PostgresResourceBrokerListenerImpl(final String table) {
        this.table = table;
        this.sql = Sql.getInstance();
        
        // Set timezone to UTC for date parsing
        this.pgDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
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
    
    /**
     * Convert PostgreSQL row to ResourceInfoDTO.
     *
     * @param resource The PostgreSQL row as JsonObject
     * @return ResourceInfoDTO with extracted information
     */
    @Override
    protected ResourceInfoDTO convertToResourceInfoDTO(JsonObject resource) {
        if (resource == null) {
            return null;
        }
        
        try {
            final String id = resource.getString("id");
            final String title = resource.getString("title", "");
            final String description = resource.getString("description", "");
            final String thumbnail = resource.getString("thumbnail", "");
            final String authorId = resource.getString("author_id", "");
            final String authorName = resource.getString("author_name", "");
            
            // Parse dates from PostgreSQL format
            final Date creationDate = parsePostgresDate(resource.getString("created_at"));
            final Date modificationDate = parsePostgresDate(resource.getString("updated_at"));
            
            return new ResourceInfoDTO(
                id,
                title,
                description,
                thumbnail,
                authorName,
                authorId,
                creationDate,
                modificationDate
            );
        } catch (Exception e) {
            log.error("Error converting PostgreSQL row to ResourceInfoDTO", e);
            return null;
        }
    }
    
    protected Date parsePostgresDate(String dateString) {
        if (dateString == null || dateString.isEmpty()) {
            return new Date();
        }
        
        try {
            return pgDateFormat.parse(dateString);
        } catch (ParseException e) {
            log.warn("Failed to parse date: {}", dateString);
            return new Date();
        }
    }
}