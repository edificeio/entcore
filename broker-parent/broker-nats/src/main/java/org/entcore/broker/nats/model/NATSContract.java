package org.entcore.broker.nats.model;

import java.util.ArrayList;
import java.util.List;

public class NATSContract {
  private String serviceName;
  private String version;
  private List<NATSEndpoint> endpoints = new ArrayList<>();

  // Getters and setters
  public String getServiceName() {
    return serviceName;
  }

  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public List<NATSEndpoint> getEndpoints() {
    return endpoints;
  }

  public void setEndpoints(List<NATSEndpoint> endpoints) {
    this.endpoints = endpoints;
  }

  @Override
  public String toString() {
    return "NATSContract{" +
      "serviceName='" + serviceName + '\'' +
      ", version='" + version + '\'' +
      ", endpoints=" + endpoints +
      '}';
  }
}
