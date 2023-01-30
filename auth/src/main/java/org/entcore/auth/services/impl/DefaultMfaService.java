package org.entcore.auth.services.impl;

import org.entcore.auth.services.MfaService;
import org.entcore.common.datavalidation.UserValidation;
import org.entcore.common.datavalidation.impl.AbstractDataValidationService;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.entcore.common.utils.Mfa;
import org.entcore.common.utils.StringUtils;

import fr.wseduc.webutils.Server;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import static org.entcore.common.datavalidation.utils.DataStateUtils.*;



public class DefaultMfaService implements MfaService {
    static Logger logger = LoggerFactory.getLogger(DefaultMfaService.class);

    /** Inner service to manage field "mfaState" */
    //---------------------------------------------------------------
    private class MfaField extends AbstractDataValidationService {
    //---------------------------------------------------------------
        MfaField(io.vertx.core.Vertx vertx, io.vertx.core.json.JsonObject config) {
            super("mfa", vertx, config);
        }

        public Future<JsonObject> getCurrentMfaState(final String userId, final boolean needMFA) {
            return (needMFA
                ? retrieveFullState(userId)
                : Future.succeededFuture(new JsonObject())
            )
            .compose( j -> {
                JsonObject state = j.getJsonObject(stateField);
    
                // Check business rules
                do {
                    if( state == null ) {
                        state = new JsonObject();
                        setState(state, needMFA ? OUTDATED : VALID);
                        break;
                    }
                    // if TTL or max tries reached, then don't check code
                    if (getState(state) == OUTDATED) {
                        break;
                    }
                    // Check time to live
                    Long ttl = getTtl(state);
                    if( ttl==null || ttl.compareTo(System.currentTimeMillis()) < 0 ) {
                        // TTL reached
                        setState(state, OUTDATED);
                        break;
                    }
                    // Check remaining tries
                    Integer tries = getTries(state);
                    if( tries==null || tries <= 0 ) {
                        setState(state, OUTDATED);
                        break;
                    }
                    // Current code is still pending.
                } while(false);
                return Future.succeededFuture(state);
            });
        }

        public JsonObject formatAsResponse(final int state, final Integer tries, final Long ttl) {
            return formatAsResponse(state, null, tries, ttl);
        }

        @Override
        public Future<JsonObject> startUpdate(String userId, String value, final long validDurationS, final int triesLimit) {
            // A new code is needed.
            final JsonObject state = new JsonObject();
            setState(state, PENDING);
            setKey(state, generateRandomCode());
            setTtl(state, System.currentTimeMillis() + validDurationS * 1000l);
            setTries(state, triesLimit);
            return updateState(userId, state); // TODO sendValidationMessage
        }

        @Override
        public Future<JsonObject> tryValidate(String userId, String code) {
            return getCurrentMfaState(userId, true)
            .compose( state -> {
                // Check business rules
                do {
                    if( state == null ) {
                        // Code is outdated
                        state = new JsonObject();
                        setState(state, OUTDATED);
                        break;
                    }
                    // if TTL or max tries reached, then don't check code
                    if (getState(state) == OUTDATED) {
                        break;
                    }
                    // Check code
                    String key = StringUtils.trimToNull( getKey(state) );
                    if( key == null || !key.equals(StringUtils.trimToNull(code)) ) {
                        // Invalid
                        Integer tries = getTries(state);
                        if(tries==null) {
                            tries = 0;
                        } else {
                            tries = Math.max(0, tries.intValue() - 1 );
                        }
                        if( tries <= 0 ) {
                            setState(state, OUTDATED);
                        }
                        setTries(state, tries);
                        break;
                    }

                    // ---Validation succeeded---
                    // Clean data in neo4j, and remember an MFA was done successfully.
                    return updateState(userId, null)
                    .map( unused -> {
                        return formatAsResponse(VALID, null, null);
                    });
                } while(false);

                // ---Validation results---
                return updateState(userId, state)
                .map( newState -> {
                    return formatAsResponse(getState(newState), getTries(newState), getTtl(newState));
                });
            });
        }

        @Override
        public Future<JsonObject> hasValid(String userId) {
            return Future.failedFuture("No data to check");
        }

        @Override
        public Future<Long> sendValidationMessage( final HttpServerRequest request, String email, JsonObject templateParams ) {
            return Future.failedFuture("not implemented yet");
        }
    }

    //---------------------------------------------------------------
    private MfaField mfaField = null;
    private EventBus eb = null;


    public DefaultMfaService(final Vertx vertx, final io.vertx.core.json.JsonObject config) {
        mfaField= new MfaField(vertx, config);
        eb = Server.getEventBus(vertx);
    }

    public Future<JsonObject> getOrStartMfa(final HttpServerRequest request, final JsonObject session, final UserInfos userInfos, final boolean forced) {
        if( Mfa.isNotActivatedForUser(userInfos) ) {
            // Mfa deactivated => error
            return Future.failedFuture("validate-mfa.error.not.active");
        }

        final boolean needMFA = Boolean.FALSE.equals( UserValidation.getIsMFA(null, session) );

        return (forced 
            ? Future.succeededFuture((JsonObject) null) // To generate a new code
            : mfaField.getCurrentMfaState(userInfos.getUserId(), needMFA)
        )
        .compose( state -> {
            if( state == null || getState(state) == OUTDATED ) {
                // A new code has to be generated.
                return mfaField.startUpdate( 
                    userInfos.getUserId(), 
                    null, 
                    UserValidation.getDefaultTtlInSeconds(), 
                    UserValidation.getDefaultRetryNumber()
                );
            } else {
                return Future.succeededFuture(state);
            }
        })
        .map( state -> {
            return mfaField.formatAsResponse(getState(state), getTries(state), getTtl(state));
        });
    }

    public Future<JsonObject> tryCode(final HttpServerRequest request, final UserInfos userInfos, final String key) {
        if( Mfa.isNotActivatedForUser(userInfos) ) {
            // Mfa deactivated => error
            return Future.failedFuture("validate-mfa.error.not.active");
        }
        return mfaField
        .tryValidate( userInfos.getUserId(), key )
        .map( result -> {
            if( result !=null && "valid".equalsIgnoreCase(result.getString("state")) ) {
                UserValidation.setIsMFA(eb, UserUtils.getSessionId(request).get(), true);
            }
            return result;
        });
    }
}