package org.entcore.common.explorer.to;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.entcore.common.user.UserInfos;

import java.util.Collections;
import java.util.List;

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
    /**
     * GroupIds of the user
     */
    private final List<String> groupIds;

    @JsonCreator
    public FolderListRequest(@JsonProperty("userId") final String userId,
                             @JsonProperty("userName") final String userName,
                             @JsonProperty("application") final String application,
                             @JsonProperty("groupIds") final List<String> groupIds) {
        this.userId = userId;
        this.userName = userName;
        this.application = application;
        this.groupIds = groupIds;
    }

    public FolderListRequest(final UserInfos user, final String application) {
        this(user.getUserId(), user.getUsername(), application, user.getGroupsIds());
    }

    public List<String> getGroupIds() {
        return groupIds;
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
