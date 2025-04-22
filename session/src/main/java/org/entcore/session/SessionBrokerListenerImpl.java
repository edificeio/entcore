package org.entcore.session;

import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import org.entcore.broker.api.dto.session.*;
import org.entcore.broker.proxy.SessionBrokerListener;
import org.entcore.common.http.request.JsonHttpServerRequest;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of the SessionBrokerListener interface.
 * This class handles session management operations received through the message broker.
 * It uses AuthManager directly and utility methods from UserUtils.
 */
public class SessionBrokerListenerImpl implements SessionBrokerListener {
    
    private static final Logger log = LoggerFactory.getLogger(SessionBrokerListenerImpl.class);
    private final AuthManager authManager;
    
    /**
     * Constructor for SessionBrokerListenerImpl with AuthManager dependency.
     *
     * @param authManager The AuthManager instance for session operations
     */
    public SessionBrokerListenerImpl(AuthManager authManager) {
        this.authManager = authManager;
    }
    
    /**
     * Finds a session by its ID using the AuthManager and returns session information.
     * Uses UserUtils to convert the session JSON to UserInfos and then to SessionDto.
     * The session ID can be provided directly in the request or extracted from cookies.
     * 
     * @param request The request containing either the session ID or cookies to extract it
     * @return A Future containing the session response or an error
     */
    @Override
    public Future<FindSessionResponseDTO> findSession(FindSessionRequestDTO request) {
        // Validate the request
        if (request == null || !request.isValid()) {
            log.error("Invalid findSession request: request is null or invalid {}", request);
            return Future.failedFuture("request.parameters.invalid");
        }
        
        // Determine the session ID
        final String sessionId;
        
        // Try to use the explicit sessionId if provided
        if (request.getSessionId() != null) {
            sessionId = request.getSessionId();
            log.debug("Using explicit sessionId: {}", sessionId);
        } 
        // Otherwise, try to extract it from cookies if provided
        else {
            // Create a simulated HttpServerRequest with the cookies
            final MultiMap headers = MultiMap.caseInsensitiveMultiMap().add("Cookie", request.getCookies());
            final HttpServerRequest httpRequest = new JsonHttpServerRequest(new JsonObject(), headers);
            // Extract sessionId from signed cookies
            final Optional<String> maybeSessionId = UserUtils.getSessionId(httpRequest);
            if (maybeSessionId.isPresent()) {
                sessionId = maybeSessionId.get();
                log.debug("Extracted sessionId from cookies: {}", sessionId);
            } else {
                log.error("No sessionId found - explicit sessionId and cookies were missing or invalid");
                return Future.failedFuture("session.id.notfound");
            }
        }

        final Promise<FindSessionResponseDTO> promise = Promise.promise();
        // Use AuthManager to get session data
        authManager.findBySessionId(sessionId).onSuccess(sessionData -> {
            if (sessionData != null) {
                try {
                    // Use UserUtils to convert the session data to UserInfos
                    final UserInfos userInfos = UserUtils.sessionToUserInfos(sessionData);
                    if (userInfos != null) {
                        // Convert from UserInfos to SessionDTO
                        SessionDto sessionDto = fromUserInfos(userInfos);
                        promise.complete(new FindSessionResponseDTO(sessionDto));
                    } else {
                        log.warn("Failed to convert session data to UserInfos for session ID: {}", sessionId);
                        promise.fail("session.conversion.error");
                    }
                } catch (Exception e) {
                    log.error("Error processing session data", e);
                    promise.fail("session.processing.error");
                }
            } else {
                log.warn("Session not found for ID: {}", sessionId);
                promise.fail("session.not.found");
            }
        }).onFailure(error -> {
            log.error("Session not found for ID: {}. Because of error: {}", sessionId, error);
            promise.fail("session.not.found");
        });
        
        return promise.future();
    }

    /**
     * Creates a new SessionDto instance from a UserInfos object.
     * This method converts the user information from the common model to the broker API DTO format.
     *
     * @param userInfos The UserInfos object containing user data
     * @return A new SessionDto instance with data from the UserInfos object, or null if input is null
     */
    private SessionDto fromUserInfos(final UserInfos userInfos) {
        if (userInfos == null) {
            return null;
        }

        // Convert authorized actions if present
        final List<ActionDto> actionDtos = new ArrayList<>();
        if (userInfos.getAuthorizedActions() != null) {
            for (Object actionObj : userInfos.getAuthorizedActions()) {
                if (actionObj instanceof JsonObject) {
                    final JsonObject action = (JsonObject) actionObj;
                    actionDtos.add(new ActionDto(
                            action.getString("name"),
                            action.getString("displayName"),
                            action.getString("type")
                    ));
                }
            }
        }

        // Convert classes - optimized version using a single loop
        final List<ClassDto> classDtos = new ArrayList<>();
        if (userInfos.getClasses() != null) {
            final List<String> classNames = userInfos.getRealClassNames(); // May be null
            for (int i = 0; i < userInfos.getClasses().size(); i++) {
                final String classId = userInfos.getClasses().get(i);
                String className = null;
                
                // Use the className if available for this index
                if (classNames != null && i < classNames.size()) {
                    className = classNames.get(i);
                }
                
                classDtos.add(new ClassDto(classId, className));
            }
        }

        // Convert groups
        final List<GroupDto> groupDtos = new ArrayList<>();
        if (userInfos.getGroupsIds() != null) {
            for (String groupId : userInfos.getGroupsIds()) {
                groupDtos.add(new GroupDto(groupId, null));
            }
        }

        // Convert structures - using the same optimization pattern
        final List<StructureDto> structureDtos = new ArrayList<>();
        if (userInfos.getStructures() != null) {
            final List<String> structureNames = userInfos.getStructureNames(); // May be null
            for (int i = 0; i < userInfos.getStructures().size(); i++) {
                final String structureId = userInfos.getStructures().get(i);
                String structureName = null;
                
                // Use the structureName if available for this index
                if (structureNames != null && i < structureNames.size()) {
                    structureName = structureNames.get(i);
                }
                
                structureDtos.add(new StructureDto(structureId, structureName));
            }
        }

        // Create and return the SessionDto
        return new SessionDto(
                userInfos.getUserId(),
                userInfos.getExternalId(),
                userInfos.getFirstName(),
                userInfos.getLastName(),
                userInfos.getUsername(),
                userInfos.getBirthDate(),
                userInfos.getLevel(),
                userInfos.getType(),
                userInfos.getLogin(),
                userInfos.getEmail(),
                userInfos.getMobile(),
                actionDtos,
                classDtos,
                groupDtos,
                structureDtos
        );
    }
}