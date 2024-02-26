package org.entcore.audience.reaction.model;

import java.util.List;

public class ReactionDetailsResponse {
    private final ReactionCounters reactionCounters;

    private final List<UserReaction> userReactions;

    public ReactionDetailsResponse(ReactionCounters reactionCounters, List<UserReaction> userReactions) {
        this.reactionCounters = reactionCounters;
        this.userReactions = userReactions;
    }

    public ReactionCounters getReactionCounters() {
        return reactionCounters;
    }

    public List<UserReaction> getUserReactions() {
        return userReactions;
    }
}
