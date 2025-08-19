package org.entcore.broker.proxy;

import io.vertx.core.Future;
import org.entcore.broker.api.BrokerPublisher;
import org.entcore.broker.api.dto.directory.GroupMembersChangedDTO;
import org.entcore.broker.api.dto.directory.GroupsDeletedDTO;
import org.entcore.broker.api.dto.directory.UsersDeletedDTO;

/**
 * Interface for publishing directory-related events to the broker.
 * This interface allows applications to notify other components when
 * users or groups are deleted from the directory or when group membership changes.
 */
public interface DirectoryBrokerPublisher {
    
    /**
     * Notifies subscribers that groups have been deleted from the directory.
     * 
     * @param notification DTO containing the IDs of deleted groups
     * @return Future indicating completion of the notification
     */
    @BrokerPublisher(subject = "directory.groups.deleted")
    Future<Void> notifyGroupsDeleted(GroupsDeletedDTO notification);
    
    /**
     * Notifies subscribers that users have been deleted from the directory.
     * 
     * @param notification DTO containing the IDs of deleted users
     * @return Future indicating completion of the notification
     */
    @BrokerPublisher(subject = "directory.users.deleted")
    Future<Void> notifyUsersDeleted(UsersDeletedDTO notification);
    
    /**
     * Notifies subscribers that the membership of a group has changed.
     * This includes both current members and members who have been removed.
     * 
     * @param notification DTO containing the group info, current members, and removed member IDs
     * @return Future indicating completion of the notification
     */
    @BrokerPublisher(subject = "directory.group.members.changed")
    Future<Void> notifyGroupMembersChanged(GroupMembersChangedDTO notification);
}