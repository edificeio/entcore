package org.entcore.common.conversation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class LegacySearchVisibleRequest {
  private final String userId;
  private final String search;
  private final String language;
  private final String parentMessageId;

  @JsonCreator
  public LegacySearchVisibleRequest(@JsonProperty("userId") final String userId,
                                    @JsonProperty("search") final String search,
                                    @JsonProperty("language") final String language,
                                    @JsonProperty("parentMessageId") final String parentMessageId) {
    this.userId = userId;
    this.search = search;
    this.language = language;
    this.parentMessageId = parentMessageId;
  }

  public String getUserId() {
    return userId;
  }

  public String getSearch() {
    return search;
  }

  public String getLanguage() {
    return language;
  }

  public String getParentMessageId() {
    return parentMessageId;
  }
}
