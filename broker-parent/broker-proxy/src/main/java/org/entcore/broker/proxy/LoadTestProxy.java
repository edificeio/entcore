package org.entcore.broker.proxy;

import io.vertx.core.Future;
import org.entcore.broker.api.BrokerListener;
import org.entcore.broker.api.dto.loadtest.LoadTestRequestDTO;
import org.entcore.broker.api.dto.loadtest.LoadTestResponseDTO;


public interface LoadTestProxy {
  @BrokerListener(subject = "ent.loadtest", proxy = true, description = "Subject used for load testing purposes")
  Future<LoadTestResponseDTO> loadTest(final LoadTestRequestDTO request);
}
