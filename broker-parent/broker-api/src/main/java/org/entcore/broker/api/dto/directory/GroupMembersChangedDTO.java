package org.entcore.broker.api.dto.directory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * DTO representing a notification about changes in group membership.
 * This includes both current members and the IDs of members who have been removed.
 */
public class GroupMembersChangedDTO {
    /**
     * ID of the group whose membership has changed
     */
    private final String groupId;
    
    /**
     * Name of the group whose membership has changed
     */
    private final String groupName;
    
    /**
     * List of current members in the group after the change
     */
    private final List<UserDTO> currentMembers;
    
    /**
     * List of user IDs that have been removed from the group
     */
    private final List<String> removedMemberIds;

    /**
     * Creates a new GroupMembersChangedDTO
     * 
     * @param groupId ID of the group whose membership has changed
     * @param groupName Name of the group whose membership has changed
     * @param currentMembers List of current members in the group after the change
     * @param removedMemberIds List of user IDs that have been removed from the group
     */
    @JsonCreator
    public GroupMembersChangedDTO(
            @JsonProperty("groupId") String groupId,
            @JsonProperty("groupName") String groupName,
            @JsonProperty("currentMembers") List<UserDTO> currentMembers,
            @JsonProperty("removedMemberIds") List<String> removedMemberIds) {
        this.groupId = groupId;
        this.groupName = groupName;
        this.currentMembers = currentMembers;
        this.removedMemberIds = removedMemberIds;
    }

    /**
     * @return ID of the group whose membership has changed
     */
    public String getGroupId() {
        return groupId;
    }

    /**
     * @return Name of the group whose membership has changed
     */
    public String getGroupName() {
        return groupName;
    }

    /**
     * @return List of current members in the group after the change
     */
    public List<UserDTO> getCurrentMembers() {
        return currentMembers;
    }

    /**
     * @return List of user IDs that have been removed from the group
     */
    public List<String> getRemovedMemberIds() {
        return removedMemberIds;
    }
    
    @Override
    public String toString() {
        return "GroupMembersChangedDTO{" +
                "groupId='" + groupId + '\'' +
                ", groupName='" + groupName + '\'' +
                ", currentMembers=" + currentMembers +
                ", removedMemberIds=" + removedMemberIds +
                '}';
    }
}