package org.entcore.common.audience.to;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties
public class AudienceCheckRightResponseMessage {
  private final boolean success;
  private final boolean access;
  private final String errorMsg;

  @JsonCreator
  public AudienceCheckRightResponseMessage(@JsonProperty("success") final boolean success,
                                           @JsonProperty("access") final boolean access,
                                           @JsonProperty("errorMsg") final String errorMsg) {
    this.success = success;
    this.access = access;
    this.errorMsg = errorMsg;
  }

  public AudienceCheckRightResponseMessage(final String errorMsg) {
    this(false, false, errorMsg);
  }

  public AudienceCheckRightResponseMessage(final boolean access) {
    this(true, access, null);
  }

  public boolean isSuccess() {
    return success;
  }

  public boolean isAccess() {
    return access;
  }

  public String getErrorMsg() {
    return errorMsg;
  }
}
