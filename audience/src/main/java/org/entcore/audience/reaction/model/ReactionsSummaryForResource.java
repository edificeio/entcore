package org.entcore.audience.reaction.model;

import java.util.Set;

public class ReactionsSummaryForResource {
  private final Set<String> reactionTypes;
  private final String userReaction;
  private final int totalReactionsCounter;

  public ReactionsSummaryForResource(Set<String> reactionTypes, String userReaction, int totalReactionsCounter) {
    this.reactionTypes = reactionTypes;
    this.userReaction = userReaction;
    this.totalReactionsCounter = totalReactionsCounter;
  }


  public Set<String> getReactionTypes() {
    return reactionTypes;
  }

  public String getUserReaction() {
    return userReaction;
  }
  public int getTotalReactionsCounter() {
    return totalReactionsCounter;
  }
}
