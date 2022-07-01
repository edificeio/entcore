package org.entcore.auth.security;

import org.entcore.auth.oauth.OAuthDataHandler;
import org.entcore.auth.services.SamlServiceProvider;
import org.entcore.auth.services.SamlServiceProviderFactory;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.Response;

import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.exception.AsyncResultException;
import fr.wseduc.webutils.security.Blowfish;
import fr.wseduc.webutils.security.HmacSha1;
import fr.wseduc.webutils.security.HmacSha256;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import jp.eisbahn.oauth2.server.exceptions.OAuthError;
import jp.eisbahn.oauth2.server.exceptions.Try;
import jp.eisbahn.oauth2.server.exceptions.OAuthError.AccessDenied;
import jp.eisbahn.oauth2.server.exceptions.OAuthError.MultipleVectorChoice;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import static fr.wseduc.webutils.Utils.isNotEmpty;

public class SamlHelper {

    private static final Logger log = LoggerFactory.getLogger(SamlHelper.class);
    private static final long CUSTOM_TOKEN_LIFETIME = 300000L;

    private final Vertx vertx;
    private final SamlServiceProviderFactory spFactory;
    private final String signKey;
    private final String encryptKey;

    public SamlHelper(Vertx vertx, SamlServiceProviderFactory spFactory, String signKey, String encryptKey) {
        this.vertx = vertx;
        this.spFactory = spFactory;
        this.signKey = signKey;
        this.encryptKey = encryptKey;
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

    public void processACSOAuth2(String base64SamlResponse, jp.eisbahn.oauth2.server.async.Handler<Try<OAuthError, String>> handler) {
        if (isNotEmpty(base64SamlResponse)) {
            validateSamlResponseAndGetAssertion(new String(Base64.getDecoder().decode(base64SamlResponse)), ar -> {
                if (ar.succeeded()) {
                    getUserFromAssertion(ar.result(), event -> {
                        if (event.isLeft()) {
                            String value = event.left().getValue();
                            if(value != null && value.equals("blocked.profile"))
                                handler.handle(new Try<OAuthError, String>(
                                    new AccessDenied(OAuthDataHandler.AUTH_ERROR_BLOCKED_PROFILETYPE)));
                            else if(value != null && value.equals("blocked.user"))
                                handler.handle(new Try<OAuthError, String>(
                                    new AccessDenied(OAuthDataHandler.AUTH_ERROR_BLOCKED_USER)));
                            else
                                handler.handle(new Try<OAuthError, String>(
                                    new AccessDenied(OAuthDataHandler.AUTH_ERROR_AUTHENTICATION_FAILED)));
                        } else {
                            if (event.right().getValue() != null && event.right().getValue() instanceof JsonObject) {
                                final JsonObject res = (JsonObject) event.right().getValue();
                                if (res.size() == 0) {
                                    handler.handle(new Try<OAuthError, String>(
                                            new AccessDenied(OAuthDataHandler.AUTH_ERROR_AUTHENTICATION_FAILED)));
                                } else {
                                    handler.handle(new Try<OAuthError, String>(res.getString("id")));
                                }
                            } else if (event.right().getValue() != null && event.right().getValue() instanceof JsonArray
                                    && isNotEmpty(signKey)) {
                                try {
                                    final JsonObject params = getUsersWithSignaturesAndEncryption(
                                            (JsonArray) event.right().getValue(), "", "");
                                    handler.handle(
                                            new Try<OAuthError, String>(new MultipleVectorChoice(params.encode())));
                                } catch (UnsupportedEncodingException | GeneralSecurityException e) {
                                    log.error("Error signing saml2 federated users for oauth2 token", e);
                                    handler.handle(new Try<OAuthError, String>(
                                            new AccessDenied(OAuthDataHandler.AUTH_ERROR_AUTHENTICATION_FAILED)));
                                }
                            } else {
                                handler.handle(new Try<OAuthError, String>(
                                        new AccessDenied(OAuthDataHandler.AUTH_ERROR_AUTHENTICATION_FAILED)));
                            }
                        }
                    });
                } else {
                    handler.handle(new Try<OAuthError, String>(
                            new AccessDenied(OAuthDataHandler.AUTH_ERROR_AUTHENTICATION_FAILED)));
                }
            });
        } else {
            handler.handle(
                    new Try<OAuthError, String>(new AccessDenied(OAuthDataHandler.AUTH_ERROR_AUTHENTICATION_FAILED)));
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
            j.put("key", HmacSha1.sign(sessionIndex + nameId + j.getString("login") + j.getString("id"), signKey));
            j.put("nameId", nameId);
            j.put("sessionIndex", sessionIndex);
        }
        return new JsonObject().put("users", array);
    }

    public JsonObject getUsersWithSignaturesAndEncryption(JsonArray array, String sessionIndex, String nameId)
            throws UnsupportedEncodingException, IllegalStateException, GeneralSecurityException {
        final JsonArray users = new JsonArray();
		for (Object o : array) {
			if (!(o instanceof JsonObject)) continue;
            JsonObject j = (JsonObject) o;
            j.put("iat", System.currentTimeMillis());
            j.put("key", HmacSha256.sign(sessionIndex + nameId + j.getString("login") + j.getString("id") + j.getLong("iat"), signKey));
            if (isNotEmpty(nameId)) {
                j.put("nameId", nameId);
            }
            if (isNotEmpty(sessionIndex)) {
                j.put("sessionIndex", sessionIndex);
            }
            final JsonObject user = new JsonObject()
                .put("structureName", j.getString("structureName"))
                .put("key", Blowfish.encrypt(j.encode(), encryptKey));
            users.add(user);
		}
		return new JsonObject().put("users", users);
    }

	public void processCustomToken(String customToken, jp.eisbahn.oauth2.server.async.Handler<Try<AccessDenied, String>> handler) {
        if (isNotEmpty(customToken)) {
            try {
                final String encodedJson = Blowfish.decrypt(customToken, encryptKey);
                final JsonObject j = new JsonObject(encodedJson);
                final String signature = HmacSha256.sign((j.getString("sessionIndex", "") +
                        j.getString("nameId", "") + j.getString("login") + j.getString("id") + j.getLong("iat")), signKey);
                if (j.size() > 0 && isNotEmpty(signature) &&
                        signature.equals(j.getString("key", "")) &&
                        (j.getLong("iat") + CUSTOM_TOKEN_LIFETIME) > System.currentTimeMillis()) {
                    handler.handle(new Try<AccessDenied, String>(j.getString("id")));
                } else {
                    handler.handle(new Try<AccessDenied, String>(new AccessDenied(OAuthDataHandler.AUTH_ERROR_AUTHENTICATION_FAILED)));
                }
            } catch (GeneralSecurityException | UnsupportedEncodingException e) {
                log.error("Error decrypting custom token or validating signature", e);
                handler.handle(new Try<AccessDenied, String>(new AccessDenied(OAuthDataHandler.AUTH_ERROR_AUTHENTICATION_FAILED)));
            }
        } else {
            handler.handle(new Try<AccessDenied, String>(new AccessDenied(OAuthDataHandler.AUTH_ERROR_AUTHENTICATION_FAILED)));
        }
	}

}
