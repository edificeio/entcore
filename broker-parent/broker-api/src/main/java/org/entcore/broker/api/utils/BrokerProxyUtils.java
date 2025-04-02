package org.entcore.broker.api.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.broker.api.BrokerListener;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

public class BrokerProxyUtils {

  private static final Logger log = LoggerFactory.getLogger(BrokerProxyUtils.class);

  private static final ObjectMapper mapper = new ObjectMapper();

  /**
   * Starts listening on the event bus for every request specified by the
   * annotation @BrokerListener
   * on the proxified class.
   * 
   * @param proxyImpl Instance of events listener.
   * @param vertx     Current vertx instance.
   * @return A function to call to stop listening.
   */
  public static Callable<Void> addBrokerProxy(Object proxyImpl, final Vertx vertx) {
    final List<Listener> listeners = getListeners(proxyImpl);
    final EventBus eb = vertx.eventBus();
    final List<MessageConsumer<?>> consumers = new ArrayList<>();
    for (Listener listener : listeners) {
      consumers.add(startListening(listener, proxyImpl, eb));
    }
    return () -> {
      for (MessageConsumer<?> consumer : consumers) {
        log.info("Unregistering event bus consumer at address " + consumer.address());
        consumer.unregister();
      }
      return null;
    };
  }

  private static MessageConsumer<?> startListening(Listener listener, Object proxyImpl, EventBus eb) {
    final BrokerListener annotation = listener.annotation;
    final Method method = listener.method;
    final String address = annotation.subject();
    final Class<?> requestType = method.getParameterTypes()[0];
    log.info("Start listening on address " + address);
    return eb.consumer(address, (Handler<Message<byte[]>>) message -> {
      log.debug("Received message on address " + address);
      Object response;
      if (method.getParameterCount() == 1) {
        final byte[] rawRequest = message.body();
        Object request = null;
        try {
          request = mapper.readValue(rawRequest, requestType);
        } catch (IOException e) {
        }
        if (request == null) {
          log.error("Error while deserializing request: " + rawRequest);
          response = new IllegalArgumentException("Invalid request: " + rawRequest);
        } else {
          log.debug("Calling method " + method.getName() + " with request: " + rawRequest);
          try {
            response = method.invoke(proxyImpl, request);
          } catch (Exception e) {
            log.error("Error while invoking method " + method.getName(), e);
            response = e;
          }
        }
      } else {
        log.error("Method " + method.getName() + " has an invalid number of parameters: " + method.getParameterCount());
        response = new IllegalStateException("Subject listener for " + address + " invalid");
      }
      sendResponse(response, message, address);
    });
  }

  private static void sendResponse(Object response, Message<byte[]> message, String address) {
    if (response instanceof Exception) {
      log.error("Error while processing message on address " + address, (Exception) response);
      message.fail(getErrorCodeForException((Exception) response), ((Exception) response).getMessage());
    } else if (response instanceof Future) {
      log.debug("Waiting for async treatment...");
      ((Future<?>) response).onSuccess(finalResponse -> {
        sendSuccess(finalResponse, message);
      }).onFailure(err -> {
        log.error("Error while processing message on address " + address, err);
        message.fail(500, err.getMessage());
      });
    } else {
      sendSuccess(response, message);
    }
  }

  private static int getErrorCodeForException(Exception response) {
    if (response instanceof IllegalArgumentException) {
      return 400;
    }
    return 500;
  }

  private static void sendSuccess(Object response, Message<byte[]> message) {
    try {
      message.reply(mapper.writeValueAsString(response));
    } catch (Exception e) {
      log.error("Error while serializing response produced by consumer", e);
      message.fail(500, e.getMessage());
    }
  }

  public static List<Listener> getListeners(Object proxyImpl) {
    final List<Listener> methods = new ArrayList<>();
    for (Method method : proxyImpl.getClass().getMethods()) {
      if (Modifier.isPublic(method.getModifiers())) {
        final Optional<BrokerListener> maybeAnnotation = getBrokerListenerAnnotation(proxyImpl, method);
        maybeAnnotation.ifPresent(annotation -> {
          if (annotation.proxy()) {
            log.debug("Adding method " + method.getName() + " as a listener for subject " + annotation.subject());
            methods.add(new Listener(method, annotation));
          } else {
            log.debug("Skipping method " + method.getName() + " as a listener for subject " + annotation.subject()
              + " because it is not a proxy");
          }
        });
      }
    }
    return methods;
  }

  public static Optional<BrokerListener> getBrokerListenerAnnotation(final Object proxyImpl, final Method method) {
    BrokerListener annotation = method.getAnnotation(BrokerListener.class);
    if (annotation == null) {
      // Check if the annotation is present on the interface method
      for (Class<?> iface : proxyImpl.getClass().getInterfaces()) {
        try {
          Method interfaceMethod = iface.getMethod(method.getName(), method.getParameterTypes());
          if (interfaceMethod != null) {
            annotation = interfaceMethod.getAnnotation(BrokerListener.class);
            if (annotation != null) {
              break;
            }
          }
        } catch (NoSuchMethodException e) {
          // Method not found in this interface, continue with next interface
        }
      }
    }
    return Optional.ofNullable(annotation);
  }

  public static class Listener {
    private final Method method;
    private final BrokerListener annotation;

    public Listener(Method method, BrokerListener annotation) {
      this.method = method;
      this.annotation = annotation;
    }
  }
}
