package org.entcore.common.emailstate;

import io.vertx.core.json.JsonObject;

/**
 * Accessors for the "emailState" of a Neo4j User.
 * @see {@link org.entcore.EmailValidationService.services.MailValidationService MailValidationService}
 */
public class EmailStateUtils {
    static public final int OUTDATED = -1;
    static public final int UNCHECKED = 0;
    static public final int PENDING = 1;
    static public final int VALID = 2;

    static private final String STATE_FIELD="state";
    static private final String VALID_FIELD="valid";
    static private final String PENDING_FIELD="pending";
    static private final String KEY_FIELD="key";
    static private final String TTL_FIELD="ttl";
    static private final String TRIES_FIELD="tries";

    /** Email field state */
    static public int getState(final JsonObject json) {
        return json==null ? UNCHECKED : json.getInteger(STATE_FIELD, UNCHECKED);
    }
    static public void setState(final JsonObject json, final int state) {
        json.put(STATE_FIELD, state);
    }
    /** Last known valid email address, or empty string */
    static public String getValid(final JsonObject json) {
        return json==null ? "" : json.getString(VALID_FIELD, "");
    }
    static public void setValid(final JsonObject json, final String validMail) {
        if(validMail==null) {
            json.put(VALID_FIELD, "");
        } else {
            json.put(VALID_FIELD, validMail);
        }
    }
    /** Email address to check */
    static public String getPending(final JsonObject json) {
        return json==null ? null : json.getString(PENDING_FIELD);
    }
    static public void setPending(final JsonObject json, final String pendingMail) {
        if(pendingMail==null) {
            json.remove(PENDING_FIELD);
        } else {
            json.put(PENDING_FIELD, pendingMail);
        }
    }
    /** Generated code to check */
    static public String getKey(final JsonObject json) {
        return json==null ? null : json.getString(KEY_FIELD);
    }
    static public void setKey(final JsonObject json, final String key) {
        if(key==null) {
            json.remove(KEY_FIELD);
        } else {
            json.put(KEY_FIELD, key);
        }
    }
    /** Limit date for checking */
    static public Long getTtl(final JsonObject json) {
        return json==null ? null : json.getLong(TTL_FIELD);
    }
    static public void setTtl(final JsonObject json, final Long ttl) {
        if(ttl==null || ttl<0l) {
            json.remove(TTL_FIELD);
        } else {
            json.put(TTL_FIELD, ttl);
        }
    }
    /** Limit retries number for checking */
    static public Integer getTries(final JsonObject json) {
        return json==null ? null : json.getInteger(TRIES_FIELD);
    }
    static public void setTries(final JsonObject json, final Integer tries) {
        if(tries==null || tries<0l) {
            json.remove(TRIES_FIELD);
        } else {
            json.put(TRIES_FIELD, tries);
        }
    }

    static public String stateToString(final int state) {
        switch( state ) {
			case VALID:     return "valid";
			case PENDING:   return "pending";
			case OUTDATED:  return "outdated";
			default:        return "unchecked";
		}
    }

    static public int ttlToRemainingSeconds(final long ttl) {
        return Math.max(0, Math.round((ttl-System.currentTimeMillis()) / 1000l));
    }

    /** 
     * Use before returning data to client apps.
     * Remove or transform data considered server-only : 
     * - key (removed),
     * - state (formatted as string),
     * - ttl (optional field, converted to seconds remaining before state becomes outdated)
     * @param emailState not usable anymore with EmailStateUtils after this call.
     */
    static public void formatAsResponse(final JsonObject emailState) {
        if( emailState != null ) {
            emailState.remove(KEY_FIELD);
            emailState.put(STATE_FIELD, stateToString(getState(emailState)));
            Long ttl = getTtl(emailState);
            if( ttl!=null ) {
                emailState.put(TTL_FIELD, ttlToRemainingSeconds(ttl.longValue()) );
            }
        }
    }
}
