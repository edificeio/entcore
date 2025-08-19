package org.entcore.broker.api.dto.directory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.List;

/**
 * DTO representing a notification about deleted users
 */
public class UsersDeletedDTO {
    /**
     * List of user IDs that have been deleted
     */
    private final List<String> userIds;

    /**
     * Creates a new UsersDeletedDTO
     * 
     * @param userIds List of IDs of deleted users
     */
    @JsonCreator
    public UsersDeletedDTO(@JsonProperty("userIds") List<String> userIds) {
        this.userIds = userIds;
    }

    /**
     * Creates a new UsersDeletedDTO for a single user
     * 
     * @param userId ID of the deleted user
     * @return A new UsersDeletedDTO with a single user ID
     */
    public static UsersDeletedDTO forSingleUser(String userId) {
        return new UsersDeletedDTO(Collections.singletonList(userId));
    }

    /**
     * @return List of user IDs that have been deleted
     */
    public List<String> getUserIds() {
        return userIds;
    }
    
    @Override
    public String toString() {
        return "UsersDeletedDTO{" +
                "userIds=" + userIds +
                '}';
    }
}