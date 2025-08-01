package org.entcore.broker.proxy;

import io.vertx.core.Future;
import org.entcore.broker.api.BrokerListener;
import org.entcore.broker.api.dto.session.FindSessionRequestDTO;
import org.entcore.broker.api.dto.session.FindSessionResponseDTO;
import org.entcore.broker.api.dto.session.SessionRecreationRequestDTO;
import org.entcore.broker.api.dto.session.SessionRecreationResponseDTO;


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

  /**
   * This method is called to recreate a session based on the provided request.
   * It takes a SessionRecreationRequestDTO object as input and returns a SessionRecreationResponseDTO object.
   *
   * @param request The request object containing the session ID or cookies for session recreation.
   * @return The response object containing the recreated session details.
   */
  @BrokerListener(subject = "session.recreate", proxy = true)
  Future<SessionRecreationResponseDTO> recreateSession(SessionRecreationRequestDTO request);
}
