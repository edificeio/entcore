package org.entcore.audience.services.impl;

import io.vertx.core.Future;
import org.entcore.audience.services.AudienceService;

import java.util.Set;

public class AudienceServiceImpl implements AudienceService {
    @Override
    public Future<Void> deleteUsers(Set<String> userIds) {
        return null;
    }

    @Override
    public Future<Void> mergeUsers(String keptdUserId, String deletedUserId) {
        return null;
    }
}
