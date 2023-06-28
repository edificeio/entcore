package org.entcore.directory.pojo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Additionnal informations used to determine the actions to take to merge duplicated users.
 */
public class MergeUsersMetadata {
    /** {@code true} if the remaining user should keep the other user's relationships.*/
    private final boolean keepRelations;

    @JsonCreator
    public MergeUsersMetadata(@JsonProperty("keepRelations") final boolean keepRelations) {
        this.keepRelations = keepRelations;
    }

    public boolean isKeepRelations() {
        return keepRelations;
    }
}
