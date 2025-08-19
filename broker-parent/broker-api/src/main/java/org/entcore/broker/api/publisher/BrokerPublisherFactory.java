package org.entcore.broker.api.publisher;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.broker.api.BrokerPublisher;
import org.entcore.broker.api.utils.AddressParameter;
import org.entcore.broker.api.utils.BrokerProxyUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Factory for creating broker publisher proxies.
 * This class provides methods to generate implementations of interfaces
 * annotated with @BrokerPublisher to publish messages to the broker.
 */
public class BrokerPublisherFactory {
    private static final Logger log = LoggerFactory.getLogger(BrokerPublisherFactory.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    
    /**
     * Creates a new instance of a broker publisher interface
     * 
     * @param <T> The publisher interface type
     * @param publisherClass The class of the publisher interface
     * @param vertx Vertx instance to use for event bus communication
     * @param addressParameters Optional parameters for subject placeholder substitution
     * @return A new instance of the publisher interface
     */
    @SuppressWarnings("unchecked")
    public static <T> T create(final Class<T> publisherClass, final Vertx vertx, final AddressParameter... addressParameters) {
        return (T) Proxy.newProxyInstance(
            publisherClass.getClassLoader(),
            new Class<?>[] { publisherClass },
            new PublisherInvocationHandler(vertx, addressParameters));
    }
    
    /**
     * Handles method invocations on the publisher proxy
     */
    private static class PublisherInvocationHandler implements InvocationHandler {
        private final Vertx vertx;
        private final Map<String, String> params;
        private final List<SubjectMatcher> disabledSubjects;
        
        /**
         * Creates a new PublisherInvocationHandler
         * 
         * @param vertx Vertx instance
         * @param addressParameters Parameters for subject placeholders
         */
        public PublisherInvocationHandler(Vertx vertx, AddressParameter... addressParameters) {
            this.vertx = vertx;
            this.params = getParametersMap(addressParameters);
            this.disabledSubjects = loadDisabledSubjects(vertx);
            
            if (!disabledSubjects.isEmpty()) {
                log.info("Broker publisher has " + disabledSubjects.size() + " disabled subject patterns");
            }
        }
        
        /**
         * Convert address parameters to a map
         * 
         * @param addressParameters Array of address parameters
         * @return Map of parameter names to values
         */
        private Map<String, String> getParametersMap(AddressParameter[] addressParameters) {
            Map<String, String> result = new HashMap<>();
            if (addressParameters != null) {
                for (AddressParameter param : addressParameters) {
                    result.put(param.getName(), param.getValue());
                }
            }
            return result;
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
            
            // Get the BrokerPublisher annotation from method or interface
            BrokerPublisher annotation = method.getAnnotation(BrokerPublisher.class);
            if (annotation == null) {
                // Check interfaces for annotation
                Class<?>[] interfaces = method.getDeclaringClass().getInterfaces();
                for (Class<?> iface : interfaces) {
                    try {
                        Method ifaceMethod = iface.getMethod(method.getName(), method.getParameterTypes());
                        annotation = ifaceMethod.getAnnotation(BrokerPublisher.class);
                        if (annotation != null) break;
                    } catch (NoSuchMethodException e) {
                        // Continue searching
                    }
                }
            }
                
            if (annotation == null) {
                return Future.failedFuture(
                    new UnsupportedOperationException("Method " + method.getName() + " is not a BrokerPublisher"));
            }
            
            // Replace placeholders in the subject
            final String subject = replacePlaceholders(annotation.subject(), params);
            
            // Check if subject is disabled
            if (isSubjectDisabled(subject)) {
                log.debug("Subject is disabled, skipping publish: " + subject);
                return Future.succeededFuture();
            }
            
            // Get the message to publish (first argument)
            final Object message = args.length > 0 ? args[0] : null;
            
            // Serialize the message
            final String messageJson = message != null ? mapper.writeValueAsString(message) : null;
            
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
        
        /**
         * Replace placeholders in subject with actual values
         * 
         * @param subject The subject with potential placeholders
         * @param params Map of parameter values
         * @return The subject with placeholders replaced
         */
        private String replacePlaceholders(String subject, Map<String, String> params) {
            String result = subject;
            for (Map.Entry<String, String> entry : params.entrySet()) {
                result = result.replace("{" + entry.getKey() + "}", entry.getValue());
            }
            return result;
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