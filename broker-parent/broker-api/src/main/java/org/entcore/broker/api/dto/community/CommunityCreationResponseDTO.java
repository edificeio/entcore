package org.entcore.broker.api.dto.community;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.entcore.broker.api.dto.BaseResponseDTO;

public class CommunityCreationResponseDTO extends BaseResponseDTO {
  private final String id;
  private final boolean read;
  private final boolean contrib;
  private final boolean manager;

  @JsonCreator
  public CommunityCreationResponseDTO(@JsonProperty("id") final String id,
                                      @JsonProperty("read") final boolean read,
                                      @JsonProperty("contrib") final boolean contrib,
                                      @JsonProperty("manager") final boolean manager) {
    super(true, null);
    this.id = id;
    this.read = read;
    this.contrib = contrib;
    this.manager = manager;
  }

  public String getId() {
    return id;
  }

  public boolean isRead() {
    return read;
  }

  public boolean isContrib() {
    return contrib;
  }

  public boolean isManager() {
    return manager;
  }
}
