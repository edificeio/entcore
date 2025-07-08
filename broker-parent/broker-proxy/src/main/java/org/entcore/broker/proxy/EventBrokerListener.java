package org.entcore.broker.proxy;

import io.vertx.core.Future;
import org.entcore.broker.api.BrokerListener;
import org.entcore.broker.api.dto.event.CreateEventRequestDTO;
import org.entcore.broker.api.dto.event.CreateEventResponseDTO;

/**
 * Interface defining the methods to listen to events from the event broker.
 * This interface allows storing events via the broker messaging system.
 */
public interface EventBrokerListener {

  /**
   * Creates and stores an event in the event store.
   * This method can be called remotely via the broker messaging system.
   *
   * @param request The request object containing event details (type, user info, custom attributes)
   * @return A Future containing the response with the event ID
   */
  @BrokerListener(subject = "event.store", proxy = true)
  Future<CreateEventResponseDTO> createAndStoreEvent(CreateEventRequestDTO request);
}