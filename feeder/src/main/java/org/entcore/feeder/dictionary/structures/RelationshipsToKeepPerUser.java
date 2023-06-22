package org.entcore.feeder.dictionary.structures;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RelationshipsToKeepPerUser {
    private final Map<String, List<RelationshipToKeepForDuplicatedUser>> relationshipPerUserId;

    public RelationshipsToKeepPerUser() {
        relationshipPerUserId = new HashMap<>();
    }
    public RelationshipsToKeepPerUser addUserRelationship(final String userId, final RelationshipToKeepForDuplicatedUser rs) {
        relationshipPerUserId.compute(userId, (key, rels) -> {
            if(rels == null) {
                rels = new ArrayList<>();
            }
            rels.add(rs);
            return rels;
        });
        return this;
    }

    public List<RelationshipToKeepForDuplicatedUser> getUserRelationship(final String userId) {
        return relationshipPerUserId.getOrDefault(userId, Collections.emptyList());
    }

    /**
     * @param userId
     * @param type
     * @param otherNodeId
     * @param outgoing {@code true} if the relationship must be efferent
     * @return {@code true} if the user has a relationship with the other node of the type {@code type} in the same direction
     * {@code outgoing}
     */
    public boolean isUserHasRs(final String userId, final String type, final String otherNodeId, final boolean outgoing) {
        return relationshipPerUserId.getOrDefault(userId, Collections.emptyList()).stream()
        .anyMatch(rs -> rs.getType().equals(type) && rs.getOtherNodeId().equals(otherNodeId) && rs.isOutoing() == outgoing);
    }
}
