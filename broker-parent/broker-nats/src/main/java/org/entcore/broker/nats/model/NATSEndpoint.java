package org.entcore.broker.nats.model;

/**
 * Represents a single NATS endpoint (subject and its associated request/response types).
 */
public class NATSEndpoint {
  private String subject;
  private String description;
  private String queue;
  private String requestType;
  private Object requestSchema; // JSON Schema representation
  private String responseType;
  private Object responseSchema; // JSON Schema representation
  private String methodName;
  private String className;
  private boolean proxy;

  public String getSubject() {
    return subject;
  }

  public void setSubject(String subject) {
    this.subject = subject;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getQueue() {
    return queue;
  }

  public void setQueue(String queue) {
    this.queue = queue;
  }

  public String getRequestType() {
    return requestType;
  }

  public void setRequestType(String requestType) {
    this.requestType = requestType;
  }

  public Object getRequestSchema() {
    return requestSchema;
  }

  public void setRequestSchema(Object requestSchema) {
    this.requestSchema = requestSchema;
  }

  public String getResponseType() {
    return responseType;
  }

  public void setResponseType(String responseType) {
    this.responseType = responseType;
  }

  public Object getResponseSchema() {
    return responseSchema;
  }

  public void setResponseSchema(Object responseSchema) {
    this.responseSchema = responseSchema;
  }

  public String getMethodName() {
    return methodName;
  }

  public void setMethodName(String methodName) {
    this.methodName = methodName;
  }

  public String getClassName() {
    return className;
  }

  public void setClassName(String className) {
    this.className = className;
  }

  public boolean isProxy() {
    return proxy;
  }

  public void setProxy(boolean proxy) {
    this.proxy = proxy;
  }

  @Override
  public String toString() {
    return "NATSEndpoint{" +
      "subject='" + subject + '\'' +
      ", description='" + description + '\'' +
      ", queue='" + queue + '\'' +
      ", requestType='" + requestType + '\'' +
      ", requestSchema=" + requestSchema +
      ", responseType='" + responseType + '\'' +
      ", responseSchema=" + responseSchema +
      ", methodName='" + methodName + '\'' +
      ", className='" + className + '\'' +
      ", proxy=" + proxy +
      '}';
  }
}
