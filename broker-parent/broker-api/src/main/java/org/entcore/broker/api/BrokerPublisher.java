package org.entcore.broker.api;

import java.lang.annotation.*;

/**
 * Annotation to mark methods that publish messages to the broker on a specific subject.
 * Methods annotated with this will automatically send messages to the broker through the event bus.
 * 
 * Example:
 * <pre>{@code
 * @BrokerPublisher(subject = "notifications.user.create")
 * Future<Void> notifyUserCreated(UserCreationMessage message);
 * 
 * @BrokerPublisher(subject = "critical.operations.execute", timeout = 5000)
 * Future<Void> executeCriticalOperation(OperationRequest request);
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface BrokerPublisher {
    /**
     * The NATS subject to publish to.
     * This subject can contain parameters enclosed in brackets (e.g. {application}.event.published)
     * that will be replaced by values provided during execution.
     * @return NATS subject to publish to
     */
    String subject();
    
    /**
     * Optional timeout in milliseconds for the broker publish operation.
     * A value of 0 (default) means no specific timeout will be applied,
     * and the system default will be used.
     * @return timeout in milliseconds
     */
    long timeout() default 0;
}