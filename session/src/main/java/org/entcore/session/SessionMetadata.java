package org.entcore.session;

import io.vertx.core.json.JsonObject;

/**
 * Some of the metadata used to create a session.
 */
public class SessionMetadata {
    /** {@code true} if the user signed in from a secure location (session lives should be extended).*/
    private final boolean secureLocation;
    /**
     *  When present and positive, this value should override the ttl of a session defined in the underlying store. It
     *  is in milliseconds.
     * */
    private final Long ttl;

    public boolean isSecureLocation() {
        return secureLocation;
    }

    public Long getTtl() {
        return ttl;
    }

    public SessionMetadata(final boolean secureLocation, final Long ttl) {

        this.secureLocation = secureLocation;
        this.ttl = ttl;
    }

    public static SessionMetadata fromJsonObject(final JsonObject jsonObject) {
        final boolean sl;
        final Long ttl;
        if(jsonObject == null) {
            sl = false;
            ttl = null;
        } else {
            sl = jsonObject.getBoolean("secureLocation", false);
            ttl = jsonObject.getLong("ttl");
        }
        return new SessionMetadata(sl, ttl);
    }
}
