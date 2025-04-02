package org.entcore.broker.api.appregistry;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AppRegistrationDTO {
  private final String name;
  private final String displayName;
  private final String appType;
  private final String icon;
  private final String address;
  private final boolean display;
  private final String prefix;
  private final Map<String, Object> customProperties;

  @JsonCreator
  public AppRegistrationDTO(@JsonProperty("name") final String name,
                            @JsonProperty("displayName") final String displayName,
                            @JsonProperty("appType") final String appType,
                            @JsonProperty("icon") final String icon,
                            @JsonProperty("address") final String address,
                            @JsonProperty("display") final boolean display,
                            @JsonProperty("prefix") final String prefix,
                            @JsonProperty("customProperties") final Map<String, Object> customProperties) {
    this.name = name;
    this.displayName = displayName;
    this.appType = appType;
    this.icon = icon;
    this.address = address;
    this.display = display;
    this.prefix = prefix;
    this.customProperties = customProperties;
  }

  public String getName() {
    return name;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getAppType() {
    return appType;
  }

  public String getIcon() {
    return icon;
  }

  public String getAddress() {
    return address;
  }

  public boolean isDisplay() {
    return display;
  }

  public String getPrefix() {
    return prefix;
  }

  public Map<String, Object> getCustomProperties() {
    return customProperties;
  }

  @Override
  public String toString() {
    return "AppRegistrationDTO{" +
      "name='" + name + '\'' +
      ", displayName='" + displayName + '\'' +
      ", appType='" + appType + '\'' +
      ", icon='" + icon + '\'' +
      ", address='" + address + '\'' +
      ", display=" + display +
      ", prefix='" + prefix + '\'' +
      ", customProperties=" + customProperties +
      '}';
  }
}
