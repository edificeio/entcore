package org.entcore.common.http.filter;

import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.filter.Filter;
import fr.wseduc.webutils.security.SecureHttpServerRequest;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;

import org.entcore.common.emailstate.EmailState;
import org.entcore.common.http.response.DefaultPages;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.entcore.common.utils.StringUtils;

import static org.entcore.common.emailstate.EmailState.FIELD_MUST_VALIDATE_TERMS;
import static org.entcore.common.emailstate.EmailState.FIELD_MUST_VALIDATE_EMAIL;

import java.net.URI;
import java.net.URLEncoder;
import java.util.Map;

/**
 * This filter checks if the user needs to be redirected to must perform some mandatory validation before processing.
 * For example :
 * - validate Terms of Use     => redirect to /auth/revalidate-terms
 * - validate an email address => redirect to /auth/validate-mail
 */
public class MandatoryUserValidationFilter implements Filter {
    private final EventBus eventBus;
    
    private final static int    TERMS_OF_USE_IDX  = 0;
    private final static int    EMAIL_ADDRESS_IDX = 1;
    private final static String[] whiteListAlways = {
        "/directory/user/mailstate", "/auth/oauth2/token"
    };
    private final static String[][] whiteListByStep = {
        {"/auth/revalidate-terms"},
        {"/auth/validate-mail", "/internal/userinfo", "/oauth2/userinfo", 
         "/userbook/", "/theme"}
    };

    private final static String REDIRECT_TO_KEY = "MandatoryUserValidationFilterRedirectsTo";

    public MandatoryUserValidationFilter(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    public void canAccess(HttpServerRequest request, Handler<Boolean> handler) {
        // Ajax requests are not redirected (useless)
        if ("XMLHttpRequest".equals(request.headers().get("X-Requested-With"))){
            handler.handle(true);
            return;
        }

        UserUtils.getUserInfos(this.eventBus, request, userInfos -> {
            if (userInfos == null) {
                // Mandatory validations for disconnected users :
                // - none !
                handler.handle(true);
            } else {
                if (!(request instanceof SecureHttpServerRequest)) {
                    // Only @SecuredAction annotated methods can be filtered !
                    // But well-known public URL requiring user validation should be blacklisted here.
                    handler.handle(true);
                    return;
                }
                final SecureHttpServerRequest sreq = (SecureHttpServerRequest) request;
                // Chained mandatory validations for connected users.
                // A failure will deny the filter and then cause a redirection.
                request.pause();
                EmailState.getMandatoryUserValidation(this.eventBus, userInfos.getUserId())
                .compose( validations -> {
                    return checkTermsOfUse(sreq, userInfos, validations);
                })
                .compose( validations -> {
                    return checkEmailAddress(sreq, userInfos, validations);
                })
                .onComplete( ar -> {
                    request.resume();
                    if( ar.succeeded() ) {
                        handler.handle(true);
                    } else {
                        // Memorize where to redirect this request
                        sreq.setAttribute(REDIRECT_TO_KEY, ar.cause().getMessage());
                        handler.handle(false);
                    }
                });
            }
        });
    }

    private boolean isInWhiteList(final String path, final String methodName, final int step) {
        for( int j=0; j<whiteListAlways.length; j++ ) {
            if( path.contains(whiteListAlways[j]) ){
                return true;
            }
        }
        // At every step we must also check the white list of the previous steps URLs, 
        // to avoid deadlocks between filters (but only for GET requests)
        final boolean isGET = methodName.equalsIgnoreCase("GET");
        for( int i=0; isGET && i<=step; i++ ) {
            final String[] whiteList = whiteListByStep[i];
            for( int j=0; j<whiteList.length; j++ ) {
                if( path.contains(whiteList[j]) ){
                    return true;
                }
            }
        }
        return false;
    }

    private Future<JsonObject> checkTermsOfUse(final SecureHttpServerRequest request, UserInfos userInfos, JsonObject validations) {
        if( Boolean.FALSE.equals(validations.getBoolean(FIELD_MUST_VALIDATE_TERMS, false)) // No need to revalidate => OK
            || request.headers().contains("Authorization") // Clients with Authorization header have terms of use validated beforehand => OK
            || isInWhiteList(request.path(), request.method().name(), TERMS_OF_USE_IDX)    // White-listed url reqquested => OK
        ) {
            return Future.succeededFuture(validations); 
        }
        // KO
        return Future.failedFuture("/auth/revalidate-terms"); // Where to redirect (must be white-listed !)
    }

    private Future<JsonObject> checkEmailAddress(final SecureHttpServerRequest request, UserInfos userInfos, JsonObject validations) {
        if( Boolean.FALSE.equals(validations.getBoolean(FIELD_MUST_VALIDATE_EMAIL, false)) // No need to revalidate => OK
            || (
                isInWhiteList(request.path(), request.method().name(), EMAIL_ADDRESS_IDX)  // white-listed url => OK
                   && !request.path().contains("mon-compte") // OK but these restrictions
                   && !request.path().contains("annuaire")   // OK but these restrictions
            )
        ) {
            return Future.succeededFuture(validations);
        }
        // KO
        String url = "/auth/validate-mail?force=true"; // Where to redirect (must be white-listed !)
        try {
            url = url +"&redirect="+ URLEncoder.encode(request.absoluteURI(), "UTF-8");
        } catch( Exception e ) {
            // silent failure
        }
        return Future.failedFuture(url);
    }

    @Override
    public void deny(HttpServerRequest request) {
		if (!(request instanceof SecureHttpServerRequest)) {
			request.response().setStatusCode(401).setStatusMessage("Unauthorized")
				.putHeader("content-type", "text/html").end(DefaultPages.UNAUTHORIZED.getPage());
		} else {
            final SecureHttpServerRequest sreq = (SecureHttpServerRequest) request;
            String to = sreq.getAttribute(REDIRECT_TO_KEY);
            // Failure due to an unexpected error ?
            if( StringUtils.isEmpty(to) || to.charAt(0) != '/' ) { 
                request.response().setStatusCode(401).setStatusMessage("Unauthorized")
				.putHeader("content-type", "text/html").end(DefaultPages.UNAUTHORIZED.getPage());
            }
            Renders.redirect(request, to);
        }
    }
}
