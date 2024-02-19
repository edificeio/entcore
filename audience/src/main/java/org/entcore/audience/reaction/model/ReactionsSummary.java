package org.entcore.audience.reaction.model;

import java.util.Map;

public class ReactionsSummary {
  private final Map<String, ReactionsSummaryForResource> reactionsByResource;

  public ReactionsSummary(Map<String, ReactionsSummaryForResource> reactionsByResource) {
    this.reactionsByResource = reactionsByResource;
  }

  public Map<String, ReactionsSummaryForResource> getReactionsByResource() {
    return reactionsByResource;
  }

  public static class ReactionsSummaryForResource {
    private final Map<String, Integer> countByType;

    public ReactionsSummaryForResource(Map<String, Integer> countByType) {
      this.countByType = countByType;
    }

    public Map<String, Integer> getCountByType() {
      return countByType;
    }
  }
}
