package org.entcore.broker.api.utils.dummy;

import org.entcore.broker.api.BrokerListener;

public interface DummyProxy {
  @BrokerListener(subject = "simple.test", proxy = true, broadcast = true)
  void simpleTest();

  @BrokerListener(subject = "simple.test", proxy = true, broadcast = true)
  ExempleResponseDTO hello(ExempleRequestDTO request);

  void notAListener();
}
