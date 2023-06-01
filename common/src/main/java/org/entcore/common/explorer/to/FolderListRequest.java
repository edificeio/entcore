package org.entcore.common.explorer.to;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.entcore.common.user.UserInfos;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FolderListRequest {
    /**
     * User id that trigger action
     */
    private final String userId;
    /**
     * User name that trigger action
     */
    private final String userName;
    /**
     * Application that initiated request
     */
    private final String application;

    @JsonCreator
    public FolderListRequest(@JsonProperty("userId") final String userId,
                             @JsonProperty("userName") final String userName,
                             @JsonProperty("application") final String application) {
        this.userId = userId;
        this.userName = userName;
        this.application = application;
    }

    public FolderListRequest(final UserInfos user, final String application) {
        this(user.getUserId(), user.getUsername(), application);
    }

    public String getApplication() {
        return application;
    }

    public String getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }
}
