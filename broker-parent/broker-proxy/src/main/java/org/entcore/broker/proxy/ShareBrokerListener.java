package org.entcore.broker.proxy;

import io.vertx.core.Future;
import org.entcore.broker.api.BrokerListener;
import org.entcore.broker.api.dto.shares.RemoveGroupSharesRequestDTO;
import org.entcore.broker.api.dto.shares.RemoveGroupSharesResponseDTO;
import org.entcore.broker.api.dto.shares.UpsertGroupSharesRequestDTO;
import org.entcore.broker.api.dto.shares.UpsertGroupSharesResponseDTO;
/**
 * This interface defines the methods that will be used to listen to events from the share broker.
 */
public interface ShareBrokerListener {
  //TODO allow dynamic routes using constructor params in impl?
  /**
   * This method is used to upsert shares of a group.
   * @param request The request object containing the details of the shares to be upserted.
   * @return A response object containing the details of the upserted shares.
   */
  @BrokerListener(subject = "share.group.upsert.{application}", proxy = true)
  Future<UpsertGroupSharesResponseDTO> upsertGroupShares(UpsertGroupSharesRequestDTO request);

  /**
   * This method is used to remove shares of a group.
   * @param request The request object containing the details of the shares to be removed.
   * @return A response object indicating the result of the removal operation.
   */
  @BrokerListener(subject = "share.group.remove.{application}", proxy = true)
  Future<RemoveGroupSharesResponseDTO> removeGroupShares(RemoveGroupSharesRequestDTO request);
}
