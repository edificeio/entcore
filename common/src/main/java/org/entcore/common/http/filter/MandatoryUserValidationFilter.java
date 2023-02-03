package org.entcore.common.http.filter;

import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.filter.Filter;
import fr.wseduc.webutils.security.SecureHttpServerRequest;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;

import org.entcore.common.datavalidation.UserValidation;
import org.entcore.common.http.response.DefaultPages;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.entcore.common.utils.Mfa;
import org.entcore.common.utils.StringUtils;

import static org.entcore.common.datavalidation.UserValidationService.FIELD_MUST_VALIDATE_EMAIL;
import static org.entcore.common.datavalidation.UserValidationService.FIELD_MUST_VALIDATE_MOBILE;
import static org.entcore.common.datavalidation.UserValidationService.FIELD_NEED_MFA;
import static org.entcore.common.datavalidation.UserValidationService.FIELD_MUST_VALIDATE_TERMS;

import java.net.URLEncoder;

/**
 * This filter checks if the user needs to be redirected to must perform some mandatory validation before processing.
 * For example :
 * - validate Terms of Use     => redirect to /auth/revalidate-terms
 * - validate an email address => redirect to /auth/validate-mail
 */
public class MandatoryUserValidationFilter implements Filter {
    private final EventBus eventBus;
    private final boolean emailValidationActive;
    
    private final static int    TERMS_OF_USE_IDX  = 0;
    private final static int    EMAIL_ADDRESS_IDX = 1;
    private final static int    MOBILE_PHONE_IDX  = 2;
    private final static int    MFA_IDX  = 3;
    private final static String[] whiteListAlways = {
        "/directory/user/mailstate", "/directory/user/mobilestate", "/auth/"
    };
    // White-lists are incremental : each step also applies to following steps
    private final static String[][] whiteListByStep = {
        {},
        {"/internal/userinfo", "/userbook/", "/theme"},
        {/*same routes as preceding step, used by /auth/validate-mail*/},
        {/*same routes as preceding step, used by /auth/validate-mail*/}
    };
    // Black-list overrides white-list for the same step
    private final static String[][] blackListByStep = {
        {},
        {"mon-compte", "annuaire"}, // Prevent navigation to this routes
        {"mon-compte", "annuaire"}, // Prevent navigation to this routes
        {"mon-compte", "annuaire"}
    };

    private final static String REDIRECT_TO_KEY = "MandatoryUserValidationFilterRedirectsTo";

    public MandatoryUserValidationFilter(EventBus eventBus, boolean emailValidationActive) {
        this.eventBus = eventBus;
        this.emailValidationActive = emailValidationActive;
    }

    @Override
    public void canAccess(HttpServerRequest request, Handler<Boolean> handler) {
        // Ajax requests are not redirected (useless)
        if ("XMLHttpRequest".equals(request.headers().get("X-Requested-With"))){
            handler.handle(true);
            return;
        }

        UserUtils.getSession(this.eventBus, request, session -> {
            final UserInfos userInfos = UserUtils.sessionToUserInfos(session);
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
                UserValidation.getMandatoryUserValidation(this.eventBus, session)
                .compose( validations -> {
                    return checkTermsOfUse(sreq, userInfos, validations);
                })
                .compose( validations -> {
                    return checkEmailAddress(sreq, userInfos, validations);
                })
                .compose( validations -> {
                    return checkMobilePhone(sreq, userInfos, validations);
                })
                .compose( validations -> {
                    return checkMfa(sreq, userInfos, validations);
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

    private boolean isInArray(final String path, final JsonArray array) {
        for( int j=0; j<array.size(); j++ ) {
            if( path.contains(array.getString(j)) ){
                return true;
            }
        }
        return false;
    }

    private boolean isInList(final String path, final String[] list) {
        for( int j=0; j<list.length; j++ ) {
            if( path.contains(list[j]) ){
                return true;
            }
        }
        return false;
    }

    private boolean isInWhiteList(final String path, final String verb, final int step) {
        if( isInList(path, whiteListAlways) ) {
            return true;
        }
        if( isInList(path, blackListByStep[step]) ) {
            return false;
        }
        // At every step we must also check the white list of the previous steps URLs, 
        // to avoid deadlocks between filters (but only for GET requests)
        final boolean isGET = verb.equalsIgnoreCase("GET");
        for( int i=0; isGET && i<=step; i++ ) {
            if( isInList(path, whiteListByStep[i]) ) {
                return true;
            }
        }
        return false;
    }

    private Future<JsonObject> checkTermsOfUse(final SecureHttpServerRequest request, UserInfos userInfos, JsonObject validations) {
        if( Boolean.FALSE.equals(validations.getBoolean(FIELD_MUST_VALIDATE_TERMS, false)) // No need to revalidate => OK
            || request.headers().contains("Authorization") // Clients with Authorization header have terms of use validated beforehand => OK
            || isInWhiteList(request.path(), request.method().name(), TERMS_OF_USE_IDX)    // White-listed url requested => OK
        ) {
            return Future.succeededFuture(validations); 
        }
        // KO
        return Future.failedFuture("/auth/revalidate-terms"); // Where to redirect (must be white-listed !)
    }

    private Future<JsonObject> checkEmailAddress(final SecureHttpServerRequest request, UserInfos userInfos, JsonObject validations) {
        if( Boolean.FALSE.equals(validations.getBoolean(FIELD_MUST_VALIDATE_EMAIL, false)) // No need to revalidate => OK
            || isInWhiteList(request.path(), request.method().name(), EMAIL_ADDRESS_IDX)  // white-listed url requested => OK
            || !this.emailValidationActive
        ) {
            return Future.succeededFuture(validations);
        }
        // KO
        String url = "/auth/validate-mail?force=true&type=email"; // Where to redirect (must be white-listed !)
        try {
            url = url +"&redirect="+ URLEncoder.encode(request.absoluteURI(), "UTF-8");
        } catch( Exception e ) {
            // silent failure
        }
        return Future.failedFuture(url);
    }

    private Future<JsonObject> checkMobilePhone(final SecureHttpServerRequest request, UserInfos userInfos, JsonObject validations) {
        if( Boolean.FALSE.equals(validations.getBoolean(FIELD_MUST_VALIDATE_MOBILE, false)) // No need to revalidate => OK
            || isInWhiteList(request.path(), request.method().name(), MOBILE_PHONE_IDX)  // white-listed url requested => OK
            || !this.emailValidationActive  // This parameter also applies to mobile phone
        ) {
            return Future.succeededFuture(validations);
        }
        // KO
        String url = "/auth/validate-mail?force=true&type=sms"; // Where to redirect (must be white-listed !)
        try {
            url = url +"&redirect="+ URLEncoder.encode(request.absoluteURI(), "UTF-8");
        } catch( Exception e ) {
            // silent failure
        }
        return Future.failedFuture(url);
    }

    private Future<JsonObject> checkMfa(final SecureHttpServerRequest request, UserInfos userInfos, JsonObject validations) {
        if( Boolean.FALSE.equals(validations.getBoolean(FIELD_NEED_MFA, false)) // No need to perform a MFA => OK
                || isInWhiteList(request.path(), request.method().name(), MFA_IDX) // white-listed url requested => OK
                || !isInArray(request.path(), Mfa.getMfaProtectedUrls()) // Url not concerned by 2FA => OK
        ) {
            return Future.succeededFuture(validations);
        }
        // KO
        String url = "/auth/validate-mfa?force=true"; // Where to redirect (must be white-listed !)
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
