package org.entcore.directory.services;

import io.vertx.core.Future;
import org.entcore.directory.dto.LinkDTO;

import java.util.List;
import java.util.UUID;


public interface UserLinkService {

    /** Outcome of a link creation. */
    enum CreateLinkResult {
        CREATED,
        /** The user has no free slot left : his links quota is reached */
        LIMIT_REACHED
    }

    /** Outcome of a link deletion. */
    enum DeleteLinkResult {
        DELETED,
        /** No such link for this user : unknown id, or link owned by somebody else */
        NOT_FOUND
    }

    /**
     * Create a new link for widget link utils attach to the user
     * @param link The link to create
     * @param userId the user
     * @return the result of the operation
     */
    Future<CreateLinkResult> createLink(LinkDTO link, String userId);

    /**
     * Get links for widget link utils attach to the user
     * @param userId the user
     * @return list of user's links
     */
    Future<List<LinkDTO>> getLinks(String userId);

    /**
     * Delete a link for widget link utils attach to the user
     * @param linkId id of the link to delete
     * @param userId the user
     * @return the result of the operation
     */
    Future<DeleteLinkResult> deleteLink(UUID linkId, String userId);

    /**
     * Delete every link belonging to the given users, when their accounts are removed
     * @param userIds the users whose links must be purged
     * @return completed once the links are deleted
     */
    Future<Void> deleteUserLinks(List<String> userIds);

}
