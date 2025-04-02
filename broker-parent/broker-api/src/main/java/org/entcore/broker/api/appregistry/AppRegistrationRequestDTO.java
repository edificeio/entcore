package org.entcore.broker.api.appregistry;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.wseduc.webutils.security.SecuredAction;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AppRegistrationRequestDTO {
  private final AppRegistrationDTO application;
  private final List<SecuredAction> actions;

  @JsonCreator
  public AppRegistrationRequestDTO(
    @JsonProperty("application") final AppRegistrationDTO application,
    @JsonProperty("actions") final List<SecuredAction> actions) {
    this.application = application;
    this.actions = actions;
  }

  public AppRegistrationDTO getApplication() {
    return application;
  }

  public List<SecuredAction> getActions() {
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
