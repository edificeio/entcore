package org.entcore.common.explorer.to;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.entcore.common.user.UserInfos;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FolderDeleteRequest {
    /**
     * User that trigger action
     */
    private final String userId;
    /**
     * Id to delete in explorer
     */
    private final Set<Long> toDelete;
    /**
     * Application that trigger upsert
     */
    private final String application;

    @JsonCreator
    public FolderDeleteRequest(@JsonProperty("userId") final String userId,
                               @JsonProperty("toDelete") final Collection<Long> toDelete,
                               @JsonProperty("application") final String application) {
        this.userId = userId;
        this.toDelete = new HashSet<>(toDelete);
        this.application = application;
    }

    public FolderDeleteRequest(final UserInfos user, final Collection<Long> toDelete, final String application) {
        this(user.getUserId(), toDelete, application);
    }

    public String getApplication() {
        return application;
    }

    public Set<Long> getToDelete() {
        return toDelete;
    }

    public String getUserId() {
        return userId;
    }
}
