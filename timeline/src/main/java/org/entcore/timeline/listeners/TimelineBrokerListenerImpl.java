package org.entcore.timeline.listeners;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.shareddata.AsyncMap;
import org.entcore.broker.api.dto.timeline.RegisterNotificationBatchRequestDTO;
import org.entcore.broker.api.dto.timeline.RegisterNotificationRequestDTO;
import org.entcore.broker.api.dto.timeline.RegisterNotificationResponseDTO;
import org.entcore.broker.api.dto.timeline.SendNotificationRequestDTO;
import org.entcore.broker.api.dto.timeline.SendNotificationResponseDTO;
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
      final List<Future> registrationFutures = new ArrayList<>();
      
      for (RegisterNotificationRequestDTO request : requests) {
        final String notificationName = request.getFullNotificationName();
        final JsonObject notification = new JsonObject()
            .put("type", request.getType().toUpperCase())
            .put("event-type", request.getEventType().toUpperCase())
            .put("app-name", request.getType())
            .put("app-address", request.getAppAddress())
            .put("template", request.getTemplate())
            .put("defaultFrequency", request.getDefaultFrequency().name())
            .put("restriction", request.getRestriction().name())
            .put("push-notif", request.isPushNotif());
        
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
      
      // Wait for all registrations to complete
      CompositeFuture.join(registrationFutures).onComplete(ar -> {
        if (ar.succeeded()) {
          promise.complete(new RegisterNotificationResponseDTO("Successfully registered " + requests.size() + " notifications"));
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
      // Create a request object with headers
      final JsonObject httpRequest = new JsonObject();
      final io.vertx.core.MultiMap headers = io.vertx.core.MultiMap.caseInsensitiveMultiMap();
      
      // Add any headers from the request
      if (request.getHeaders() != null && !request.getHeaders().isEmpty()) {
        for (final Map.Entry<String, String> header : request.getHeaders().entrySet()) {
          headers.add(header.getKey(), header.getValue());
        }
      }
      
      // Create request with headers
      final JsonHttpServerRequest jsonRequest = new JsonHttpServerRequest(httpRequest, headers);
      
      // Create sender user info
      final UserInfos sender = new UserInfos();
      sender.setUserId(request.getSenderId());
      sender.setUsername(request.getSenderName());
      
      // Process recipients
      final List<String> recipients = new ArrayList<>(request.getRecipientIds());
      if (recipients.contains(sender.getUserId())) {
        recipients.remove(sender.getUserId());  // Remove sender from recipients to avoid self-notification
      }
      
      if (recipients.isEmpty()) {
        log.warn("No recipients for notification: " + request.getNotificationName());
        return Future.succeededFuture(new SendNotificationResponseDTO(0, null));
      }
      
      // Clone parameters to avoid modification of the original
      final JsonObject params = request.getParams() != null ? request.getParams().copy() : new JsonObject();
      
      // Add resource URI if available
      if (request.getResourceUri() != null) {
        params.put("resourceUri", request.getResourceUri());
      }
      
      // Add publish date if provided
      if (request.getPublishDate() != null) {
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
      
      // Generate a unique notification ID if not provided
      final String notificationId = request.getResourceId() != null ? 
          request.getResourceId() : UUID.randomUUID().toString();

      final Promise<SendNotificationResponseDTO> promise = Promise.promise();
      
      // Send notification using the Future-returning method
      timelineHelper.notifyTimeline(
        jsonRequest,
        request.getNotificationName(),
        sender,
        recipients,
        notificationId,
        request.getSubResourceId(),
        params,
        request.isDisableAntiFlood(),
        request.getPreview()
      ).onSuccess(result -> {
          log.debug("Successfully sent notification: " + request.getNotificationName() + " to " + recipients.size() + " recipients");
          promise.complete(new SendNotificationResponseDTO(recipients.size(), notificationId));
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
}