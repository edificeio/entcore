package org.entcore.timeline.listeners;

import io.vertx.core.*;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.shareddata.AsyncMap;
import org.entcore.broker.api.dto.timeline.*;
import org.entcore.broker.proxy.TimelineBrokerListener;
import org.entcore.common.http.request.JsonHttpServerRequest;
import org.entcore.common.notification.TimelineHelper;
import org.entcore.common.user.UserInfos;
import org.entcore.common.utils.MapFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of TimelineBrokerListener that handles notification registration and sending.
 */
public class TimelineBrokerListenerImpl implements TimelineBrokerListener {

    private static final Logger log = LoggerFactory.getLogger(TimelineBrokerListenerImpl.class);
    private static final String NOTIFICATIONS_MAP = "notificationsMap";

    // Error codes
    private static final String ERR_INVALID_REQUEST = "timeline.notification.register.invalid";
    private static final String ERR_MAP_ACCESS = "timeline.notification.register.map.error";
    private static final String ERR_REGISTRATION = "timeline.notification.register.error";
    private static final String ERR_SEND_INVALID = "timeline.notification.send.invalid";
    private static final String ERR_SEND_ERROR = "timeline.notification.send.error";

    private final Vertx vertx;
    private final TimelineHelper timelineHelper;

    public TimelineBrokerListenerImpl(Vertx vertx) {
        this.vertx = vertx;
        this.timelineHelper = new TimelineHelper(vertx, vertx.eventBus(), new JsonObject());
    }

    @Override
    public Future<RegisterNotificationResponseDTO> registerNotification(RegisterNotificationRequestDTO request) {
        // Create a list with a single notification and use the batch method
        return registerNotifications(new RegisterNotificationBatchRequestDTO(Collections.singletonList(request)));
    }

    @Override
    public Future<RegisterNotificationResponseDTO> registerNotifications(RegisterNotificationBatchRequestDTO batchRequest) {
        // Validate the batch request
        if (batchRequest == null || !batchRequest.isValid()) {
            log.error(ERR_INVALID_REQUEST + ": Batch request is null or empty");
            return Future.failedFuture(ERR_INVALID_REQUEST);
        }

        final List<RegisterNotificationRequestDTO> requests = batchRequest.getNotifications();

        // Validate all individual requests
        final List<String> invalidRequests = requests.stream()
                .filter(req -> !req.isValid())
                .map(req -> req.getType() + "." + req.getEventType())
                .collect(Collectors.toList());

        if (!invalidRequests.isEmpty()) {
            log.error(ERR_INVALID_REQUEST + ": " + String.join(", ", invalidRequests));
            return Future.failedFuture(ERR_INVALID_REQUEST);
        }

        final Promise<RegisterNotificationResponseDTO> promise = Promise.promise();

        MapFactory.getClusterMap(NOTIFICATIONS_MAP, vertx, (final AsyncMap<String, String> notificationsMap) -> {
            if (notificationsMap == null) {
                log.error(ERR_MAP_ACCESS + ": Failed to access notifications map");
                promise.fail(ERR_MAP_ACCESS);
                return;
            }

            // Process each notification
            final List<Future> registrationFutures = registerNotificationsInMap(requests, notificationsMap);
            // Wait for all registrations to complete
            CompositeFuture.join(registrationFutures).onComplete(ar -> {
                if (ar.succeeded()) {
                    promise.complete(new RegisterNotificationResponseDTO(requests.size()));
                } else {
                    log.error(ERR_REGISTRATION + ": " + ar.cause().getMessage());
                    promise.fail(ERR_REGISTRATION);
                }
            });
        });

        return promise.future();
    }

    @Override
    public Future<SendNotificationResponseDTO> sendNotification(final SendNotificationRequestDTO request) {
        if (!request.isValid()) {
            log.error(ERR_SEND_INVALID + ": Missing required fields");
            return Future.failedFuture(ERR_SEND_INVALID);
        }

        try {
            // Prepare request with HTTP headers
            final JsonHttpServerRequest jsonRequest = createHttpRequest(request.getHeaders());

            // Create sender user info
            final UserInfos sender = createSender(request.getSenderId(), request.getSenderName());

            // Process recipients
            final List<String> recipients = processRecipients(request.getRecipientIds(), sender.getUserId());
            if (recipients.isEmpty()) {
                log.warn("No recipients for notification: " + request.getNotificationName());
                return Future.succeededFuture(new SendNotificationResponseDTO(0));
            }

            // Prepare notification parameters
            final JsonObject params = prepareNotificationParams(request);

            // Convert NotificationPreviewDTO to JsonObject if provided
            final JsonObject previewJson = request.getPreview() != null ? request.getPreview().toJson() : null;

            final Promise<SendNotificationResponseDTO> promise = Promise.promise();

            // Send notification using the Future-returning method
            timelineHelper.notifyTimeline(
                    jsonRequest,
                    request.getNotificationName(),
                    sender,
                    recipients,
                    request.getResourceId(),
                    request.getSubResourceId(),
                    params,
                    request.isDisableAntiFlood(),
                    previewJson
            ).onSuccess(result -> {
                log.debug("Successfully sent notification: " + request.getNotificationName() + " to " + recipients.size() + " recipients");
                promise.complete(new SendNotificationResponseDTO(recipients.size()));
            }).onFailure(err -> {
                log.error(ERR_SEND_ERROR + ": " + err.getMessage(), err.getCause());
                promise.fail(ERR_SEND_ERROR);
            });

            return promise.future();
        } catch (Exception e) {
            log.error(ERR_SEND_ERROR + ": " + e.getMessage(), e);
            return Future.failedFuture(ERR_SEND_ERROR);
        }
    }

    /**
     * Registers individual notifications in the provided map
     *
     * @param requests         List of notification requests to register
     * @param notificationsMap Map to store notifications in
     * @return List of futures representing individual registration operations
     */
    private List<Future> registerNotificationsInMap(List<RegisterNotificationRequestDTO> requests,
                                                    AsyncMap<String, String> notificationsMap) {
        final List<Future> registrationFutures = new ArrayList<>();
        for (RegisterNotificationRequestDTO request : requests) {
            final String notificationName = request.getFullNotificationName();
            final JsonObject notification = createNotificationObject(request);

            final Promise<Void> registrationPromise = Promise.promise();
            registrationFutures.add(registrationPromise.future());

            notificationsMap.put(notificationName, notification.encode(), ar -> {
                if (ar.succeeded()) {
                    log.info("Successfully registered notification: " + notificationName);
                    registrationPromise.complete();
                } else {
                    log.error(ERR_REGISTRATION + ": " + notificationName + " - " + ar.cause().getMessage());
                    registrationPromise.fail(ERR_REGISTRATION);
                }
            });
        }
        return registrationFutures;
    }

    /**
     * Creates a notification object from a request DTO
     *
     * @param request Notification request
     * @return JsonObject containing notification configuration
     */
    private JsonObject createNotificationObject(RegisterNotificationRequestDTO request) {
        return new JsonObject()
                .put("type", request.getType().toUpperCase())
                .put("event-type", request.getEventType().toUpperCase())
                .put("app-name", request.getType())
                .put("app-address", request.getAppAddress())
                .put("template", request.getTemplate())
                .put("defaultFrequency", request.getDefaultFrequency().name())
                .put("restriction", request.getRestriction().name())
                .put("push-notif", request.isPushNotif());
    }

    /**
     * Prepares notification parameters from the request
     *
     * @param request Notification request
     * @return JsonObject containing notification parameters
     */
    private JsonObject prepareNotificationParams(final SendNotificationRequestDTO request) {
        final JsonObject params = new JsonObject();

        // Add all parameters from the request
        if (request.getParams() != null) {
            for (final Map.Entry<String, String> param : request.getParams().entrySet()) {
                params.put(param.getKey(), param.getValue());
            }
        }

        // Add resourceName if available
        if (request.getResourceName() != null && !request.getResourceName().isEmpty()) {
            params.put("resourceName", request.getResourceName());
        }

        // Add resource URI if available
        if (request.getResourceUri() != null) {
            params.put("resourceUri", request.getResourceUri());
        }

        // Add publish date if provided
        if (request.getPublishDate() != -1l) {
            params.put("timeline-publish-date", request.getPublishDate());
        }

        // Add push notification parameters if available
        if (request.getPushNotif() != null) {
            params.put("pushNotif", request.getPushNotif().toJson());
        }

        // Add mail notification flag if needed
        if (request.isDisableMailNotification()) {
            params.put("disableMailNotification", true);
        }

        // Add sender ID if available
        if(request.getSenderId() != null) {
            params.put("senderId", request.getSenderId());
        }

        // Add sender name if available
        if(request.getSenderName() != null) {
            params.put("senderName", request.getSenderName());
        }

        return params;
    }

    /**
     * Creates an HTTP request object with the specified headers
     *
     * @param headerMap Map of HTTP headers to include in the request
     * @return A JsonHttpServerRequest with the specified headers
     */
    private JsonHttpServerRequest createHttpRequest(final Map<String, String> headerMap) {
        final JsonObject httpRequest = new JsonObject();
        final MultiMap headers = MultiMap.caseInsensitiveMultiMap();

        if (headerMap != null && !headerMap.isEmpty()) {
            for (final Map.Entry<String, String> header : headerMap.entrySet()) {
                headers.add(header.getKey(), header.getValue());
            }
        }

        return new JsonHttpServerRequest(httpRequest, headers);
    }

    /**
     * Creates a UserInfos object representing the sender
     *
     * @param userId   ID of the sender
     * @param username Username of the sender
     * @return UserInfos object for the sender
     */
    private UserInfos createSender(String userId, String username) {
        final UserInfos sender = new UserInfos();
        sender.setUserId(userId);
        sender.setUsername(username);
        return sender;
    }

    /**
     * Processes the list of recipient IDs, removing the sender if present
     *
     * @param recipientIds List of recipient IDs
     * @param senderId     ID of the sender
     * @return Processed list of recipients
     */
    private List<String> processRecipients(List<String> recipientIds, String senderId) {
        if (recipientIds == null) {
            return Collections.emptyList();
        }

        final List<String> recipients = new ArrayList<>(recipientIds);
        recipients.remove(senderId); // Remove sender to avoid self-notification
        return recipients;
    }
}