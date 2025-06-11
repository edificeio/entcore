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
import org.apache.commons.lang3.StringUtils;
import org.entcore.broker.api.BrokerListener;
import org.entcore.broker.api.dto.NATSResponseDTO;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.Callable;

public class BrokerProxyUtils {

  private static final Logger log = LoggerFactory.getLogger(BrokerProxyUtils.class);

  private static final ObjectMapper mapper = new ObjectMapper();

  /**
   * Starts listening on the event bus for every request specified by the
   * annotation @BrokerListener on the proxified class.
   * 
   * @param proxyImpl Instance of events listener.
   * @param vertx     Current vertx instance.
   * @param addressParameters List of pairs of  to be used in the address. For instance, if the proxy declared the routes
   *                          {@code share.group.upsert.{application}.{subdomain}} and {@code export.{application}.{subresource}}
   *                          then the {@code addressParameters} should contain exactly 3 parameters, each one containing
   *                          the value for each parameter : {@code application} {@code subdomain} and {@code subresource}.<br />
   *                          <u>Example</u>
   *                          {@code addBrokerProxy(new BlogBrokerListener(), vertx, new AddressParameter("application", "blog"), new AddressParameter("subdomain", "shares"), new new AddressParameter("subresource")}</br>
   *                          Which will be listening on the address {@code share.group.upsert.blog.shares}<br />
   *                          If you do not provide the expected number of parameters then an exception will be raised.
   * @return A function to call to stop listening.
   */
  public static Callable<Void> addBrokerProxy(Object proxyImpl, final Vertx vertx, final AddressParameter... addressParameters) {
    final Map<String, String> params = getParameters(addressParameters);
    final List<Listener> listeners = getListeners(proxyImpl);
    checkParameters(params, proxyImpl, listeners);
    final EventBus eb = vertx.eventBus();
    final List<MessageConsumer<?>> consumers = new ArrayList<>();
    for (Listener listener : listeners) {
      consumers.add(startListening(listener, proxyImpl, eb, params));
    }
    return () -> {
      for (MessageConsumer<?> consumer : consumers) {
        log.info("Unregistering event bus consumer at address " + consumer.address());
        consumer.unregister();
      }
      return null;
    };
  }

  private static void checkParameters(final Map<String, String> params, final Object proxyImpl, final List<Listener> listeners) {
    final Set<String> listenerParameters = getListenerParameters(listeners);
    final Set<String> missingParameters = new HashSet<>(listenerParameters);
    for (String parameter : listenerParameters) {
      if (!params.containsKey(parameter) || StringUtils.isBlank(params.get(parameter))) {
        missingParameters.add(parameter);
      } else {
        missingParameters.remove(parameter);
      }
    }
    if(!missingParameters.isEmpty()) {
      final StringBuilder sb = new StringBuilder();
      sb.append("Missing parameters for listener on class ").append(proxyImpl.getClass().getName()).append(": ");
      for (String parameter : missingParameters) {
        sb.append(parameter).append(", ");
      }
      sb.delete(sb.length() - 2, sb.length());
      throw new IllegalArgumentException(sb.toString());
    }
  }

  private static Set<String> getListenerParameters(List<Listener> listeners) {
    final Set<String> listenerParameters = new HashSet<>();
    for (Listener listener : listeners) {
      final BrokerListener annotation = listener.annotation;
      if (annotation != null) {
        final String[] parameters = annotation.subject().split("\\.");
        for (String parameter : parameters) {
          if (parameter.startsWith("{") && parameter.endsWith("}")) {
            listenerParameters.add(parameter.substring(1, parameter.length() - 1));
          }
        }
      }
    }
    return listenerParameters;
  }

  private static Map<String, String> getParameters(AddressParameter[] addressParameters) {
    final Map<String, String> params;
    if(addressParameters == null) {
      params = Collections.emptyMap();
    } else {
      params = new HashMap<>();
      for (AddressParameter addressParameter : addressParameters) {
        params.put(addressParameter.getName(), addressParameter.getValue());
      }
    }
    return params;
  }

  private static MessageConsumer<?> startListening(Listener listener, Object proxyImpl, EventBus eb, Map<String, String> params) {
    final BrokerListener annotation = listener.annotation;
    final Method method = listener.method;
    final String address = replacePlaceHoldersInListeningAddress(annotation.subject(), params);
    final Class<?> requestType = method.getParameterTypes()[0];
    log.info("Start listening on address " + address);
    return eb.consumer(address, (Handler<Message<byte[]>>) message -> {
      log.debug("Received message on address " + address);
      Object response = null;
      if (method.getParameterCount() == 1) {
        final byte[] rawRequest = message.body();
        Object request = null;
        try {
          request = mapper.readValue(rawRequest, requestType);
        } catch (IOException e) {
          log.error("Error while deserializing request" , e);
          response = e;
        }
        if (request == null) {
          log.error("Error while deserializing request : " + new String(rawRequest));
          response = response == null ? new IllegalArgumentException("Invalid request") : response;
        } else {
          log.debug("Calling method " + method.getName() + " with request: " + new String(rawRequest));
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

  private static String replacePlaceHoldersInListeningAddress(final String subject, final Map<String, String> params) {
    String address = subject;
    for (Map.Entry<String, String> entry : params.entrySet()) {
      final String key = entry.getKey();
      final String value = entry.getValue();
      address = address.replace("{" + key + "}", value);
    }
    return address;
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
      message.reply(mapper.writeValueAsString(new NATSResponseDTO(response, null, false, null)));
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
