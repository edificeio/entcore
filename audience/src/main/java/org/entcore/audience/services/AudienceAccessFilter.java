package org.entcore.audience.services;

import io.vertx.core.Future;
import org.entcore.common.user.UserInfos;

import java.util.Set;

/**
 * A service that checks whether a user has access to a set of resources
 * identified by their ids.
 */
public interface AudienceAccessFilter {

    /**
     * Checks whether the user has access to <b>all</b> the specified resources.
     *
     * @param module Name of the application which owns the resources to check
     * @param resourceType Type of the resources to check
     * @param user        The user desiring to access the resources
     * @param resourceIds Ids of the resources to access
     * @return A {@code Future} that succeeds if the check was successful and can be
     *         trusted (otherwise the response
     *         cannot be considered as a sign that the user cannot access the
     *         resources). If the response is {@code true},
     *         the user can access <b>all</b> the resources, otherwise, <b>at least
     *         one</b> of the resources cannot be accessed.
     */
    Future<Boolean> canAccess(final String module,
                              final String resourceType,
                              final UserInfos user,
                              final Set<String> resourceIds);

}
