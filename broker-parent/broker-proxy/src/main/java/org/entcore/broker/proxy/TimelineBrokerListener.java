package org.entcore.broker.proxy;

import io.vertx.core.Future;
import org.entcore.broker.api.BrokerListener;
import org.entcore.broker.api.dto.timeline.RegisterNotificationBatchRequestDTO;
import org.entcore.broker.api.dto.timeline.RegisterNotificationRequestDTO;
import org.entcore.broker.api.dto.timeline.RegisterNotificationResponseDTO;
import org.entcore.broker.api.dto.timeline.SendNotificationRequestDTO;
import org.entcore.broker.api.dto.timeline.SendNotificationResponseDTO;

/**
 * Broker listener interface for Timeline services.
 * This interface defines methods to register and send notifications through the Timeline broker.
 */
public interface TimelineBrokerListener {
  
  /**
   * Registers a new notification template in the Timeline system.
   * The notification will be stored in the notificationsMap and be available for use.
   * 
   * @param request Request object containing notification template details
   * @return Response indicating success or failure of the registration
   */
  @BrokerListener(subject = "timeline.notification.register", proxy = true)
  Future<RegisterNotificationResponseDTO> registerNotification(RegisterNotificationRequestDTO request);
  
  /**
   * Registers multiple notification templates in the Timeline system.
   * The notifications will be stored in the notificationsMap and be available for use.
   * 
   * @param request Request containing a list of notification templates to register
   * @return Response indicating success or failure of the registrations
   */
  @BrokerListener(subject = "timeline.notification.register.batch", proxy = true)
  Future<RegisterNotificationResponseDTO> registerNotifications(RegisterNotificationBatchRequestDTO request);
  
  /**
   * Sends a notification to specified recipients.
   * The notification can be sent as a timeline notification, email, and/or push notification.
   * 
   * @param request Request object containing notification details and recipients
   * @return Response indicating delivery status of the notification
   */
  @BrokerListener(subject = "timeline.notification.send", proxy = true)
  Future<SendNotificationResponseDTO> sendNotification(SendNotificationRequestDTO request);
}