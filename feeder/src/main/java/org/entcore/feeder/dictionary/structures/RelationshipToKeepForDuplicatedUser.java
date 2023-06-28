package org.entcore.feeder.dictionary.structures;

import io.vertx.core.json.JsonObject;

import java.util.Set;

public class RelationshipToKeepForDuplicatedUser {
    private final String otherNodeId;
    private final Set<String> otherNodeLabels;
    private final JsonObject properties;
    private final String type;
    private final boolean outgoing;

    public RelationshipToKeepForDuplicatedUser(final String otherNodeId, final Set<String> otherNodeLabels,
                                               final JsonObject properties, final String type,
                                               final boolean outgoing) {
        this.otherNodeId = otherNodeId;
        this.otherNodeLabels = otherNodeLabels;
        this.properties = properties;
        this.type = type;
        this.outgoing = outgoing;
    }

    public String getOtherNodeId() {
        return otherNodeId;
    }

    public JsonObject getProperties() {
        return properties;
    }

    public String getType() {
        return type;
    }

    public boolean isOutgoing() {
        return outgoing;
    }

    public Set<String> getOtherNodeLabels() {
        return otherNodeLabels;
    }

    @Override
    public String toString() {
        return "RelationshipToKeepForDuplicatedUser{" +
                "otherNodeId='" + otherNodeId + '\'' +
                ", otherNodeLabels='" + otherNodeLabels + '\'' +
                ", properties=" + properties +
                ", type='" + type + '\'' +
                ", outoing=" + outgoing +
                '}';
    }
}
