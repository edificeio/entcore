package org.entcore.common.events;

import fr.wseduc.webutils.security.SecureHttpServerRequest;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import org.entcore.broker.api.dto.event.CreateEventRequestDTO;
import org.entcore.broker.api.dto.event.CreateEventResponseDTO;
import org.entcore.broker.proxy.EventBrokerListener;
import org.entcore.common.http.request.JsonHttpServerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Implementation of EventBrokerListener to handle event storage via broker messaging system.
 * This class receives event storage requests, transforms them into appropriate format,
 * and delegates to the EventStore for actual storage.
 */
public class EventBrokerListenerImpl implements EventBrokerListener {

    private static final Logger log = LoggerFactory.getLogger(EventBrokerListenerImpl.class);

    /**
     * Constructor with EventStore dependency
     *
     */
    public EventBrokerListenerImpl() {}

    /**
     * Create and store an event based on the received request
     * 
     * @param request The request containing event details
     * @return Future with response indicating success/failure
     */
    @Override
    public Future<CreateEventResponseDTO> createAndStoreEvent(CreateEventRequestDTO request) {
        Promise<CreateEventResponseDTO> promise = Promise.promise();
        
        try {
            // Validate the request
            if (request == null || !request.isValid()) {
                log.error("Invalid request for createAndStoreEvent: {}", request);
                return Future.failedFuture("request.parameters.invalid");
            }
            
            final String eventType = request.getEventType();
            
            // Get the EventStore using the module parameter
            final EventStore eventStore = EventStoreFactory.getFactory().getEventStore(request.getModule());
            
            // Always create a simulated HttpServerRequest with all available information
            SecureHttpServerRequest httpRequest = createSimulatedHttpRequest(request);
            
            // Always use the same method to store the event
            eventStore.createAndStoreEvent(eventType, httpRequest);
            
            // Return success response
            promise.complete(new CreateEventResponseDTO(""));
            
        } catch (Exception e) {
            log.error("Error while creating and storing event", e);
            return Future.failedFuture("event.creation.error");
        }
        
        return promise.future();
    }
    
    /**
     * Creates a simulated HttpServerRequest based on the received request data
     * 
     * @param request The data transfer object containing request information
     * @return A SecureHttpServerRequest constructed from the provided data
     */
    private SecureHttpServerRequest createSimulatedHttpRequest(CreateEventRequestDTO request) {
        // Create base JSON structure for the request
        final JsonObject jsonRequest = new JsonObject();
        jsonRequest.put("path", request.getPath() != null ? request.getPath() : "/");
        
        // Set up headers
        final MultiMap headers = MultiMap.caseInsensitiveMultiMap();
        // force IP
        headers.add("X-Forwarded-For", request.getIp());
        // Add provided headers
        if (request.getHeaders() != null) {
            for (Map.Entry<String, String> header : request.getHeaders().entrySet()) {
                headers.add(header.getKey(), header.getValue());
            }
        }
        
        // Add specific headers if provided individually
        if (request.getUserAgent() != null) {
            headers.add("User-Agent", request.getUserAgent());
        }
        
        // Create and return the request
        return new SecureHttpServerRequest(new JsonHttpServerRequest(jsonRequest, headers));
    }
}