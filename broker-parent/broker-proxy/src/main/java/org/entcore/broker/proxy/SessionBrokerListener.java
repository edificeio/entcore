package org.entcore.broker.proxy;

import io.vertx.core.Future;
import org.entcore.broker.api.BrokerListener;
import org.entcore.broker.api.dto.session.FindSessionRequestDTO;
import org.entcore.broker.api.dto.session.FindSessionResponseDTO;


/**
 * This interface defines the methods that will be used to listen to events from the session broker.
 */
public interface SessionBrokerListener {

  /**
   * This method is called when a session is requested.
   * It takes a FindSessionRequestDTO object as input and returns a FindSessionResponseDTO object.
   *
   * @param request The request object containing the session ID to be found.
   * @return The response object containing the session details.
   * @throw Exception If an error occurs while processing the request or session is not found.
   */
  @BrokerListener(subject = "session.find", proxy = true)
  Future<FindSessionResponseDTO> findSession(FindSessionRequestDTO request);
}
