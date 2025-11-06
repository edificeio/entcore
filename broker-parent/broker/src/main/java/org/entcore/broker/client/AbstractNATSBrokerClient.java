package org.entcore.broker.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Message;
import io.nats.vertx.NatsClient;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.broker.api.dto.NATSResponseDTO;
import org.entcore.broker.listener.BrokerListener;
import org.entcore.broker.nats.model.NATSContract;
import org.entcore.broker.nats.model.NATSEndpoint;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Abstract base class for NATS broker clients.<br>
 * <b>Purpose:</b>
 * <ul>
 *   <li>Centralizes all common logic for NATS/Vert.x integration, including EventBus bridging, listener management, and NATS operations (publish, request, subscribe, etc.).</li>
 *   <li>Implements the <code>BrokerClient</code> interface and provides a template for both single-broker and multi-broker implementations.</li>
 *   <li>Handles dynamic routing, contract-based listener registration, and proxying between NATS and Vert.x EventBus.</li>
 * </ul>
 *
 * <b>Usage:</b>
 * <ul>
 *   <li>Extend it to implement a concrete broker client (e.g., <code>NATSBrokerClient</code> or <code>NATSMultiBrokerClient</code>).</li>
 *   <li>Subclasses must implement three abstract methods: <code>getNatsClientForSubject</code>, <code>getAllNatsClients</code>, and <code>getQueueName</code>.</li>
 *   <li>Supports both legacy (single broker) and advanced (multi-broker, subject-based routing) scenarios.</li>
 *   <li>Automatically loads and registers listeners and proxies from contract files (<code>META-INF/nats.json</code>, <code>META-INF/broker-proxy.nats.json</code>).</li>
 *   <li>Provides robust error handling, dynamic listener instantiation, and transparent bridging between NATS and Vert.x EventBus.</li>
 * </ul>
 *
 * <b>Design patterns:</b>
 * <ul>
 *   <li>Template method: subclasses only provide routing and client selection, all orchestration is handled here.</li>
 *   <li>Polymorphism: all NATS operations (publish, request, subscribe, etc.) are routed through the abstract methods.</li>
 *   <li>Reflection: listeners are instantiated and invoked dynamically based on contract metadata.</li>
 * </ul>
 *
 * <b>See also:</b>
 * <ul>
 *   <li><code>NATSBrokerClient</code> (single broker, legacy mode)</li>
 *   <li><code>NATSMultiBrokerClient</code> (multi-broker, advanced routing)</li>
 *   <li><code>NATSBrokersConfig</code> (configuration and routing rules)</li>
 * </ul>
 */
public abstract class AbstractNATSBrokerClient implements BrokerClient {

    protected static final Logger log = LoggerFactory.getLogger(AbstractNATSBrokerClient.class);
    
    protected final Vertx vertx;
    protected final ObjectMapper mapper = new ObjectMapper();
    protected final String charset = "UTF-8";
    protected final long defaultTimeout;
    protected final String serverId = UUID.randomUUID().toString();
    protected final Set<String> subscriptions = new HashSet<>();
    protected final Map<String, Object> listeners = new HashMap<>();
    
    // EventBus consumers
    private MessageConsumer<?> publishConsumer;
    private MessageConsumer<?> requestConsumer;
    
    /**
     * Creates an abstract NATS broker client.
     * 
     * @param vertx The Vert.x instance
     */
    protected AbstractNATSBrokerClient(Vertx vertx) {
        this.vertx = vertx;
        final JsonObject conf = vertx.getOrCreateContext().config();
        this.defaultTimeout = conf.getLong("request-default-timeout", 5000L);
    }
    
    // ============================================================
    // ABSTRACT METHODS - To be implemented by subclasses
    // ============================================================
    
    /**
     * Gets the NATS client to use for a given subject.
     * Single broker returns the same client always.
     * Multi broker routes based on configuration.
     * 
     * @param subject The NATS subject
     * @return The NatsClient to use for this subject
     */
    protected abstract NatsClient getNatsClientForSubject(String subject);
    
    /**
     * Gets all NATS clients managed by this broker.
     * Used for initialization and shutdown.
     * 
     * @return List of all NatsClient instances
     */
    protected abstract List<NatsClient> getAllNatsClients();
    
    /**
     * Gets the queue name for NATS subscriptions.
     * 
     * @return The queue name
     */
    protected abstract String getQueueName();
    
    // ============================================================
    // START & LIFECYCLE
    // ============================================================
    
    @Override
    public Future<Void> start() {
        // 1. Connect all NATS clients
        List<Future<Void>> connectFutures = new ArrayList<>();
        for (NatsClient client : getAllNatsClients()) {
            Future<Void> connectFuture = client.connect()
                .onSuccess(e -> {
                    if (client.getConnection() != null) {
                        log.info("NATS client connected on " + client.getConnection().getConnectedUrl());
                    }
                });
            connectFutures.add(connectFuture);
        }
        
        return Future.all(connectFutures)
            // 2. Load and register listeners
            .compose(v -> loadAndRegisterAll())
            // 3. Setup EventBus handlers
            .compose(v -> setupEventBusPublishHandler())
            .compose(v -> setupEventBusRequestHandler())
            .mapEmpty();
    }
    
    @Override
    public Future<Void> close() {
        // Unregister EventBus handlers
        if (publishConsumer != null) {
            publishConsumer.unregister();
            publishConsumer = null;
        }
        if (requestConsumer != null) {
            requestConsumer.unregister();
            requestConsumer = null;
        }
        
        // Close all NATS clients
        List<Future<Void>> closeFutures = new ArrayList<>();
        for (NatsClient client : getAllNatsClients()) {
            closeFutures.add(client.close());
        }
        
        return Future.all(closeFutures).mapEmpty();
    }
    
    // ============================================================
    // EVENTBUS HANDLERS
    // ============================================================
    
    /**
     * Sets up the EventBus handler for broker.publish messages.
     * Delegates actual publishing to the underlying BrokerClient.
     * 
     * @return Future that completes when handler is set up
     */
    private Future<Void> setupEventBusPublishHandler() {
        final Promise<Void> promise = Promise.promise();
        
        // Unregister any existing consumer
        if (this.publishConsumer != null) {
            this.publishConsumer.unregister();
        }
        
        // Set up an event bus handler for "broker.publish" messages
        this.publishConsumer = vertx.eventBus().<JsonObject>consumer("broker.publish", message -> {
            final JsonObject body = message.body();
            final String subject = body.getString("subject");
            final String messageStr = body.getString("message");
            
            if (subject == null || subject.isEmpty()) {
                message.fail(400, "Subject cannot be empty");
                return;
            }
            
            try {
                // Prepare message object
                final Object messageObj = messageStr != null ? messageStr : "";
                
                // Delegate to sendMessage (uses polymorphism)
                this.<Object>sendMessage(subject, messageObj)
                    .onSuccess(v -> {
                        log.debug("Successfully published message to subject: " + subject);
                        message.reply(null); // Empty reply to indicate success
                    })
                    .onFailure(err -> {
                        log.error("Failed to publish message to subject: " + subject, err);
                        message.fail(500, err.getMessage());
                    });
            } catch (Exception e) {
                log.error("Error publishing message to subject: " + subject, e);
                message.fail(500, e.getMessage());
            }
        });
        
        log.info("Set up event bus handler for broker.publish");
        promise.complete();
        
        return promise.future();
    }
    
    /**
     * Sets up the EventBus handler for broker.request messages.
     * Delegates actual requests to the underlying BrokerClient.
     * 
     * @return Future that completes when handler is set up
     */
    private Future<Void> setupEventBusRequestHandler() {
        final Promise<Void> promise = Promise.promise();
        
        // Unregister any existing consumer
        if (this.requestConsumer != null) {
            this.requestConsumer.unregister();
        }
        
        // Set up an event bus handler for "broker.request" messages
        this.requestConsumer = vertx.eventBus().<JsonObject>consumer("broker.request", message -> {
            final JsonObject body = message.body();
            final String subject = body.getString("subject");
            final String requestStr = body.getString("message");
            final long timeout = body.getLong("timeout", defaultTimeout);
            
            if (subject == null || subject.isEmpty()) {
                message.fail(400, "Subject cannot be empty");
                return;
            }
            
            try {
                final NatsClient client = getNatsClientForSubject(subject);
                if (client == null) {
                    message.fail(500, "No NATS client available for subject: " + subject);
                    return;
                }
                request(client, subject, requestStr, timeout)
                    .onSuccess(response -> {
                        try {
                            final String responseJson = mapper.writeValueAsString(response);
                            message.reply(responseJson);
                        } catch (Exception e) {
                            log.error("Error serializing response", e);
                            message.fail(500, e.getMessage());
                        }
                    })
                    .onFailure(err -> {
                        log.error("Failed to make request to subject: " + subject, err);
                        message.fail(500, err.getMessage());
                    });
            } catch (Exception e) {
                log.error("Error processing request message for subject: " + subject, e);
                message.fail(500, e.getMessage());
            }
        });
        
        log.info("Set up event bus handler for broker.request");
        promise.complete();
        
        return promise.future();
    }
    
    // ============================================================
    // LISTENERS LOADING & REGISTRATION
    // ============================================================
    
    /**
     * Loads standard NATS contracts and registers them on all broker clients.
     * Loads both META-INF/nats.json and META-INF/broker-proxy.nats.json.
     * 
     * @return Future that completes when all contracts are loaded and registered
     */
    private Future<Void> loadAndRegisterAll() {
        return loadContract("META-INF/nats.json")
            .compose(contract -> registerListeners(contract))
            .compose(v -> loadContract("META-INF/broker-proxy.nats.json"))
            .compose(contract -> registerProxies(contract))
            .recover(err -> {
                // If files don't exist, it's not a critical error
                log.info("No NATS contract files found or error loading them: " + err.getMessage());
                return Future.succeededFuture();
            });
    }
    
    /**
     * Loads a NATS contract from the classpath.
     * 
     * @param path The classpath resource path to the JSON contract file
     * @return Future containing the loaded contract
     */
    private Future<NATSContract> loadContract(String path) {
        final InputStream is = this.getClass().getClassLoader().getResourceAsStream(path);
        if (is == null) {
            log.warn("Cannot find " + path + " file in classpath");
            return Future.failedFuture(new FileNotFoundException("Cannot find " + path + " file in classpath"));
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
     * Registers all listeners from a contract.
     * 
     * @param contract The NATS contract containing endpoints
     * @return Future that completes when all listeners are registered
     */
    private Future<Void> registerListeners(NATSContract contract) {
        for (NATSEndpoint endpoint : contract.getEndpoints()) {
            try {
                final Class<?> listenerClass = Class.forName(endpoint.getClassName());
                final Object listener = getListener(listenerClass);
                final Class<?> requestType = Class.forName(endpoint.getRequestType());
                final Class<?> responseType = isBlank(endpoint.getResponseType()) 
                    ? Void.class 
                    : Class.forName(endpoint.getResponseType());
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
                            if (response instanceof Future) {
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
                    log.info("Registered listener for subject: " + endpoint.getSubject());
                }).onFailure(e -> {
                    log.error("Error while subscribing to subject: " + endpoint.getSubject(), e);
                });
            } catch (Exception e) {
                log.error("Error while registering listener for subject: " + endpoint, e);
                throw new RuntimeException("Error while registering listener for subject: " + endpoint, e);
            }
        }
        
        return Future.succeededFuture();
    }
    
    /**
     * Registers all proxies from a contract.
     * Proxies bridge NATS subjects to EventBus addresses.
     * 
     * @param contract The NATS contract containing endpoints
     * @return Future that completes when all proxies are registered
     */
    private Future<Void> registerProxies(NATSContract contract) {
        final EventBus eb = this.vertx.eventBus();
        
        for (NATSEndpoint endpoint : contract.getEndpoints()) {
            if (!endpoint.isProxy()) {
                continue;
            }
            
            try {
                final String subject = transformToWildcard(endpoint.getSubject());
                final Handler<Message> messageHandler = getProxyMessageHandler(eb);
                final NatsClient natsClient = getNatsClientForSubject(endpoint.getSubject());
                final Future<Void> future;
                
                if (endpoint.isBroadcast()) {
                    future = natsClient.subscribe(subject, messageHandler);
                } else {
                    future = natsClient.subscribe(subject, getQueueName(), messageHandler);
                }
                
                future.onSuccess(e -> log.info("Registered proxy for subject: " + endpoint.getSubject()))
                    .onFailure(th -> log.error("Error while registering proxy for subject: " + endpoint.getSubject(), th));
            } catch (Exception e) {
                log.error("Error while registering proxy for subject: " + endpoint, e);
                throw new RuntimeException("Error while registering proxy for subject: " + endpoint, e);
            }
        }
        
        return Future.succeededFuture();
    }
    
    /**
     * Creates a proxy message handler that bridges NATS to EventBus.
     * 
     * @param eventBus The EventBus instance
     * @return The message handler
     */
    private Handler<Message> getProxyMessageHandler(final EventBus eventBus) {
        return msg -> {
            final Promise<Object> promise = Promise.promise();
            final NatsClient natsClient = getNatsClientForSubject(msg.getSubject());
            
            try {
                eventBus.request(msg.getSubject(), getDataFromMessage(msg))
                    .onSuccess(response -> {
                        try {
                            promise.tryComplete(response.body());
                        } catch (Exception e) {
                            log.error("Error serializing response to JSON", e);
                            promise.tryFail(e);
                        }
                    })
                    .onFailure(th -> {
                        log.error("Error calling subject " + msg.getSubject(), th);
                        promise.tryFail(th);
                    });
            } catch (IOException e) {
                promise.tryFail(e);
            }
            
            promise.future().onSuccess(response -> {
                try {
                    final byte[] payload = ((String) response).getBytes(charset);
                    natsClient.publish(msg.getReplyTo(), payload);
                } catch (Exception e) {
                    sendError(msg, e, natsClient);
                }
            }).onFailure(th -> {
                sendError(msg, th, natsClient);
            });
        };
    }
    
    /**
     * Extracts data from a NATS message.
     * 
     * @param msg The NATS message
     * @return The extracted data
     * @throws IOException If parsing fails
     */
    private Object getDataFromMessage(Message msg) throws IOException {
        byte[] data = msg.getData();
        final JsonNode rootNode = mapper.readTree(data);
        JsonNode dataNode = rootNode.get("data");
        if (dataNode != null) {
            data = dataNode.toString().getBytes(StandardCharsets.UTF_8);
        }
        return data;
    }
    
    /**
     * Sends an error response to a NATS reply subject.
     * 
     * @param msg The original NATS message
     * @param e The error
     * @param natsClient The NATS client to use
     */
    private void sendError(Message msg, Throwable e, NatsClient natsClient) {
        final String message = e.getMessage() == null ? String.valueOf(e) : e.getMessage();
        final NATSResponseDTO natsResponseDTO = new NATSResponseDTO(null, message, false, null);
        try {
            natsClient.publish(msg.getReplyTo(), mapper.writeValueAsBytes(natsResponseDTO));
        } catch (JsonProcessingException ex) {
            log.error("Cannot serialize error", ex);
            natsClient.publish(msg.getReplyTo(), message.getBytes(StandardCharsets.UTF_8));
        }
    }
    
    /**
     * Gets or creates a listener instance for the given class.
     * 
     * @param listenerClass The listener class
     * @return The listener instance
     */
    private Object getListener(Class<?> listenerClass) {
        return listeners.computeIfAbsent(listenerClass.getCanonicalName(), k -> {
            try {
                return listenerClass.getConstructor(Vertx.class).newInstance(vertx);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                throw new IllegalStateException("Could not create listener of type " + listenerClass, e);
            }
        });
    }
    
    // ============================================================
    // BROKERCLIENT INTERFACE IMPLEMENTATION
    // ============================================================
    
    @Override
    public <K> Future<Void> sendMessage(String subject, K message) {
        final NatsClient client = getNatsClientForSubject(subject);
        if (client == null) {
            return Future.failedFuture("No NATS client available for subject: " + subject);
        }
        
        try {
            final byte[] payload = mapper.writeValueAsString(message).getBytes(charset);
            return client.publish(subject, payload)
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
        final NatsClient client = getNatsClientForSubject(subject);
        if (client == null) {
            return Future.failedFuture("No NATS client available for subject: " + subject);
        }
        try {
            final String payload = mapper.writeValueAsString(message);
            return request(client, subject, payload, timeout)
                    .compose(responseStr -> {
                        try {
                            @SuppressWarnings("unchecked")
                            V response = mapper.readValue(responseStr, (Class<V>) Object.class);
                            return Future.succeededFuture(response);
                        } catch (Exception err) {
                            return Future.failedFuture(err);
                        }
                    });
        } catch (Exception e) {
            log.error("Error while serializing message to JSON", e);
            return Future.failedFuture(e);
        }
    }
    /**
     * Internal helper to perform a NATS request and deserialize the response.
     * @param client The NatsClient to use
     * @param subject The NATS subject
     * @param payload The serialized request payload
     * @param timeout The timeout in ms
     * @return Future with the serialized response
     */
    private Future<String> request(NatsClient client, String subject, String payload, long timeout) throws Exception {
        Promise<String> future = Promise.promise();
        final byte[] payloadBytes = payload != null? payload.getBytes(charset) : new byte[0];
        client.request(subject, payloadBytes, Duration.ofMillis(timeout))
            .onSuccess(e -> {
                log.debug("Message sent to subject: " + subject);
                try {
                    final String responseStr = new String(e.getData(), charset);
                    future.complete(responseStr);
                } catch(Exception err) {
                    future.fail(err);
                }
            })
            .onFailure(e -> {
                log.error("Error while sending message to NATS", e);
                future.fail(e);
            });
        return future.future();
    }
    
    @Override
    public <K, V> Future<Void> subscribe(String subject, BrokerListener<K, V> listener) {
        final NatsClient client = getNatsClientForSubject(subject);
        if (client == null) {
            return Future.failedFuture("No NATS client available for subject: " + subject);
        }
        
        if (subscriptions.contains(subject)) {
            throw new IllegalStateException("already.listening.on.subject." + subject);
        }
        
        return client.subscribe(transformToWildcard(subject), getQueueName(), msg -> {
            try {
                final K request = mapper.readValue(new String(msg.getData(), charset), listener.getRequestType());
                final Future<V> futureResponse = listener.onMessage(request, msg.getSubject());
                final String replyTo = msg.getReplyTo();
                if (replyTo != null && !replyTo.trim().isEmpty()) {
                    futureResponse.onSuccess(response -> {
                        try {
                            final byte[] payload = mapper.writeValueAsString(response).getBytes(charset);
                            client.publish(replyTo, payload);
                        } catch (Exception e) {
                            log.error("Error serializing response to JSON", e);
                            sendError(msg, e, client);
                        }
                    }).onFailure(e -> {
                        log.error("Error while processing message", e);
                        sendError(msg, e, client);
                    });
                }
            } catch (Exception e) {
                log.error("Error deserializing request : " + msg, e);
            }
        })
        .onSuccess(e -> {
            subscriptions.add(subject);
            log.info("Listening on subject: " + subject);
        })
        .onFailure(th -> log.error("Failed to subscribe to subject: " + subject, th));
    }
    
    @Override
    public Future<Void> unsubscribe(String subject) {
        final NatsClient client = getNatsClientForSubject(subject);
        if (client == null) {
            return Future.failedFuture("No NATS client available for subject: " + subject);
        }
        
        subscriptions.remove(subject);
        return client.unsubscribe(subject)
            .onSuccess(e -> log.info("Successfully unsubscribed from subject: " + subject))
            .onFailure(e -> log.error("Error while unsubscribing from subject: " + subject, e));
    }
    
    // ============================================================
    // UTILITY METHODS
    // ============================================================
    
    /**
     * Transforms a subject pattern with {param} placeholders to a NATS wildcard pattern.
     * Example: "users.{id}.profile" -> "users.*.profile"
     * 
     * @param subject The original subject pattern with {param} placeholders
     * @return A NATS-compatible wildcard pattern
     */
    protected String transformToWildcard(String subject) {
        if (subject == null || !subject.contains("{")) {
            return subject;
        }
        // Simple regex to replace all {param} patterns with NATS wildcards (*)
        return subject.replaceAll("\\{[^}]+\\}", "*");
    }
}
