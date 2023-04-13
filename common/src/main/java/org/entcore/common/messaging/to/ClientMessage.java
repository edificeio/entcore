package org.entcore.common.messaging.to;

/**
 * Messages that can be handled by {@code IMessagingClient}.
 */
public class ClientMessage {
    /**
     * Identifier of the user whose action generated the message.
     * Generally a userId but can be in some cases a default "system" user.
     */
    private final String originator;
    /** Timestamp of the creation of this message. */
    private final long creationTime;

    protected ClientMessage(final String originator, final long creationTime) {
        this.originator = originator;
        this.creationTime = creationTime;
    }

    public String getOriginator() {
        return originator;
    }

    public long getCreationTime() {
        return creationTime;
    }
}
