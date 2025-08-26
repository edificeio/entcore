package org.entcore.broker.api.dto.timeline;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.apache.commons.lang3.StringUtils;
import java.beans.Transient;

/**
 * Data Transfer Object for registering a notification template in the Timeline system.
 */
public class RegisterNotificationRequestDTO {
  public enum Frequencies {
    NEVER,
    IMMEDIATE,
    DAILY,
    WEEKLY
  }
  public enum Restrictions {
    INTERNAL,
    EXTERNAL,
    NONE,
    HIDDEN
  }
  /**
   * The application type for this notification
   */
  private final String type;
  
  /**
   * The event type identifier for this notification
   */
  private final String eventType;
  
  /**
   * The HTML template content for this notification
   */
  private final String template;
  
  /**
   * URL path to access the application
   */
  private final String appAddress;
  
  /**
   * Default frequency for this notification (IMMEDIATE, DAILY, WEEKLY, NEVER)
   */
  private final Frequencies defaultFrequency;
  
  /**
   * Restriction level for this notification (NONE, INTERNAL, EXTERNAL, HIDDEN)
   */
  private final Restrictions restriction;
  
  /**
   * Whether this notification should also be sent as a push notification
   */
  private final boolean pushNotif;
  
  @JsonCreator
  public RegisterNotificationRequestDTO(
      @JsonProperty("type") String type,
      @JsonProperty("eventType") String eventType,
      @JsonProperty("template") String template,
      @JsonProperty("appAddress") String appAddress,
      @JsonProperty("defaultFrequency") Frequencies defaultFrequency,
      @JsonProperty("restriction") Restrictions restriction,
      @JsonProperty("pushNotif") Boolean pushNotif) {
    
    this.type = type;
    this.eventType = eventType;
    this.template = template;
    this.appAddress = (appAddress != null) ? appAddress : "/";
    this.defaultFrequency = (defaultFrequency != null) ? defaultFrequency : Frequencies.WEEKLY;
    this.restriction = (restriction != null) ? restriction : Restrictions.NONE;
    this.pushNotif = (pushNotif != null) ? pushNotif : false;
  }
  
  public String getType() {
    return type;
  }
  
  public String getEventType() {
    return eventType;
  }
  
  public String getTemplate() {
    return template;
  }
  
  public String getAppAddress() {
    return appAddress;
  }
  
  public Frequencies getDefaultFrequency() {
    return defaultFrequency;
  }
  
  public Restrictions getRestriction() {
    return restriction;
  }
  
  public boolean isPushNotif() {
    return pushNotif;
  }
  
  /**
   * Gets the full notification name by combining type and event-type.
   * @return The full notification name in lowercase
   */
  @Transient
  public String getFullNotificationName() {
    return (type + "." + eventType).toLowerCase();
  }
  
  @Transient
  public boolean isValid() {
    return !StringUtils.isEmpty(type) && 
           !StringUtils.isEmpty(eventType) && 
           !StringUtils.isEmpty(template);
  }
}