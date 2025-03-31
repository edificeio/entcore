package org.entcore.common.http;

import fr.wseduc.webutils.request.CookieHelper;
import fr.wseduc.webutils.request.filter.AbstractBasicFilter;
import fr.wseduc.webutils.request.filter.AbstractQueryParamTokenFilter;
import fr.wseduc.webutils.request.filter.UserAuthFilter;
import fr.wseduc.webutils.security.SecureHttpServerRequest;
import fr.wseduc.webutils.security.oauth.OAuthResourceProvider;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;

import java.util.UUID;

public class UserAuthWithQueryParamFilter extends UserAuthFilter {
    private final JsonObject config;

    public UserAuthWithQueryParamFilter(
			OAuthResourceProvider oauth, 
            AbstractBasicFilter basicFilter, 
            AbstractQueryParamTokenFilter paramFilter,
            Vertx vertx,
            JsonObject config ) {
		super(oauth, basicFilter, paramFilter);
        this.config = config;
        this.setVertx( vertx );
	}

    @Override
    protected void checkRecreateSession( final SecureHttpServerRequest request, String oneSessionId,
			String userId, final Handler<Boolean> handler) {
        boolean shouldRecreate = true;
        Future<Boolean> dropIfNeeded = null;
        UserInfos infos = null;

        if( oneSessionId != null 
                && !oneSessionId.trim().isEmpty()
                && (infos = UserUtils.sessionToUserInfos(request.getSession())) != null
                && userId!=null ) {
            // If a session is already active
            if( !userId.equals(infos.getUserId()) ) {
                // If it doesn't match the token info, drop it.
                dropIfNeeded = dropSession( oneSessionId );
            } else {
                // User session is matching the token, nothing to do.
                shouldRecreate = false;
            }
        } else {
            dropIfNeeded = Future.succeededFuture(true);
        }

        if( !shouldRecreate ) {
            handler.handle(true);
        } else {
            dropIfNeeded
            .compose( ok -> {
                if( ok!=null && ok.booleanValue() ) {
                    return createUserSession(userId, false);
                } else {
                    return Future.failedFuture("ko");
                }
            })
            .onSuccess( newSessionId -> {
                long timeout = config.getLong("cookie_timeout", Long.MIN_VALUE);
                CookieHelper.getInstance().setSigned("oneSessionId", newSessionId, timeout, request);
                CookieHelper.set("authenticated", "true", timeout, request);
                //create xsrf token on create session to avoid cache issue
                if(config.getBoolean("xsrfOnAuth", true)){
                    CookieHelper.set("XSRF-TOKEN", UUID.randomUUID().toString(), timeout, request);
                }
                UserUtils.getSession(vertx.eventBus(), newSessionId, session -> {
                    if( session != null ) {
                        request.setSession( session );
                    }
                    // Bypass standard filters (user session is now initialized)
                    handler.handle(true);
                });
            })
            .onFailure( f -> {
                handler.handle(false);
            });
        }
    }

	private Future<Boolean> dropSession(String sessionId) {
		Promise<Boolean> promise = Promise.promise();
        UserUtils.deleteSession(vertx.eventBus(), sessionId, res -> {
            promise.complete( res );
        });
		return promise.future();
	}

	private Future<String> createUserSession(String userId, boolean secureLocation) {
		Promise<String> promise = Promise.promise();
        UserUtils.createSession(vertx.eventBus(), userId, secureLocation, res -> {
            promise.complete( res );
        });
		return promise.future();
	}
}
