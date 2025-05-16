package org.entcore.broker.api.appregistry;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SecuredActionDTO {
    private final String name;
    private final String displayName;
    private final String type;

    @JsonCreator
    public SecuredActionDTO(@JsonProperty("name") String name,
                            @JsonProperty("displayName") String displayName,
                            @JsonProperty("type") String type) {
      this.name = name;
      this.displayName = displayName;
      this.type = type;
    }

    public String getName() {
      return this.name;
    }

    public String getDisplayName() {
      return this.displayName;
    }

    public String getType() {
      return this.type;
    }
}
