package org.entcore.common.explorer.to;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.entcore.common.user.UserInfos;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FolderUpsertRequest {
    /**
     * Id in the explorer.
     */
    private final Long id;
    /**
     * Current id of the parent.
     */
    private final Long parentId;
    /**
     * name of the folder
     */
    private final String name;
    /**
     * {@code true} if we folder is trashed.
     */
    private final Boolean trashed;
    /**
     * User id that trigger action
     */
    private final String userId;
    /**
     * User name that trigger action
     */
    private final String userName;
    /**
     * Application that trigger upsert
     */
    private final String application;

    @JsonCreator
    public FolderUpsertRequest(@JsonProperty("userId") final String userId,
                               @JsonProperty("userName") final String userName,
                               @JsonProperty("id") final Long id,
                               @JsonProperty("parentId") final Long parentId,
                               @JsonProperty("name") String name,
                               @JsonProperty("trashed") final Boolean trashed,
                               @JsonProperty("application") String application) {
        this.id = id;
        this.parentId = parentId;
        this.name = name;
        this.trashed = trashed;
        this.userId = userId;
        this.userName = userName;
        this.application = application;
    }

    public FolderUpsertRequest(final UserInfos user, final Long id, final Long parentId, final Boolean trashed, String application, String name) {
        this(user.getUserId(), user.getUsername(), id, parentId, name, trashed, application);
    }

    public FolderUpsertRequest(final UserInfos user, final Long parentId, String application, String name) {
        this(user.getUserId(), user.getUsername(), null, parentId, name, false, application);
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

    public Long getId() {
        return id;
    }

    public Long getParentId() {
        return parentId;
    }

    public Boolean getTrashed() {
        return trashed;
    }

    public String getName() {
        return name;
    }
}
