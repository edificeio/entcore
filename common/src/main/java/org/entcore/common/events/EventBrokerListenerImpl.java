package org.entcore.common.events;

import fr.wseduc.webutils.security.SecureHttpServerRequest;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import org.entcore.broker.api.dto.event.CreateEventRequestDTO;
import org.entcore.broker.api.dto.event.CreateEventResponseDTO;
import org.entcore.broker.proxy.EventBrokerListener;
import org.entcore.common.http.request.JsonHttpServerRequest;
import org.entcore.common.user.UserInfos;
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
            final JsonObject customAttributes = request.getCustomAttributes();
            
            // Get the EventStore using the module parameter
            final EventStore eventStore = EventStoreFactory.getFactory().getEventStore(request.getModule());
            
            // Determine which method to use based on available information
            if (request.getUserId() != null) {
                // Use userId-based event creation
                UserInfos user = new UserInfos();
                user.setUserId(request.getUserId());
                
                // Store the event
                eventStore.createAndStoreEvent(eventType, user, customAttributes);
                
                // Return success response
                promise.complete(new CreateEventResponseDTO(""));
            } else if (request.getLogin() != null) {
                if (request.getHeaders() != null || request.getPath() != null) {
                    // Create a simulated HttpServerRequest
                    HttpServerRequest httpRequest = createSimulatedHttpRequest(request);
                    
                    // Store the event
                    if (request.getClientId() != null) {
                        eventStore.createAndStoreEvent(eventType, request.getLogin(), request.getClientId(), httpRequest);
                    } else {
                        eventStore.createAndStoreEvent(eventType, request.getLogin(), httpRequest);
                    }
                } else {
                    // Use login-based event creation without HttpServerRequest
                    if (request.getClientId() != null) {
                        eventStore.createAndStoreEventByUserId(eventType, request.getLogin(), request.getClientId(), null);
                    } else {
                        eventStore.createAndStoreEvent(eventType, request.getLogin());
                    }
                }
                
                // Return success response
                promise.complete(new CreateEventResponseDTO(""));
            } else {
                // This should not happen if isValid() is properly implemented
                return Future.failedFuture("request.missing.user.information");
            }
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