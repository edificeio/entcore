package org.entcore.broker.api;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.broker.api.utils.AddressParameter;
import org.entcore.broker.api.utils.BrokerAddressUtils;
import org.entcore.broker.api.utils.ReflectionUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;

/**
 * Factory for creating broker proxies.
 * This class provides methods to generate implementations of interfaces
 * annotated with @BrokerPublisher for publishing messages or @BrokerListener 
 * for request/response communication with the broker.
 */
public class BrokerFactory {
    private static final Logger log = LoggerFactory.getLogger(BrokerFactory.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private BrokerFactory(){}
    
    /**
     * Creates a new instance of a broker publisher interface
     * 
     * @param <T> The interface type (publisher or listener)
     * @param interfaceClass The class of the interface
     * @param vertx Vertx instance to use for event bus communication
     * @param addressParameters Optional parameters for subject placeholder substitution
     * @return A new instance of the interface
     */
    @SuppressWarnings("unchecked")
    public static <T> T create(final Class<T> interfaceClass, final Vertx vertx, final AddressParameter... addressParameters) {
        return (T) Proxy.newProxyInstance(
            interfaceClass.getClassLoader(),
            new Class<?>[] { interfaceClass },
            new BrokerProxyInvocationHandler(vertx, addressParameters));
    }
    
    /**
     * Handles method invocations on the broker proxy (both publisher and listener)
     */
    private static class BrokerProxyInvocationHandler implements InvocationHandler {
        private final Vertx vertx;
        private final Map<String, String> params;
        private final List<SubjectMatcher> disabledSubjects;
        
        /**
         * Creates a new BrokerProxyInvocationHandler
         * 
         * @param vertx Vertx instance
         * @param addressParameters Parameters for subject placeholders
         */
        public BrokerProxyInvocationHandler(Vertx vertx, AddressParameter... addressParameters) {
            this.vertx = vertx;
            this.params = BrokerAddressUtils.getParametersMap(addressParameters);
            this.disabledSubjects = loadDisabledSubjects(vertx);
            
            if (!disabledSubjects.isEmpty()) {
                log.info("Broker proxy has " + disabledSubjects.size() + " disabled subject patterns");
            }
        }
        
        /**
         * Loads the disabled subjects from configuration
         * 
         * @param vertx Vertx instance to access shared data
         * @return List of subject matchers for disabled subjects
         */
        private List<SubjectMatcher> loadDisabledSubjects(Vertx vertx) {
            final List<SubjectMatcher> matchers = new ArrayList<>();
            
            try {
                // Get the broker configuration from shared data
                final String brokerConfigStr = (String) vertx.sharedData().getLocalMap("server").get("brokerConfig");
                if (brokerConfigStr != null && !brokerConfigStr.isEmpty()) {
                    JsonObject brokerConfig = new JsonObject(brokerConfigStr);
                    
                    // Get the disabled subjects array
                    final JsonArray disabledArray = brokerConfig.getJsonArray("disabledSubjects");
                    if (disabledArray != null) {
                        for (int i = 0; i < disabledArray.size(); i++) {
                            final String pattern = disabledArray.getString(i);
                            if (pattern != null && !pattern.isEmpty()) {
                                matchers.add(new SubjectMatcher(pattern));
                                log.debug("Added disabled subject pattern: " + pattern);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Error loading broker disabled subjects configuration", e);
            }
            
            return matchers;
        }
        
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            // Check for Object methods like toString(), equals(), etc.
            if (method.getDeclaringClass() == Object.class) {
                return method.invoke(this, args);
            }
            
            // Check for BrokerPublisher annotation (pub/sub pattern)
            Optional<BrokerPublisher> publisherAnnotation = ReflectionUtils.getMethodAnnotation(
                BrokerPublisher.class, 
                method.getDeclaringClass(), 
                method);

            if (publisherAnnotation.isPresent()) {
                return handlePublisherMethod(publisherAnnotation.get(), method, args);
            }
            
            // Check for BrokerListener annotation (request/response pattern)
            Optional<BrokerListener> listenerAnnotation = ReflectionUtils.getMethodAnnotation(
                BrokerListener.class, 
                method.getDeclaringClass(), 
                method);
                
            if (listenerAnnotation.isPresent()) {
                return handleListenerMethod(listenerAnnotation.get(), method, args);
            }

            // No valid annotation found
            return Future.failedFuture(
                new UnsupportedOperationException("Method " + method.getName() + 
                    " has neither BrokerPublisher nor BrokerListener annotation"));
        }
        
        /**
         * Handles methods annotated with BrokerPublisher (pub/sub pattern)
         */
        private Future<Void> handlePublisherMethod(BrokerPublisher annotation, Method method, Object[] args) 
                throws JsonProcessingException {
            // Replace placeholders in the subject
            final String subject = BrokerAddressUtils.replacePlaceholders(annotation.subject(), params);
            
            // Check if subject is disabled
            if (isSubjectDisabled(subject)) {
                log.debug("Subject is disabled, skipping publish: " + subject);
                return Future.succeededFuture();
            }

            final Object message = args != null && args.length > 0 ? args[0] : null;
            final String messageJson = message == null ? null : mapper.writeValueAsString(message);
            
            // Build the request body
            final JsonObject requestBody = new JsonObject()
                .put("subject", subject)
                .put("message", messageJson);
            
            // Configure delivery options if timeout is specified
            final DeliveryOptions options = new DeliveryOptions();
            if (annotation.timeout() > 0) {
                options.setSendTimeout(annotation.timeout());
            }
            
            // Publish via event bus
            final Promise<Void> promise = Promise.promise();
            vertx.eventBus().request("broker.publish", requestBody, options)
                .onSuccess(reply -> {
                    log.debug("Successfully published message to subject: " + subject);
                    promise.complete();
                })
                .onFailure(err -> {
                    log.error("Failed to publish message to subject: " + subject, err);
                    promise.fail(err);
                });
                
            return promise.future();
        }
        
        /**
         * Handles methods annotated with BrokerListener (request/response pattern)
         */
        @SuppressWarnings("unchecked")
        private <T> Future<T> handleListenerMethod(BrokerListener annotation, Method method, Object[] args) 
                throws JsonProcessingException {
            // Replace placeholders in the subject
            final String subject = BrokerAddressUtils.replacePlaceholders(annotation.subject(), params);
            
            // Check if subject is disabled
            if (isSubjectDisabled(subject)) {
                log.debug("Subject is disabled, skipping request: " + subject);
                return Future.failedFuture(new IllegalStateException("Subject is disabled: " + subject));
            }

            final Object request = args != null && args.length > 0 ? args[0] : null;
            final String requestJson = request == null ? null : mapper.writeValueAsString(request);
            
            // Build the request body for the event bus
            final JsonObject requestBody = new JsonObject()
                .put("subject", subject)
                .put("message", requestJson)
                .put("timeout", annotation.timeout());
            
            // Configure delivery options if timeout is specified
            final DeliveryOptions options = new DeliveryOptions();
            if (annotation.timeout() > 0) {
                options.setSendTimeout(annotation.timeout());
            }
            
            // Get the expected return type
            Type returnType = method.getGenericReturnType();
            final JavaType javaType;
            if (returnType instanceof Class) {
                // If it's a simple class type
                javaType = TypeFactory.defaultInstance().constructType(returnType);
            } else {
                // For parameterized types (like Future<MyType>), determine the actual type argument once
                JavaType tmp;
                try {
                    Class<?> responseType = method.getReturnType();
                    if (Future.class.isAssignableFrom(responseType)) {
                        tmp = TypeFactory.defaultInstance().constructType(
                            ReflectionUtils.getTypeArgumentOfFuture(method));
                    } else {
                        tmp = TypeFactory.defaultInstance().constructType(Object.class);
                    }
                } catch (Exception e) {
                    log.warn("Could not determine response type for " + method.getName(), e);
                    tmp = TypeFactory.defaultInstance().constructType(Object.class);
                }
                javaType = tmp;
            }
            
            // Make the request via event bus
            final Promise<T> promise = Promise.promise();
            vertx.eventBus().request("broker.request", requestBody, options)
                .onSuccess(reply -> {
                    try {
                        if (reply.body() == null) {
                            promise.complete(null);
                            return;
                        }
                        final String responseJson = (String) reply.body();
                        T response = (T) mapper.readValue(responseJson, javaType);
                        promise.complete(response);
                    } catch (Exception e) {
                        log.error("Error deserializing response for subject: " + subject, e);
                        promise.fail(e);
                    }
                })
                .onFailure(err -> {
                    log.error("Failed to make request to subject: " + subject, err);
                    promise.fail(err);
                });
                
            return promise.future();
        }
        
        /**
         * Check if a subject is disabled based on configuration
         * 
         * @param subject The subject to check
         * @return true if the subject is disabled, false otherwise
         */
        private boolean isSubjectDisabled(String subject) {
            for (SubjectMatcher matcher : disabledSubjects) {
                if (matcher.matches(subject)) {
                    return true;
                }
            }
            return false;
        }
    }
    
    /**
     * Helper class to match subject patterns
     * Supports wildcards: 
     * - * to match any segment in a subject
     * - # to match multiple segments (only at the end)
     */
    private static class SubjectMatcher {
        private final String pattern;
        private final Pattern regex;
        
        /**
         * Create a new subject matcher from a pattern string
         * 
         * @param pattern Subject pattern with possible wildcards
         */
        public SubjectMatcher(String pattern) {
            this.pattern = pattern;
            
            // Convert the NATS-style subject pattern to a regex
            // Replace . with \. (escape dots)
            // Replace * with [^.]+ (match any segment)
            // Replace # with .* (match anything to the end, only allowed at the end)
            String regexPattern = pattern
                .replace(".", "\\.")
                .replace("*", "[^.]+");
            
            if (regexPattern.endsWith("#")) {
                regexPattern = regexPattern.substring(0, regexPattern.length() - 1) + ".*";
            }
            
            this.regex = Pattern.compile("^" + regexPattern + "$");
        }
        
        /**
         * Check if this matcher matches the given subject
         * 
         * @param subject Subject to check
         * @return true if the subject matches this pattern, false otherwise
         */
        public boolean matches(String subject) {
            return regex.matcher(subject).matches();
        }
        
        @Override
        public String toString() {
            return pattern;
        }
    }
}