package org.entcore.common.explorer.to;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FolderResponse {
    /**
     * Id in the explorer.
     */
    private final Long id;
    /**
     * Current name of the folder
     */
    private final String name;
    /**
     * Current uuid in the ent.
     */
    private final String entId;
    /**
     * Current id of the parent.
     */
    private final Long parentId;
    /**
     * {@code true} if we folder is trashed.
     */
    private final Boolean trashed;
    /**
     * List of resources in this folder
     */
    private final List<String> entResourceIds;
    /**
     * Owner id of the folder
     */
    private final String ownerUserId;
    /**
     * Owner name of the folder
     */
    private final String ownerUserName;
    /**
     * Creation timestamp
     */
    private final Long created;
    /**
     * Modification timestamp
     */
    private final Long modified;

    @JsonCreator
    public FolderResponse(@JsonProperty("id") final Long id,
                          @JsonProperty("name") String name,
                          @JsonProperty("entId") final String entId,
                          @JsonProperty("parentId") final Long parentId,
                          @JsonProperty("trashed") final Boolean trashed,
                          @JsonProperty("entResourceIds") final List<String> entResourceIds,
                          @JsonProperty("ownerUserId") String ownerUserId,
                          @JsonProperty("ownerUserName") String ownerUserName,
                          @JsonProperty("created") Long created,
                          @JsonProperty("modified") Long modified) {
        this.id = id;
        this.name = name;
        this.entId = entId;
        this.parentId = parentId;
        this.trashed = trashed;
        this.entResourceIds = entResourceIds;
        this.ownerUserId = ownerUserId;
        this.ownerUserName = ownerUserName;
        this.created = created;
        this.modified = modified;
    }

    public Long getId() {
        return id;
    }

    public String getEntId() {
        return entId;
    }

    public Long getParentId() {
        return parentId;
    }

    public Boolean getTrashed() {
        return trashed;
    }

    public List<String> getEntResourceIds() {
        return entResourceIds;
    }

    public String getOwnerUserId() {
        return ownerUserId;
    }

    public String getOwnerUserName() {
        return ownerUserName;
    }

    public Long getCreated() {
        return created;
    }

    public Long getModified() {
        return modified;
    }

    public String getName() {
        return name;
    }
}
