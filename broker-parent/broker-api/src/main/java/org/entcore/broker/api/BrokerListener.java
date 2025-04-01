package org.entcore.broker.api;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark methods that should listen on a NATS subject.
 * Methods annotated with this will be automatically registered as NATS listeners at runtime.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface BrokerListener {
  /**
   * The NATS subject to subscribe to.
   * @return NATS subject
   */
  String subject();

  /**
   * Optional queue name for queue groups.
   * If specified, the subscription will join this queue group.
   * @return queue name
   */
  String queue() default "";

  /**
   * Description of this NATS endpoint.
   * @return endpoint description
   */
  String description() default "";

  boolean proxy() default false;
}