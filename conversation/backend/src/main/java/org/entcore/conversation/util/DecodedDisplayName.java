package org.entcore.conversation.util;

import java.util.Optional;

import org.entcore.common.user.UserUtils;
import static org.entcore.common.utils.StringUtils.isEmpty;

/**
 * Utility class to decode display names stored in the database.
 */
public class DecodedDisplayName {
    private String id;
    private String displayName;
    private boolean isGroup;

    /**
     * Gets the ID of the decoded display name.
     * @return the ID
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the display name.
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Checks if this is the display name of a group.
     * @return true for a group, false otherwise
     */
    public boolean ofGroup() {
        return isGroup;
    }

    /**
     * Checks if this is the display name of a user.
     * @return true for a user, false otherwise
     */
    public boolean ofUser() {
        return !isGroup;
    }

    /**
     * Decodes a display name stored in the database.
     * @param dbEncodedDisplayName the encoded display name
     * @param lang the language
     * @return an Optional containing the decoded display name if successful, otherwise an empty Optional
     */
    static public Optional<DecodedDisplayName> decode(String dbEncodedDisplayName, String lang) {
        if( isEmpty(dbEncodedDisplayName) ) {
            return Optional.empty();
        }

        final String[] a = dbEncodedDisplayName.split("\\$");
        if( a.length != 4 ) {
            return Optional.empty();
        }
        final boolean isGroup = !isEmpty(a[2]);

        return Optional.of( isGroup 
            ? newGroupDisplayName(a[0], UserUtils.groupDisplayName(a[2], isEmpty(a[3]) ? null : a[3], lang)) 
            : newUserDisplayName(a[0], a[1])
        );
    }

    /**
     * Creates a new DecodedDisplayName of a user.
     * @param id the user ID
     * @param displayName the display name
     * @return the new DecodedDisplayName
     */
    private static DecodedDisplayName newUserDisplayName(String id, String displayName) {
        DecodedDisplayName name = new DecodedDisplayName();
        name.id = id;
        name.displayName = displayName;
        name.isGroup = false;
        return name;
    }

    /**
     * Creates a new DecodedDisplayName of a group.
     * @param id the group ID
     * @param displayName the display name
     * @return the new DecodedDisplayName
     */
    private static DecodedDisplayName newGroupDisplayName(String id, String displayName) {
        DecodedDisplayName name = new DecodedDisplayName();
        name.id = id;
        name.displayName = displayName;
        name.isGroup = true;
        return name;
    }
}