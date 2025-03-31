package org.entcore.broker.proxy;

import io.vertx.core.Future;
import org.entcore.broker.api.BrokerListener;
import org.entcore.broker.api.dto.resources.GetResourcesRequestDTO;
import org.entcore.broker.api.dto.resources.GetResourcesResponseDTO;

/**
 * This interface defines the methods that will be used to listen to events from the resource broker.
 * It provides functionality to retrieve detailed information about resources across different applications.
 */
public interface ResourceBrokerListener {
  /**
   * Retrieves detailed information about multiple resources identified by their IDs/UUIDs.
   * This method allows applications to fetch resource metadata without direct access to the resource storage.
   * 
   * @param request The request object containing the list of resource identifiers to retrieve information for.
   * @return A response object containing the details of the requested resources including title, description,
   *         thumbnail URL, author information, and timestamps.
   */
  @BrokerListener(subject = "resource.get.{application}", proxy = true)
  Future<GetResourcesResponseDTO> getResources(GetResourcesRequestDTO request);
}