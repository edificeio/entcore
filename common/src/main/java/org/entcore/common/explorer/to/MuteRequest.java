package org.entcore.common.explorer.to;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.entcore.common.explorer.IdAndVersion;

import java.util.Set;

/**
 * Payload of a request to change the mute status of a resource for the caller.
 */
@JsonIgnoreProperties
public class MuteRequest {
    /** ENT ids of the resources to mute and the version in which they were when the user tried to mute them. */
    private final Set<IdAndVersion> resourceIds;
    /** {@code true} if the user doesn't want to receive notifications for the resources.**/
    private final boolean mute;

    @JsonCreator
    public MuteRequest(@JsonProperty("mute") final boolean mute,
                       @JsonProperty("resourceIds") Set<IdAndVersion> resourceIds) {
        this.mute = mute;
        this.resourceIds = resourceIds;
    }

    public boolean isMute() {
        return mute;
    }

    public Set<IdAndVersion> getResourceIds() {
        return resourceIds;
    }
}
