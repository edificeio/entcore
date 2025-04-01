package org.entcore.broker.proxy;

import org.entcore.broker.api.BrokerListener;
import org.entcore.broker.api.dto.directory.CreateSharesRequestDTO;
import org.entcore.broker.api.dto.directory.CreateSharesResponseDTO;
import org.entcore.broker.api.dto.directory.DeleteSharesRequestDTO;
import org.entcore.broker.api.dto.directory.DeleteSharesResponseDTO;

public interface DirectoryBrokerListener {

  @BrokerListener(subject = "directory.shares.create", proxy = true)
  CreateSharesResponseDTO createShares(CreateSharesRequestDTO request);

  @BrokerListener(subject = "directory.shares.delete", proxy = true)
  DeleteSharesResponseDTO deleteShares(DeleteSharesRequestDTO request);
}
