package org.entcore.broker.api.dto.directory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.List;

/**
 * DTO representing a notification about deleted groups
 */
public class GroupsDeletedDTO {
    /**
     * List of group IDs that have been deleted
     */
    private final List<String> groupIds;

    /**
     * Creates a new GroupsDeletedDTO
     * 
     * @param groupIds List of IDs of deleted groups
     */
    @JsonCreator
    public GroupsDeletedDTO(@JsonProperty("groupIds") List<String> groupIds) {
        this.groupIds = groupIds;
    }

    /**
     * Creates a new GroupsDeletedDTO for a single group
     * 
     * @param groupId ID of the deleted group
     * @return A new GroupsDeletedDTO with a single group ID
     */
    public static GroupsDeletedDTO forSingleGroup(String groupId) {
        return new GroupsDeletedDTO(Collections.singletonList(groupId));
    }

    /**
     * @return List of group IDs that have been deleted
     */
    public List<String> getGroupIds() {
        return groupIds;
    }
    
    @Override
    public String toString() {
        return "GroupsDeletedDTO{" +
                "groupIds=" + groupIds +
                '}';
    }
}