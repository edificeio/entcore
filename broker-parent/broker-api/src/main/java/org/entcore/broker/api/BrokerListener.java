package org.entcore.broker.api;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark methods that should listen on a broker particular subject.
 * Example :
 * <pre>{@code
 *
 * @BrokerListener(subject = "my.subject.noreply", description = "Meaningful description here")
 * public Future<Void> myMethod(final RequestDTO request) {
 *   // Do async stuff here that will automatically be called when a message is received on the subject 'my.subject'
 *   // But does not provide any response to the caller
 * }
 *
 * @BrokerListener(subject = "my.subject.reply", description = "Meaningful description here")
 * public Future<ResponseDTO> myMethod(final RequestDTO request) {
 *   // Do async stuff here that will automatically be called when a message is received on the subject 'my.subject.reply'
 *   // The caller will automatically receive a response of type {@code ResponseDTO} when the method is done.
 * }
 *
 * // Declare a method like this in an interface that will listen on the event bus to receive messages coming from the broker.
 * // It allows different modules to interact with the broker via the event bus to avoid direct dependencies on the broker
 * // and opening multiple connections to the broker.
 * @BrokerListener(subject = "app.eventbus.subject", description = "Meaningful description here", proxy=true)
 * Future<ResponseDTO> myMethod(final RequestDTO request);
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface BrokerListener {
  /**
   * The NATS subject to subscribe to.<br />
   * This subject must :
   * <ul>
   * <li>respect the naming conventions of <a href="https://docs.nats.io/nats-concepts/subjects">NATS subjects</a></li>
   * <li>(when {@code proxy=true})can contain parameters enclosed in brackets (e.g. {@code {application}.export.resources}) that will be replaced by the values
   * provided in the {@code addressParameters} at instantiation</li>
   * </ul>
   * @return NATS subject
   */
  String subject();

  /**
   * Description of this NATS endpoint.
   * @return endpoint description
   */
  String description() default "";

  /**
   * Set this to {@code true} if the method should just act as a proxy between the broker and the event bus
   * @return true if is a proxy
   */
  boolean proxy() default false;
}