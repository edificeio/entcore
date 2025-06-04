package org.entcore.broker.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Message;
import io.nats.vertx.NatsClient;
import io.nats.vertx.NatsOptions;
import io.vertx.codegen.annotations.Nullable;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.broker.api.dto.BaseResponseDTO;
import org.entcore.broker.listener.BrokerListener;
import org.entcore.broker.nats.model.NATSContract;
import org.entcore.broker.nats.model.NATSEndpoint;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.apache.commons.lang3.StringUtils.isBlank;

public class NATSBrokerClient implements BrokerClient {

  private static final Logger log = LoggerFactory.getLogger(NATSBrokerClient.class);

  private final NatsClient natsClient;
  private final ObjectMapper mapper = new ObjectMapper();
  private final String charset = "UTF-8";

  private final long defaultTimeout;
  private final Vertx vertx;
  private final String serverId = UUID.randomUUID().toString();
  private final Set<String> subscriptions = new HashSet<>();
  private final Map<String, Object> listeners = new HashMap<>();
  private final String queueName;

  public NATSBrokerClient(final Vertx vertx) {
    this.vertx = vertx;
    final JsonObject conf = vertx.getOrCreateContext().config();
    final JsonObject natsRawConf = conf.getJsonObject("nats");
    defaultTimeout = conf.getLong("request-default-imeout", 5000L);
    queueName = conf.getString("queue-name", "entcore");
    io.nats.client.Options.Builder builder = new io.nats.client.Options.Builder(getNatsProperties(natsRawConf));
    final NatsOptions natsOptions = new NatsOptions()
      .setNatsBuilder(builder)
      .setVertx(vertx);
    natsClient = NatsClient.create(natsOptions);
  }

  @Override
  public Future<Void> start() {
    assertNatsClient("cannot.start.null.client");
    return natsClient.connect()
      .onSuccess(e -> log.info("NATS client connected on " + natsClient.getConnection().getConnectedUrl()))
      .compose(e -> loadListeners("META-INF/nats.json"))
      .onSuccess(this::registerListeners)
      .compose(e -> this.loadListeners("META-INF/broker-proxy.nats.json"))
      .onSuccess(this::registerProxies)
      .mapEmpty();
  }

  private void registerProxies(NATSContract contract) {
    final EventBus eb = this.vertx.eventBus();
    for (NATSEndpoint endpoint : contract.getEndpoints()) {
      if(!endpoint.isProxy()) {
        continue;
      }
      try {
        this.natsClient.subscribe(transformToWildcard(endpoint.getSubject()), this.queueName, msg -> {
          final Promise<Object> promise = Promise.promise();
            try {
              // call the real subject without wildcard
              eb.request(msg.getSubject(), getDataFromMessage(msg))
                .onSuccess(response -> {
                  try {
                    promise.tryComplete(response.body());
                  } catch (Exception e) {
                    log.error("Error serializing response to JSON", e);
                    promise.tryFail(e);
                  }
                })
                .onFailure(th -> {
                  log.error("Error calling subject " + msg.getSubject());
                  promise.tryFail(th);
                });
            } catch (IOException e) {
              promise.tryFail(e);
            }
            promise.future().onSuccess(response -> {
              try {
                final byte[] payload = mapper.writeValueAsString(response).getBytes(charset);
                natsClient.publish(msg.getReplyTo(), payload);
              } catch (Exception e) {
                sendError(msg, e);
              }
            }).onFailure(th -> {
              sendError(msg, th);
            });
          }).onSuccess(e -> log.info("Registered proxy for subject: " + endpoint.getSubject()))
          .onFailure(th -> log.error("Error while registering proxy for subject: " + endpoint.getSubject(), th));
      } catch (Exception e) {
        throw new RuntimeException("Error while registering listener for subject: " + endpoint, e);
      }
    }
  }

  private @Nullable Object getDataFromMessage(Message msg) throws IOException {
    byte[] data = msg.getData();
    final JsonNode rootNode = mapper.readTree(data);
    JsonNode dataNode = rootNode.get("data");
    if (dataNode != null) {
      data = dataNode.toString().getBytes(StandardCharsets.UTF_8);
    }
    return data;
  }

  private void sendError(Message msg, Throwable e) {
    final String message = e.getMessage() == null ? String.valueOf(e) : e.getMessage();
    try {
      natsClient.publish(msg.getReplyTo(), mapper.writeValueAsBytes(new BaseResponseDTO(false, message)));
    } catch (JsonProcessingException ex) {
      log.error("Cannot serialize error", ex);
      natsClient.publish(msg.getReplyTo(), message.getBytes(StandardCharsets.UTF_8));
    }
  }

  private void registerListeners(NATSContract contracts) {
    for (NATSEndpoint endpoint : contracts.getEndpoints()) {
      try {
        final Class<?> listenerClass = Class.forName(endpoint.getClassName());
        final Object listener = getListener(listenerClass);
        final Class<?> requestType = Class.forName(endpoint.getRequestType());
        final Class<?> responseType = isBlank(endpoint.getResponseType()) ? Void.class : Class.forName(endpoint.getRequestType());
        final Method handler = listenerClass.getMethod(endpoint.getMethodName(), requestType);
        //noinspection unchecked
        this.subscribe(endpoint.getSubject(), new BrokerListener() {
          @Override
          public Class<?> getRequestType() {
            return requestType;
          }

          @Override
          public Class getResponseType() {
            return responseType;
          }

          @Override
          public Future<?> onMessage(Object request, String subject) {
            try {
              Object response = handler.invoke(listener, request);
              if(response instanceof Future) {
                return (Future<?>) response;
              } else {
                return Future.succeededFuture(response);
              }
            } catch (Exception e) {
              log.error("Error invoking method", e);
              return Future.failedFuture(e);
            }
          }
        }).onSuccess(v -> {
          subscriptions.add(endpoint.getSubject());
        }).onFailure(e -> {
          log.error("Error while subscribing to subject: " + endpoint.getSubject(), e);
        });
      } catch (Exception e) {
        throw new RuntimeException("Error while registering listener for subject: " + endpoint, e);
      }
    }
  }

  private Object getListener(Class<?> listenerClass) {
    return listeners.computeIfAbsent(listenerClass.getCanonicalName(), k -> {
      try {
        return listenerClass.getConstructor(Vertx.class).newInstance(vertx);
      } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
        throw new IllegalStateException("Could not create listener of type " + listenerClass, e);
      }
    });
  }

  @Override
  public <K> Future<Void> sendMessage(String subject, K message) {
    assertNatsClient("cannot.send.message.null.client");
    final byte[] payload;
    try {
      payload = mapper.writeValueAsString(message).getBytes(charset);
      return natsClient.publish(subject, payload)
        .onFailure(e -> log.error("Error while sending message to NATS", e));
    } catch (Exception e) {
      log.error("An error occurred while serializing message to JSON : " + message, e);
      return Future.failedFuture(e);
    }
  }

  @Override
  public <K, V> Future<V> request(String subject, K message) {
    return this.request(subject, message, defaultTimeout);
  }

  @Override
  public <K, V> Future<V> request(String subject, K message, long timeout) {
    assertNatsClient("cannot.request.message.null.client");
    try {
      final byte[] payload = mapper.writeValueAsString(message).getBytes(charset);

      Promise<V> future = Promise.promise();
      final String replyTo = subject + ".reply." + serverId + "." + UUID.randomUUID();
      final AtomicLong timer = new AtomicLong(System.currentTimeMillis()); // This will be used to actually time out the request
      // Start by creating a subscription for the reply which will be where the response will be sent
      natsClient.subscribe(replyTo, msg -> {
        try {
          @SuppressWarnings("unchecked")
          V response = mapper.readValue(new String(msg.getData(), charset), (Class<V>) Object.class);
          future.tryComplete(response);
        } catch (Exception e) {
          log.error("Error deserializing response", e);
          future.tryFail(e);
        } finally {
          vertx.cancelTimer(timer.get());
          // Unsubscribe from the reply subject which was temporarily created
          this.unsubscribe(replyTo);
        }
      }).onFailure(th -> {
        log.error("Failed to subscribe to reply subject", th);
        future.tryFail(th);
      }).onSuccess(subscription -> {
        // Now that we're listening for the reply, we can send the request with our 
        // temporary reply subject
        natsClient.publish(subject, replyTo, payload)
          .onSuccess(e -> {
            log.debug("Message sent to subject: " + subject);
            // Set a timeout for the request
            long handle = vertx.setTimer(timeout, id -> {
              log.error("Request timed out for subject: " + subject);
              future.fail(new RuntimeException("Request timed out"));
              this.unsubscribe(replyTo);
            });
            timer.set(handle);
          })
          .onFailure(e -> {
            log.error("Error while sending message to NATS", e);
            future.fail(e);
            this.unsubscribe(replyTo);
          });
      });
      return future.future();
    } catch (Exception e) {
      log.error("Error while serializing message to JSON", e);
      return Future.failedFuture(e);
    }
  }

  @Override
  public <K, V> Future<Void> subscribe(String subject, BrokerListener<K, V> listener) {
    assertNatsClient("cannot.subscribe.null.client");
    if (subscriptions.contains(subject)) {
      throw new IllegalStateException("already.listening.on.subject." + subject);
    }
    return natsClient.subscribe(transformToWildcard(subject), this.queueName, msg -> {
      try {
        final K request = mapper.readValue(new String(msg.getData(), charset), listener.getRequestType());
        final Future<V> futureResponse = listener.onMessage(request, msg.getSubject());
        final String replyTo = msg.getReplyTo();
        if (replyTo != null && !replyTo.trim().isEmpty()) {
          futureResponse.onSuccess(response -> {
            try {
              final byte[] payload = mapper.writeValueAsString(response).getBytes(charset);
              natsClient.publish(replyTo, payload);
            } catch (Exception e) {
              log.error("Error serializing response to JSON", e);
            }
          }).onFailure(e -> {
            log.error("Error while processing message", e);
          });
        }
      } catch (Exception e) {
        log.error("Error deserializing request : " + msg, e);
      }
    })
    .onSuccess(e -> log.info("Listening on subject: " + subject))
    .onFailure(th -> log.error("Failed to subscribe to subject: " + subject, th));
  }

  @Override
  public Future<Void> close() {
    assertNatsClient("cannot.close.null.client");
    return natsClient.close();
  }

  private void assertNatsClient(final String message) {
    if (natsClient == null) {
      throw new IllegalStateException(message);
    }
  }

  private Properties getNatsProperties(JsonObject natsRawConf) {
    Properties properties = new Properties();
    if (natsRawConf != null) {
      natsRawConf.getMap().forEach((key, value) -> {
        if (value instanceof String) {
          properties.put(key, value);
        } else if (value instanceof Number) {
          properties.put(key, ((Number) value).intValue());
        } else if (value instanceof Boolean) {
          properties.put(key, value);
        }
      });
    }
    return properties;
  }

  @Override
  public Future<Void> unsubscribe(String subject) {
    assertNatsClient("cannot.unsubscribe.null.client");
    return natsClient.unsubscribe(subject)
      .onSuccess(e -> log.info("Successfully unsubscribed from subject: " + subject))
      .onFailure(e -> log.error("Error while unsubscribing from subject: " + subject, e));
  }

  private Future<NATSContract> loadListeners(final String natsJsonPath) {
    final InputStream is = this.getClass().getClassLoader().getResourceAsStream(natsJsonPath);
    if (is == null) {
      log.error("Cannot find " + natsJsonPath + " file in classpath");
      return Future.failedFuture(new FileNotFoundException("Cannot find " + natsJsonPath + " file in classpath"));
    } else {
      try {
        final NATSContract contracts = this.mapper.readValue(is, NATSContract.class);
        return Future.succeededFuture(contracts);
      } catch (IOException e) {
        return Future.failedFuture(e);
      }
    }
  }

  /**
   * Transforms a subject pattern with {param} placeholders to a NATS wildcard pattern
   * Example: "users.{id}.profile" -> "users.*.profile"
   * 
   * @param subject The original subject pattern with {param} placeholders
   * @return A NATS-compatible wildcard pattern
   */
  private String transformToWildcard(String subject) {
    if (subject == null || !subject.contains("{")) {
        return subject;
    }
    // Simple regex to replace all {param} patterns with NATS wildcards (*)
    return subject.replaceAll("\\{[^}]+\\}", "*");
  }

}
