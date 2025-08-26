package org.entcore.broker.api.dto.timeline;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.vertx.core.json.JsonObject;

import java.beans.Transient;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Data Transfer Object for sending a notification through the Timeline system.
 */
public class SendNotificationRequestDTO {
  
  /**
   * The notification name (type.event-type)
   */
  private final String notificationName;
  
  /**
   * User ID of the sender
   */
  private final String senderId;
  
  /**
   * Username of the sender
   */
  private final String senderName;
  
  /**
   * List of recipient IDs (user or group IDs)
   */
  private final List<String> recipientIds;
  
  /**
   * Resource ID associated with this notification
   */
  private final String resourceId;
  
  /**
   * Sub-resource ID if applicable
   */
  private final String subResourceId;
  
  /**
   * URI to the resource (used for linking in notifications)
   */
  private final String resourceUri;
  
  /**
   * Optional publishing date for the notification
   */
  private final Long publishDate;
  
  /**
   * Parameters to be used for template rendering and notification display
   */
  private final JsonObject params;
  
  /**
   * Optional parameters specific to push notifications
   */
  private final PushNotifParamsDTO pushNotif;
  
  /**
   * Preview information for notification displays
   */
  private final JsonObject preview;
  
  /**
   * HTTP headers from the original request.
   * These can be used to provide context such as language preferences, host information,
   * user agent details, etc. to the notification system.
   * May be empty if not provided.
   */
  private final Map<String, String> headers;
  
  /**
   * Whether to disable sending mail notifications
   */
  private final boolean disableMailNotification;
  
  /**
   * Whether to disable anti-flood protection
   */
  private final boolean disableAntiFlood;
  
  @JsonCreator
  public SendNotificationRequestDTO(
      @JsonProperty("notificationName") String notificationName,
      @JsonProperty("senderId") String senderId,
      @JsonProperty("senderName") String senderName,
      @JsonProperty("recipientIds") List<String> recipientIds,
      @JsonProperty("resourceId") String resourceId,
      @JsonProperty("subResourceId") String subResourceId,
      @JsonProperty("resourceUri") String resourceUri,
      @JsonProperty("publishDate") Long publishDate,
      @JsonProperty("params") JsonObject params,
      @JsonProperty("pushNotif") PushNotifParamsDTO pushNotif,
      @JsonProperty("preview") JsonObject preview,
      @JsonProperty("headers") Map<String, String> headers,
      @JsonProperty("disableMailNotification") Boolean disableMailNotification,
      @JsonProperty("disableAntiFlood") Boolean disableAntiFlood) {
    
    this.notificationName = notificationName;
    this.senderId = senderId;
    this.senderName = senderName;
    this.recipientIds = recipientIds;
    this.resourceId = resourceId;
    this.subResourceId = subResourceId;
    this.resourceUri = resourceUri;
    this.publishDate = publishDate;
    this.params = params != null ? params : new JsonObject();
    this.pushNotif = pushNotif;
    this.preview = preview;
    this.headers = headers != null ? headers : Collections.emptyMap();
    this.disableMailNotification = disableMailNotification != null ? disableMailNotification : false;
    this.disableAntiFlood = disableAntiFlood != null ? disableAntiFlood : false;
  }
  
  public String getNotificationName() {
    return notificationName;
  }
  
  public String getSenderId() {
    return senderId;
  }
  
  public String getSenderName() {
    return senderName;
  }
  
  public List<String> getRecipientIds() {
    return recipientIds;
  }
  
  public String getResourceId() {
    return resourceId;
  }
  
  public String getSubResourceId() {
    return subResourceId;
  }
  
  public String getResourceUri() {
    return resourceUri;
  }
  
  public Long getPublishDate() {
    return publishDate;
  }
  
  public JsonObject getParams() {
    return params;
  }
  
  public PushNotifParamsDTO getPushNotif() {
    return pushNotif;
  }
  
  public JsonObject getPreview() {
    return preview;
  }
  
  public boolean isDisableMailNotification() {
    return disableMailNotification;
  }
  
  public boolean isDisableAntiFlood() {
    return disableAntiFlood;
  }
  
  /**
   * Gets the HTTP headers from the original request.
   * These headers can provide context for notification generation and delivery,
   * such as language preferences, host information, etc.
   *
   * @return Map of HTTP header names to values. Never null, but may be empty.
   */
  public Map<String, String> getHeaders() {
    return headers;
  }
  
  @Transient
  public boolean isValid() {
    return notificationName != null && 
           recipientIds != null && 
           !recipientIds.isEmpty();
  }
  
  /**
   * DTO for push notification specific parameters
   */
  public static class PushNotifParamsDTO {
    private final String title;
    private final String body;
    
    @JsonCreator
    public PushNotifParamsDTO(
        @JsonProperty("title") String title,
        @JsonProperty("body") String body) {
      this.title = title;
      this.body = body;
    }
    
    public String getTitle() {
      return title;
    }
    
    public String getBody() {
      return body;
    }
    
    public JsonObject toJson() {
      return new JsonObject()
          .put("title", title)
          .put("body", body);
    }
  }
}