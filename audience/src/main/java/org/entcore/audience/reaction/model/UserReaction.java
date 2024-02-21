package org.entcore.audience.reaction.model;

public class UserReaction {
    private final String userId;
    private final  String profile;
    private final String reactionType;

    public UserReaction(String userId, String profile, String reactionType) {
        this.userId = userId;
        this.profile = profile;
        this.reactionType = reactionType;
    }

    public String getUserId() {
        return userId;
    }

    public String getProfile() {
        return profile;
    }

    public String getReactionType() {
        return reactionType;
    }
}
