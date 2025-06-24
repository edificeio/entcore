package org.entcore.workspace.listeners;

import io.vertx.core.json.JsonObject;
import org.entcore.broker.api.dto.resources.ResourceInfoDTO;
import org.entcore.broker.proxy.ResourceBrokerListener;
import org.entcore.common.resources.MongoResourceBrokerListenerImpl;
import org.entcore.common.utils.DateUtils;

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
            
            // Parse dates from string format using the centralized DateUtils
            final Date creationDate = DateUtils.parseDotSeparatedDate(resource.getString("created"));
            final Date modificationDate = DateUtils.parseDotSeparatedDate(resource.getString("modified"));
            
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
}
