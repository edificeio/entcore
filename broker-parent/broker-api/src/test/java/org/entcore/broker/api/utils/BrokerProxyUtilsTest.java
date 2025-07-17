package org.entcore.broker.api.utils;

import io.vertx.core.json.JsonObject;
import org.entcore.broker.api.BrokerListener;
import org.entcore.broker.api.utils.dummy.*;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BrokerProxyUtilsTest {
  @Test
  public void testGetListenersWithoutAnnotation() {
    assertTrue("A class which does not implement @BrokerListener should not have listeners", BrokerProxyUtils.getListeners(new JsonObject()).isEmpty());
  }
  @Test
  public void testGetListenersWithAnnotatedMethods() {
    final List<BrokerProxyUtils.Listener> listeners = BrokerProxyUtils.getListeners(new Object() {
      @BrokerListener(subject = "simple.test", proxy = true, broadcast = true)
      public void simpleTest() {
      }
      @BrokerListener(subject = "simple.test", proxy = true, broadcast = true)
      public ExampleResponseDTO hello(ExampleRequestDTO request) {
        return null;
      }
      public void notAListener() {
      }
    });
    assertEquals("Should find all listeners and just these functions", 2, listeners.size());
  }
  @Test
  public void testGetListenersWithAnnotatedMethodsInheritedFromDirectInterface() {
    final List<BrokerProxyUtils.Listener> listeners = BrokerProxyUtils.getListeners(new DummyProxy() {
      @Override
      public void simpleTest() {

      }

      @Override
      public ExampleResponseDTO hello(ExampleRequestDTO request) {
        return null;
      }

      @Override
      public void notAListener() {

      }
    });
    assertEquals("Should find all listeners and just these functions", 2, listeners.size());
  }
  @Test
  public void testGetListenersWithAnnotatedMethodsInheritedFromNestedInterface() {
    final List<BrokerProxyUtils.Listener> listeners = BrokerProxyUtils.getListeners(new DummierProxy() {
      @Override
      public void simpleTest() {

      }

      @Override
      public ExampleResponseDTO hello(ExampleRequestDTO request) {
        return null;
      }

      @Override
      public void notAListener() {

      }
    });
    assertEquals("Should find all listeners and just these functions", 2, listeners.size());
  }
  @Test
  public void testGetListenersWithAnnotatedMethodsInheritedFromNestedClass() {
    final List<BrokerProxyUtils.Listener> listeners = BrokerProxyUtils.getListeners(new DummyProxyImpl() {
      @Override
      public void simpleTest() {

      }

      @Override
      public ExampleResponseDTO hello(ExampleRequestDTO request) {
        return null;
      }

      @Override
      public void notAListener() {

      }
    });
    assertEquals("Should find all listeners and just these functions", 2, listeners.size());
  }

}