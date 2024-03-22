package org.entcore.audience.reaction.model;

public class UserReaction {
    private final String userId;
    private final  String profile;
    private final String reactionType;
    private final String displayName;

    public UserReaction(String userId, String profile, String reactionType, String displayName) {
        this.userId = userId;
        this.profile = profile;
        this.reactionType = reactionType;
        this.displayName = displayName;
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

    public String getDisplayName() {
        return displayName;
    }
}
