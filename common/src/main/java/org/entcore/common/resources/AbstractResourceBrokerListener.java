package org.entcore.common.resources;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import org.entcore.broker.api.dto.resources.GetResourcesRequestDTO;
import org.entcore.broker.api.dto.resources.GetResourcesResponseDTO;
import org.entcore.broker.api.dto.resources.ResourceInfoDTO;
import org.entcore.broker.proxy.ResourceBrokerListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Abstract base class for ResourceBrokerListener implementations.
 * Provides common functionality to retrieve resource information.
 */
public abstract class AbstractResourceBrokerListener implements ResourceBrokerListener {
    
    protected static final Logger log = LoggerFactory.getLogger(AbstractResourceBrokerListener.class);
    
    /**
     * Retrieves detailed information about multiple resources identified by their IDs.
     *
     * @param request The request object containing resource IDs
     * @return Future with response containing resource details
     */
    @Override
    public Future<GetResourcesResponseDTO> getResources(GetResourcesRequestDTO request) {
        Promise<GetResourcesResponseDTO> promise = Promise.promise();
        
        // Validate request
        if (request == null || !request.isValid()) {
            log.error("Invalid request for getResources: {}", request);
            return Future.failedFuture("request.parameters.invalid");
        }
        
        // Query resources
        fetchResourcesByIds(request.getResourceIds())
            .onSuccess(resources -> {
                try {
                    List<ResourceInfoDTO> resourceInfos = resources.stream()
                        .map(this::convertToResourceInfoDTO)
                        .collect(Collectors.toList());
                    
                    promise.complete(new GetResourcesResponseDTO(resourceInfos));
                } catch (Exception e) {
                    log.error("Error converting resources to DTOs", e);
                    promise.fail("error.converting.resources");
                }
            })
            .onFailure(err -> {
                log.error("Failed to fetch resources: {}", err.getMessage());
                promise.fail(err);
            });
        
        return promise.future();
    }
    
    /**
     * Fetch resources by their IDs from the data store.
     * This method is implemented differently for MongoDB and PostgreSQL.
     *
     * @param resourceIds List of resource IDs to fetch
     * @return Future with list of resource objects
     */
    protected abstract Future<List<JsonObject>> fetchResourcesByIds(List<String> resourceIds);
    
    /**
     * Convert a resource from data store to ResourceInfoDTO.
     * Each implementation will handle its specific data structure.
     *
     * @param resource The resource object from data store
     * @return A ResourceInfoDTO with extracted information
     */
    protected abstract ResourceInfoDTO convertToResourceInfoDTO(JsonObject resource);
}