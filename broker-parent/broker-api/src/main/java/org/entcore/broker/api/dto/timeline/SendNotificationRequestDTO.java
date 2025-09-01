package org.entcore.broker.api.dto.timeline;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.beans.Transient;
import java.util.Collections;
import java.util.HashMap;
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
     * Resource name to be displayed in the notification
     */
    private final String resourceName;

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
    private final long publishDate;

    /**
     * Parameters to be used for template rendering and notification display
     * Replaced JsonObject with Map<String, String> for simpler usage
     */
    private final Map<String, String> params;

    /**
     * Optional parameters specific to push notifications
     */
    private final PushNotifParamsDTO pushNotif;

    /**
     * Preview information for notification displays
     * Replaced JsonObject with NotificationPreviewDTO
     */
    private final NotificationPreviewDTO preview;

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
            @JsonProperty("resourceName") String resourceName,
            @JsonProperty("subResourceId") String subResourceId,
            @JsonProperty("resourceUri") String resourceUri,
            @JsonProperty("publishDate") Long publishDate,
            @JsonProperty("params") Map<String, String> params,
            @JsonProperty("pushNotif") PushNotifParamsDTO pushNotif,
            @JsonProperty("preview") NotificationPreviewDTO preview,
            @JsonProperty("headers") Map<String, String> headers,
            @JsonProperty("disableMailNotification") boolean disableMailNotification,
            @JsonProperty("disableAntiFlood") boolean disableAntiFlood) {

        this.notificationName = notificationName;
        this.senderId = senderId;
        this.senderName = senderName;
        this.recipientIds = recipientIds;
        this.resourceId = resourceId;
        this.resourceName = resourceName;
        this.subResourceId = subResourceId;
        this.resourceUri = resourceUri;
        this.publishDate = publishDate == null ? -1l : publishDate;
        this.params = params != null ? params : new HashMap<>();
        this.pushNotif = pushNotif;
        this.preview = preview;
        this.headers = headers != null ? headers : Collections.emptyMap();
        this.disableMailNotification = disableMailNotification;
        this.disableAntiFlood = disableAntiFlood;
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

    public String getResourceName() {
        return resourceName;
    }

    public String getSubResourceId() {
        return subResourceId;
    }

    public String getResourceUri() {
        return resourceUri;
    }

    public long getPublishDate() {
        return publishDate;
    }

    public Map<String, String> getParams() {
        return params;
    }

    public PushNotifParamsDTO getPushNotif() {
        return pushNotif;
    }

    public NotificationPreviewDTO getPreview() {
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

}