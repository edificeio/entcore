package org.entcore.broker.api.utils.dummy;

public class DummyProxyImpl implements DummierProxy {
  @Override
  public void simpleTest() {

  }

  @Override
  public ExempleResponseDTO hello(ExempleRequestDTO request) {
    return null;
  }

  @Override
  public void notAListener() {

  }
}
