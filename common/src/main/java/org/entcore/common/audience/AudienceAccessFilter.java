package org.entcore.common.audience;

import java.util.Set;

import io.vertx.core.Future;
import org.entcore.common.user.UserInfos;

/**
 * A service that checks whether a user has access to a set of resources
 * identified by their ids.
 */
public interface AudienceAccessFilter {

    /**
     * Checks whether the user has access to <b>all</b> the specified resources.
     * 
     * @param user        The user desiring to access the resources
     * @param resourceIds Ids of the resources to access
     * @return A {@code Future} that succeeds if the check was successful and can be
     *         trusted (otherwise the response
     *         cannot be considered as a sign that the user cannot access the
     *         resources). If the response is {@code true},
     *         the user can access <b>all</b> the resources, otherwise, <b>at least
     *         one</b> of the resources cannot be accessed.
     */
    Future<Boolean> canAccess(final UserInfos user, Set<String> resourceIds);

}
