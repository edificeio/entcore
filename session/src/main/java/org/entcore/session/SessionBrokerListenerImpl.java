package org.entcore.session;

import fr.wseduc.webutils.request.CookieHelper;
import fr.wseduc.webutils.request.filter.UserAuthFilter;
import fr.wseduc.webutils.security.SecureHttpServerRequest;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.LocalMap;
import org.entcore.broker.api.dto.session.*;
import org.entcore.broker.proxy.SessionBrokerListener;
import org.entcore.common.events.EventStoreFactory;
import org.entcore.common.http.BasicFilter;
import org.entcore.common.http.QueryParamTokenFilter;
import org.entcore.common.http.UserAuthWithQueryParamFilter;
import org.entcore.common.http.filter.AppOAuthResourceProvider;
import org.entcore.common.http.request.JsonHttpServerRequest;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Implementation of the SessionBrokerListener interface.
 * This class handles session management operations received through the message broker.
 * It uses AuthManager directly and utility methods from UserUtils.
 */
public class SessionBrokerListenerImpl implements SessionBrokerListener {
    
    private static final Logger log = LoggerFactory.getLogger(SessionBrokerListenerImpl.class);
    private final AuthManager authManager;
    private final LocalMap<Object, Object> serverConfig;
    /**
     * Constructor for SessionBrokerListenerImpl with AuthManager dependency.
     *
     * @param authManager The AuthManager instance for session operations
     */
    public SessionBrokerListenerImpl(AuthManager authManager) {
        this.authManager = authManager;
        final Vertx vertx = authManager.getVertx();
        this.serverConfig = authManager.getVertx().sharedData().getLocalMap("server");
        // init cookie helper
        CookieHelper.getInstance().init((String) serverConfig.get("signKey"),
                (String) serverConfig.get("sameSiteValue"),
                io.vertx.core.logging.LoggerFactory.getLogger(getClass()));
        // init needed by OAuthResourceProvider
        EventStoreFactory.getFactory().setVertx(vertx);
    }
    
    /**
     * Finds a session using multiple authentication methods through UserAuthWithQueryParamFilter:
     * - Direct sessionId
     * - JWT token (from headers or query params)
     * - OAuth Bearer token
     * - Cookie-based session
     * 
     * @param request The request containing authentication information
     * @return A Future containing the session response or an error
     */
    @Override
    public Future<FindSessionResponseDTO> findSession(FindSessionRequestDTO request) {
        // Validate the request
        if (request == null || !request.isValid()) {
            log.error("Invalid findSession request: request is null or invalid {}", request);
            return Future.failedFuture("request.parameters.invalid");
        }
        
        // Direct sessionId check first (highest priority)
        if (request.getSessionId() != null && !request.getSessionId().isEmpty()) {
            return getSessionById(request.getSessionId());
        }
        
        // Create a request that can be used with the filter
        final SecureHttpServerRequest secureRequest = createSecureRequest(request);
        
        // Use UserAuthWithQueryParamFilter to get session
        return getSessionWithFilter(secureRequest, request.getPathPrefix());
    }

    /**
     * Creates a SecureHttpServerRequest from the FindSessionRequestDTO
     * This encapsulates all HTTP request simulation logic
     * 
     * @param request The FindSessionRequestDTO with authentication data
     * @return A SecureHttpServerRequest that can be used with filters
     */
    private SecureHttpServerRequest createSecureRequest(FindSessionRequestDTO request) {
        final JsonObject jsonRequest = new JsonObject();
        final MultiMap headers = MultiMap.caseInsensitiveMultiMap();
        
        // Add cookies if available
        if (request.getCookies() != null && !request.getCookies().isEmpty()) {
            headers.add("Cookie", request.getCookies());
        }
        
        // Add any other headers from the request (Authorization, etc.)
        if (request.getHeaders() != null && !request.getHeaders().isEmpty()) {
            for (Map.Entry<String, String> header : request.getHeaders().entrySet()) {
                headers.add(header.getKey(), header.getValue());
            }
        }
        
        // Add query parameters
        if (request.getParams() != null && !request.getParams().isEmpty()) {
            JsonObject params = new JsonObject();
            for (Map.Entry<String, String> param : request.getParams().entrySet()) {
                params.put(param.getKey(), param.getValue());
            }
            jsonRequest.put("params", params);
        }
        jsonRequest.put("path", request.getPath());
        return new SecureHttpServerRequest(new JsonHttpServerRequest(jsonRequest, headers));
    }

    /**
     * Gets a session using UserAuthWithQueryParamFilter for authentication
     * This handles OAuth, JWT and cookie authentication
     * 
     * @param secureRequest The SecureHttpServerRequest with auth data
     * @param pathPrefix The path prefix for OAuth scope verification
     * @return A Future with the session response or error
     */
    private Future<FindSessionResponseDTO> getSessionWithFilter(SecureHttpServerRequest secureRequest, String pathPrefix) {
        // Initialize the filter with all required components
        final Optional<Object> oauthCache = Optional.ofNullable(serverConfig.get("oauthCache"));
        final Optional<JsonObject> oauthConfigJson = oauthCache.map(e-> new JsonObject((String)e));
        final Optional<Integer> oauthTtl = oauthConfigJson.map(e -> e.getInteger("ttlSeconds"));
        // Create the filter with the path prefix and OAuth TTL
        // Note: oauthTtl is optional, if not present, it will be set to default value in AppOAuthResourceProvider
        final UserAuthFilter userAuth = new UserAuthWithQueryParamFilter(
            new AppOAuthResourceProvider(
                authManager.getVertx().eventBus(),
                pathPrefix,
                // we dont need to cache "oauth sessions"
                Optional::empty,
                oauthTtl
            ),
            new BasicFilter(),
            new QueryParamTokenFilter(),
            authManager.getVertx(),
            authManager.config()
        );
        
        final Promise<FindSessionResponseDTO> promise = Promise.promise();
        
        // Use the filter to check if the request can access
        userAuth.canAccess(secureRequest, canAccess -> {
            if (Boolean.TRUE.equals(canAccess)) {
                // Authentication successful, get the session
                UserUtils.getUserInfos(authManager.getVertx().eventBus(), secureRequest, userInfos -> {
                    if (userInfos != null) {
                        final SessionDto sessionDto = fromUserInfos(userInfos);
                        promise.complete(new FindSessionResponseDTO(sessionDto));
                    } else {
                        log.warn("Failed to convert session data to UserInfos");
                        promise.fail("session.conversion.error");
                    }
                });
            } else {
                log.debug("Authentication failed with all methods");
                promise.fail("authentication.failed");
            }
        });
        
        return promise.future();
    }

    /**
     * Gets a session directly by its ID
     */
    private Future<FindSessionResponseDTO> getSessionById(String sessionId) {
        log.debug("Getting session with direct sessionId: {}", sessionId);
        final Promise<FindSessionResponseDTO> promise = Promise.promise();
        
        authManager.findBySessionId(sessionId).onComplete(ar -> {
            if (ar.succeeded() && ar.result() != null) {
                try {
                    final UserInfos userInfos = UserUtils.sessionToUserInfos(ar.result());
                    if (userInfos != null) {
                        SessionDto sessionDto = fromUserInfos(userInfos);
                        promise.complete(new FindSessionResponseDTO(sessionDto));
                    } else {
                        promise.fail("invalid.session.data");
                    }
                } catch (Exception e) {
                    log.error("Error processing session data from sessionId", e);
                    promise.fail(e);
                }
            } else {
                promise.fail("session.not.found");
            }
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

        // Convert functions
        final List<UserFunctionDto> functionDtos = new ArrayList<>();
        if (userInfos.getFunctions() != null) {
            for (Map.Entry<String, UserInfos.Function> entry : userInfos.getFunctions().entrySet()) {
                UserInfos.Function function = entry.getValue();
                functionDtos.add(new UserFunctionDto(
                    function.getScope(),
                    function.getCode()
                ));
            }
        }

        // Determine if user is a super admin using UserUtils
        boolean isSuperAdmin = UserUtils.isSuperAdmin(userInfos);

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
                structureDtos,
                functionDtos,
                isSuperAdmin
        );
    }
}