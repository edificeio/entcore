package org.entcore.auth.security;

import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.exception.AsyncResultException;
import fr.wseduc.webutils.security.HmacSha1;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import jp.eisbahn.oauth2.server.exceptions.OAuthError;
import jp.eisbahn.oauth2.server.exceptions.OAuthError.AccessDenied;
import jp.eisbahn.oauth2.server.exceptions.OAuthError.MultipleVectorChoice;
import jp.eisbahn.oauth2.server.exceptions.Try;
import jp.eisbahn.oauth2.server.models.UserData;
import org.entcore.auth.oauth.OAuthDataHandler;
import org.entcore.auth.services.SamlServiceProvider;
import org.entcore.auth.services.SamlServiceProviderFactory;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.Response;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import static fr.wseduc.webutils.Utils.isNotEmpty;

public class SamlHelper {

    private static final Logger log = LoggerFactory.getLogger(SamlHelper.class);

    private final Vertx vertx;
    private final SamlServiceProviderFactory spFactory;
    private final String signKey;

    public SamlHelper(Vertx vertx, SamlServiceProviderFactory spFactory, String signKey) {
        this.vertx = vertx;
        this.spFactory = spFactory;
        this.signKey = signKey;
    }

    public void validateSamlResponseAndGetAssertion(String samlResponse, Handler<AsyncResult<Assertion>> handler) {
        if (samlResponse != null && samlResponse.contains("EncryptedAssertion")) {
            final JsonObject j = new JsonObject().put("action", "validate-signature-decrypt").put("response",
                    samlResponse);
            vertx.eventBus().request("saml", j, ar -> {
                if (ar.succeeded()) {
                    final JsonObject event = (JsonObject) ar.result().body();
                    final String assertion = event.getString("assertion");
                    if ("ok".equals(event.getString("status")) && event.getBoolean("valid", false)
                            && assertion != null) {
                        try {
                            handler.handle(Future.succeededFuture(SamlUtils.unmarshallAssertion(assertion)));
                        } catch (Exception e) {
                            handler.handle(Future.failedFuture(e));
                        }
                    } else {
                        handler.handle(Future.failedFuture(new AsyncResultException("invalid.signature")));
                    }
                } else {
                    handler.handle(Future.failedFuture(ar.cause()));
                }
            });
        } else if (samlResponse != null) {
            final JsonObject j = new JsonObject().put("action", "validate-signature").put("response", samlResponse);
            vertx.eventBus().request("saml", j, ar -> {
                if (ar.succeeded()) {
                    final JsonObject event = (JsonObject) ar.result().body();
                    if ("ok".equals(event.getString("status")) && event.getBoolean("valid", false)) {
                        try {
                            final Response response = SamlUtils.unmarshallResponse(samlResponse);
                            if (response.getAssertions() == null || response.getAssertions().size() != 1) {
                                handler.handle(Future.failedFuture(new AsyncResultException("invalid.assertion")));
                            } else {
                                handler.handle(Future.succeededFuture(response.getAssertions().get(0)));
                            }
                        } catch (Exception e) {
                            handler.handle(Future.failedFuture(e));
                        }
                    } else {
                        handler.handle(Future.failedFuture(new AsyncResultException("invalid.assertion")));
                    }
                } else {
                    handler.handle(Future.failedFuture(ar.cause()));
                }
            });
        } else {
            handler.handle(Future.failedFuture(new AsyncResultException("invalid.assertion")));
        }
    }

    public void getUserFromAssertion(Assertion assertion, Handler<Either<String, Object>> handler) {
        final SamlServiceProvider sp = spFactory.serviceProvider(assertion);
        sp.execute(assertion, handler);
    }

    public void processACSOAuth2(String base64SamlResponse, jp.eisbahn.oauth2.server.async.Handler<Try<OAuthError, UserData>> handler) {
        if (isNotEmpty(base64SamlResponse)) {
            validateSamlResponseAndGetAssertion(new String(Base64.getDecoder().decode(base64SamlResponse)), ar -> {
                if (ar.succeeded()) {
                    getUserFromAssertion(ar.result(), event -> {
                        if (event.isLeft()) {
                            String value = event.left().getValue();
                            if(value != null && value.equals("blocked.profile"))
                                handler.handle(new Try<OAuthError, UserData>(
                                    new AccessDenied(OAuthDataHandler.AUTH_ERROR_BLOCKED_PROFILETYPE)));
                            else if(value != null && value.equals("blocked.user"))
                                handler.handle(new Try<OAuthError, UserData>(
                                    new AccessDenied(OAuthDataHandler.AUTH_ERROR_BLOCKED_USER)));
                            else
                                handler.handle(new Try<OAuthError, UserData>(
                                    new AccessDenied(OAuthDataHandler.AUTH_ERROR_AUTHENTICATION_FAILED)));
                        } else {
                            if (event.right().getValue() != null && event.right().getValue() instanceof JsonObject) {
                                final JsonObject res = (JsonObject) event.right().getValue();
                                if (res.size() == 0) {
                                    handler.handle(new Try<OAuthError, UserData>(
                                            new AccessDenied(OAuthDataHandler.AUTH_ERROR_AUTHENTICATION_FAILED)));
                                } else {
                                    handler.handle(new Try<OAuthError, UserData>(json2UserData(res)));
                                }
                            } else if (event.right().getValue() != null && event.right().getValue() instanceof JsonArray
                                    && isNotEmpty(signKey)) {
                                try {
                                    final JsonObject params = getUsersWithSignaturesAndEncryption(
                                            (JsonArray) event.right().getValue(), "", "");
                                    handler.handle(
                                            new Try<OAuthError, UserData>(new MultipleVectorChoice(params.encode())));
                                } catch (UnsupportedEncodingException | GeneralSecurityException e) {
                                    log.error("Error signing saml2 federated users for oauth2 token", e);
                                    handler.handle(new Try<OAuthError, UserData>(
                                            new AccessDenied(OAuthDataHandler.AUTH_ERROR_AUTHENTICATION_FAILED)));
                                }
                            } else {
                                handler.handle(new Try<OAuthError, UserData>(
                                        new AccessDenied(OAuthDataHandler.AUTH_ERROR_AUTHENTICATION_FAILED)));
                            }
                        }
                    });
                } else {
                    handler.handle(new Try<OAuthError, UserData>(
                            new AccessDenied(OAuthDataHandler.AUTH_ERROR_AUTHENTICATION_FAILED)));
                }
            });
        } else {
            handler.handle(
                    new Try<OAuthError, UserData>(new AccessDenied(OAuthDataHandler.AUTH_ERROR_AUTHENTICATION_FAILED)));
        }
    }

    /**
     * @deprecated Use getUsersWithSignaturesAndEncryption instead
     * @param array users found 
     * @param sessionIndex
     * @param nameId
     * @return
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     * @throws UnsupportedEncodingException
     */
    public JsonObject getUsersWithSignatures(JsonArray array, String sessionIndex, String nameId)
            throws NoSuchAlgorithmException, InvalidKeyException, UnsupportedEncodingException {
        for (Object o : array) {
            if (!(o instanceof JsonObject))
                continue;
            JsonObject j = (JsonObject) o;
            if (!j.getBoolean("blockedUser", false)) {
                j.put("key", HmacSha1.sign(sessionIndex + nameId + j.getString("login") + j.getString("id"), signKey));
            }
            j.put("nameId", nameId);
            j.put("sessionIndex", sessionIndex);
        }
        return new JsonObject().put("users", array);
    }

    public JsonObject getUsersWithSignaturesAndEncryption(JsonArray array, String sessionIndex, String nameId)
            throws UnsupportedEncodingException, IllegalStateException, GeneralSecurityException {
        return CustomTokenHelper.getUsersWithSignaturesAndEncryption(array, sessionIndex, nameId);
    }

    public static UserData json2UserData(JsonObject j) {
        if (j == null) {
            return null;
        } else if (isNotEmpty(j.getString("activationCode"))) {
            return new UserData(j.getString("id"), j.getString("activationCode"),
                j.getString("login"), j.getString("email"), j.getString("mobile"), j.getString("source"));
        } else {
            return new UserData(j.getString("id"));
        }
    }

}
