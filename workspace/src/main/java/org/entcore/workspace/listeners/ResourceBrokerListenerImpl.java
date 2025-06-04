package org.entcore.workspace.listeners;

import io.vertx.core.json.JsonObject;
import org.entcore.broker.api.dto.resources.ResourceInfoDTO;
import org.entcore.broker.proxy.ResourceBrokerListener;
import org.entcore.common.resources.MongoResourceBrokerListenerImpl;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Implementation of ResourceBrokerListener for the Workspace module.
 * Retrieves resource information from the workspace documents collection.
 * Implements ResourceBrokerListener to detect Broker Annotation
 */
public class ResourceBrokerListenerImpl extends MongoResourceBrokerListenerImpl implements ResourceBrokerListener {

    /**
     * Name of the MongoDB collection containing workspace documents
     */
    private static final String DOCUMENTS_COLLECTION = "documents";
    
    /**
     * Date format used in the documents collection
     */
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm.ss.SSS");

    /**
     * Create a new MongoDB implementation of ResourceBrokerListener.
     */
    public ResourceBrokerListenerImpl() {
        super(DOCUMENTS_COLLECTION);
    }
    
    /**
     * Convert MongoDB workspace document to ResourceInfoDTO.
     * Overrides parent method to match the specific document structure in workspace.
     *
     * @param resource The MongoDB document from workspace collection
     * @return ResourceInfoDTO with extracted information
     */
    @Override
    protected ResourceInfoDTO convertToResourceInfoDTO(JsonObject resource) {
        if (resource == null) {
            return null;
        }
        
        try {
            final String id = resource.getString("_id");
            // Use name instead of title
            final String title = resource.getString("name", "");
            // No direct description field, using empty string
            final String description = "";
            
            // Get thumbnail from thumbnails object if available
            String thumbnail = "";
            JsonObject thumbnails = resource.getJsonObject("thumbnails");
            if (thumbnails != null && !thumbnails.isEmpty()) {
                // Get the first thumbnail available (e.g. "640x480")
                String firstKey = thumbnails.fieldNames().iterator().next();
                thumbnail = thumbnails.getString(firstKey, "");
            }
            
            // Extract user information - different structure
            final String authorId = resource.getString("owner", "");
            final String authorName = resource.getString("ownerName", "");
            
            // Parse dates from string format
            final Date creationDate = parseStringDate(resource.getString("created"));
            final Date modificationDate = parseStringDate(resource.getString("modified"));
            
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
            log.error("Error converting Workspace document to ResourceInfoDTO", e);
            return null;
        }
    }
    
    /**
     * Parse date from workspace format (string) to Date object
     * @param dateStr Date string in format "yyyy-MM-dd HH:mm.ss.SSS"
     * @return Parsed Date or current date if parsing fails
     */
    private Date parseStringDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return new Date();
        }
        
        try {
            return DATE_FORMAT.parse(dateStr);
        } catch (ParseException e) {
            log.warn("Failed to parse date: " + dateStr, e);
            return new Date();
        }
    }
}
