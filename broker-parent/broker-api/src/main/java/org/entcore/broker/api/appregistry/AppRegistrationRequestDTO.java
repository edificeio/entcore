package org.entcore.broker.api.appregistry;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AppRegistrationRequestDTO {
  private final AppRegistrationDTO application;
  private final List<SecuredActionDTO> actions;

  @JsonCreator
  public AppRegistrationRequestDTO(
    @JsonProperty("application") final AppRegistrationDTO application,
    @JsonProperty("actions") final List<SecuredActionDTO> actions) {
    this.application = application;
    this.actions = actions;
  }

  public AppRegistrationDTO getApplication() {
    return application;
  }

  public List<SecuredActionDTO> getActions() {
    return actions;
  }

  @Override
  public String toString() {
    return "AppRegistrationRequestDTO{" +
      "application=" + application +
      ", actions=" + actions +
      '}';
  }
}
