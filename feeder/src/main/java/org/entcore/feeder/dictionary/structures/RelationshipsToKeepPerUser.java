package org.entcore.feeder.dictionary.structures;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Summaries of Neo4J relationships coming in and out of nodes identified by an id.
 */
public class RelationshipsToKeepPerUser {
    /** Store associating the id of a node to its relationships. */
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

    /**
     *
     * @param nodeId Id of the node whose relationships we want
     * @return
     */
    public List<RelationshipToKeepForDuplicatedUser> getNodeRelationships(final String nodeId) {
        return relationshipPerUserId.getOrDefault(nodeId, Collections.emptyList());
    }

    /**
     * @param nodeId Id of the node at the other end of the relationship to check
     * @param type Type of the relation
     * @param otherNodeId Id of the node at the other end of the relationship
     * @param outgoing {@code true} if the relationship must be efferent
     * @return {@code true} if the user has a relationship with the other node of the type {@code type} in the same direction
     * {@code outgoing} and pointing out/from the node with the id {@code otherNodeId}
     */
    public boolean isUserHasRs(final String nodeId, final String type, final String otherNodeId, final boolean outgoing) {
        return relationshipPerUserId.getOrDefault(nodeId, Collections.emptyList()).stream()
        .anyMatch(rs -> rs.getType().equals(type) && rs.getOtherNodeId().equals(otherNodeId) && rs.isOutgoing() == outgoing);
    }
}
