package org.entcore.audience.reaction.model;

import java.util.Map;

public class ReactionsSummaryResponse {
    private final Map<String, ReactionsSummaryForResource> reactionsByResource;

    public ReactionsSummaryResponse(Map<String, ReactionsSummaryForResource> reactionsByResource) {
        this.reactionsByResource = reactionsByResource;
    }

    public Map<String, ReactionsSummaryForResource> getReactionsByResource() {
        return reactionsByResource;
    }
}
