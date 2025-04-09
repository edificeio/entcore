/* Copyright © "Open Digital Education", 2014
 *
 * This program is published by "Open Digital Education".
 * You must indicate the name of the software and the company in any production /contribution
 * using the software and indicate on the home page of the software industry in question,
 * "powered by Open Digital Education" with a reference to the website: https://opendigitaleducation.com/.
 *
 * This program is free software, licensed under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation, version 3 of the License.
 *
 * You can redistribute this application and/or modify it since you respect the terms of the GNU Affero General Public License.
 * If you modify the source code and then use this modified source code in your creation, you must make available the source code of your modifications.
 *
 * You should have received a copy of the GNU Affero General Public License along with the software.
 * If not, please see : <http://www.gnu.org/licenses/>. Full compliance requires reading the terms of this license and following its directives.

 *
 */

package org.entcore.auth.security;

import fr.wseduc.webutils.security.Blowfish;
import fr.wseduc.webutils.security.HmacSha256;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import jp.eisbahn.oauth2.server.exceptions.OAuthError;
import jp.eisbahn.oauth2.server.exceptions.OAuthError.UnsupportedResponseType;
import jp.eisbahn.oauth2.server.exceptions.Try;
import jp.eisbahn.oauth2.server.models.UserData;
import org.entcore.auth.oauth.OAuthDataHandler;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;

import static fr.wseduc.webutils.Utils.isNotEmpty;
import static org.entcore.auth.security.SamlHelper.json2UserData;

public final class CustomTokenHelper {
    private static final Logger log = LoggerFactory.getLogger(CustomTokenHelper.class);
    private static final long CUSTOM_TOKEN_LIFETIME = 300000L;
    private static String SIGN_KEY;
    private static String ENCRYPT_KEY;

    public static JsonObject getUsersWithSignaturesAndEncryption(JsonArray array, String sessionIndex, String nameId)
            throws UnsupportedEncodingException, IllegalStateException, GeneralSecurityException {
        final JsonArray users = new JsonArray();
        for (Object o : array) {
            if (!(o instanceof JsonObject)) continue;
            JsonObject j = (JsonObject) o;
            final JsonObject user = new JsonObject()
                    .put("structureName", j.getString("structureName"))
                    .put("key", generateCustomToken(j, sessionIndex, nameId));
            users.add(user);
        }
        return new JsonObject().put("users", users);
    }

    public static String getUserWithSignaturesAndEncryption(JsonObject userInfo, String sessionIndex, String nameId)
            throws UnsupportedEncodingException, IllegalStateException, GeneralSecurityException, UnsupportedResponseType {
        if (userInfo == null) {
            throw new UnsupportedResponseType("user information is empty");
        } else {
            return generateCustomToken(userInfo, sessionIndex, nameId);
        }
    }

    private static String generateCustomToken(JsonObject userInfo, String sessionIndex, String nameId)
            throws UnsupportedEncodingException, IllegalStateException, GeneralSecurityException {
        userInfo.put("iat", System.currentTimeMillis());
        userInfo.put("key", HmacSha256.sign(
                sessionIndex + nameId + userInfo.getString("login") + userInfo.getString("id") + userInfo.getLong("iat"), SIGN_KEY));
        if (isNotEmpty(nameId)) {
            userInfo.put("nameId", nameId);
        }
        if (isNotEmpty(sessionIndex)) {
            userInfo.put("sessionIndex", sessionIndex);
        }
        return Blowfish.encrypt(userInfo.encode(), ENCRYPT_KEY);
    }

    public static void processCustomToken(String customToken, jp.eisbahn.oauth2.server.async.Handler<Try<OAuthError.AccessDenied, UserData>> handler) {
        if (isNotEmpty(customToken)) {
            try {
                final String encodedJson = Blowfish.decrypt(customToken, ENCRYPT_KEY);
                final JsonObject j = new JsonObject(encodedJson);
                final String signature = HmacSha256.sign((j.getString("sessionIndex", "") +
                        j.getString("nameId", "") + j.getString("login") + j.getString("id") + j.getLong("iat")), SIGN_KEY);
                if (j.size() > 0 && isNotEmpty(signature) &&
                        signature.equals(j.getString("key", "")) &&
                        (j.getLong("iat") + CUSTOM_TOKEN_LIFETIME) > System.currentTimeMillis()) {
                    handler.handle(new Try<OAuthError.AccessDenied, UserData>(json2UserData(j)));
                } else {
                    handler.handle(new Try<OAuthError.AccessDenied, UserData>(new OAuthError.AccessDenied(OAuthDataHandler.AUTH_ERROR_AUTHENTICATION_FAILED)));
                }
            } catch (GeneralSecurityException | UnsupportedEncodingException e) {
                log.error("Error decrypting custom token or validating signature", e);
                handler.handle(new Try<OAuthError.AccessDenied, UserData>(new OAuthError.AccessDenied(OAuthDataHandler.AUTH_ERROR_AUTHENTICATION_FAILED)));
            }
        } else {
            handler.handle(new Try<OAuthError.AccessDenied, UserData>(new OAuthError.AccessDenied(OAuthDataHandler.AUTH_ERROR_AUTHENTICATION_FAILED)));
        }
    }

    public static void setSignKey(String signKey) {
        SIGN_KEY = signKey;
    }

    public static void setEncryptKey(String encryptKey) {
        ENCRYPT_KEY = encryptKey;
    }
}
