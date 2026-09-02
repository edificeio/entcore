package org.entcore.directory.services;

import io.vertx.core.Future;
import org.entcore.directory.dto.LinkDTO;

import java.util.List;


public interface UserLinkService {

    class LinkOperationError {
        private int code;
        private String errorCode;

        public LinkOperationError(int code, String errorCode) {
            this.code = code;
            this.errorCode = errorCode;
        }

        public int getCode() {
            return code;
        }

        public void setCode(int code) {
            this.code = code;
        }

        public String getErrorCode() {
            return errorCode;
        }

        public void setErrorCode(String errorCode) {
            this.errorCode = errorCode;
        }
    }

    /**
     * Create a new link for widget link utils attach to the user
     * @param link The link to create
     * @param userId the user
     * @return the result of the operation
     */
    Future<LinkOperationError> createLink(LinkDTO link, String userId);

    /**
     * Get links for widget link utils attach to the user
     * @param userId the user
     * @return list of user's links
     */
    Future<List<LinkDTO>> getLinks(String userId);

    /**
     * Delete a link for widget link utils attach to the user
     * @param link The link to delete
     * @param userId the user
     * @return the result of the operation
     */
    Future<LinkOperationError> deleteLink(LinkDTO link, String userId);

    /**
     * Delete every link belonging to the given users, when their accounts are removed
     * @param userIds the users whose links must be purged
     * @return completed once the links are deleted
     */
    Future<Void> deleteUserLinks(List<String> userIds);

}
