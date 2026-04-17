package org.entcore.auth;

import com.eatthepath.otp.TimeBasedOneTimePasswordGenerator;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.entcore.auth.services.impl.DefaultMfaService;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.test.TestHelper;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import javax.crypto.spec.SecretKeySpec;
import java.lang.reflect.Method;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

@RunWith(VertxUnitRunner.class)
public class DefaultMfaServiceTest {

    private static final TestHelper test = TestHelper.helper();
    private static DefaultMfaService mfaService;
    private static final TimeBasedOneTimePasswordGenerator TOTP_GENERATOR = new TimeBasedOneTimePasswordGenerator(Duration.ofSeconds(60));

    // Secret shared by all tests: raw bytes -> Base64 (format stored in Neo4j)
    private static final byte[] SECRET_BYTES = new byte[20];
    private static final String SECRET_B64;

    static {
        new SecureRandom().nextBytes(SECRET_BYTES);
        SECRET_B64 = Base64.getEncoder().encodeToString(SECRET_BYTES);
    }

    @BeforeClass
    public static void setUp(TestContext context) throws Exception {
        // No Docker needed: service instantiation only, Neo4j is mocked per-test
        mfaService = new DefaultMfaService(test.vertx(), new JsonObject(), new HashMap<>());
    }

    /** Computes the current TOTP code using the same algorithm as DefaultMfaService. */
    private static String currentValidCode() throws Exception {
        final SecretKeySpec key = new SecretKeySpec(SECRET_BYTES, TOTP_GENERATOR.getAlgorithm());
        return TOTP_GENERATOR.generateOneTimePasswordString(key, Instant.now());
    }

    /** Calls the private verifyTotp(secretB64, code) method via reflection. */
    @SuppressWarnings("unchecked")
    private static Future<JsonObject> invokeVerifyTotp(String secretB64, String code) throws Exception {
        final Method method = DefaultMfaService.class.getDeclaredMethod("verifyTotp", String.class, String.class);
        method.setAccessible(true);
        return (Future<JsonObject>) method.invoke(mfaService, secretB64, code);
    }

    /**
     * Configures a mocked Neo4j instance to return a single row (or empty if row is null)
     * when execute() is called.
     */
    @SuppressWarnings("unchecked")
    private static void mockNeo4jExecute(Neo4j mockNeo4j, JsonObject row) {
        Mockito.doAnswer(inv -> {
            final Handler<Message<JsonObject>> handler = (Handler<Message<JsonObject>>) inv.getArgument(2);
            final Message<JsonObject> msg = (Message<JsonObject>) Mockito.mock(Message.class);
            final JsonArray result = row != null ? new JsonArray().add(row) : new JsonArray();
            Mockito.when(msg.body()).thenReturn(
                    new JsonObject().put("status", "ok").put("result", result));
            handler.handle(msg);
            return null;
        }).when(mockNeo4j).execute(anyString(), any(JsonObject.class),
                Mockito.<Handler<Message<JsonObject>>>any());
    }

    // ───────────────────────────────────────
    // verifyTotp -- pure TOTP algorithm tests
    // ───────────────────────────────────────

    @Test
    public void testVerifyTotp_valid(TestContext context) throws Exception {
        final Async async = context.async();
        invokeVerifyTotp(SECRET_B64, currentValidCode())
                .onFailure(context::fail)
                .onSuccess(result -> {
                    context.assertEquals("valid", result.getString("state"),
                            "A correct TOTP code should return state=valid");
                    async.complete();
                });
    }

    @Test
    public void testVerifyTotp_invalid(TestContext context) throws Exception {
        final Async async = context.async();
        // "badcode" is non-numeric and will never equal a 6-digit TOTP string
        invokeVerifyTotp(SECRET_B64, "badcode")
                .onFailure(context::fail)
                .onSuccess(result -> {
                    context.assertEquals("invalid", result.getString("state"),
                            "A wrong TOTP code should return state=invalid");
                    async.complete();
                });
    }

    @Test
    public void testVerifyTotp_codeFromDifferentSecret(TestContext context) throws Exception {
        final Async async = context.async();
        // A code from a different secret must be rejected by the original secret
        final byte[] otherBytes = new byte[20];
        new SecureRandom().nextBytes(otherBytes);
        final SecretKeySpec otherKey = new SecretKeySpec(otherBytes, TOTP_GENERATOR.getAlgorithm());
        final String codeForOtherSecret = TOTP_GENERATOR.generateOneTimePasswordString(otherKey, Instant.now());

        invokeVerifyTotp(SECRET_B64, codeForOtherSecret)
                .onFailure(context::fail)
                .onSuccess(result -> {
                    context.assertEquals("invalid", result.getString("state"),
                            "A code from a different secret should return state=invalid");
                    async.complete();
                });
    }

    // ───────────────────────────────────────────────────
    // verifyTotpForUser -- Neo4j layer mocked via Mockito
    // ───────────────────────────────────────────────────

    @Test
    public void testVerifyTotpForUser_valid(TestContext context) throws Exception {
        final Async async = context.async();
        final String code = currentValidCode();

        try (MockedStatic<Neo4j> neo4jMock = Mockito.mockStatic(Neo4j.class)) {
            final Neo4j mockNeo4j = Mockito.mock(Neo4j.class);
            neo4jMock.when(Neo4j::getInstance).thenReturn(mockNeo4j);
            mockNeo4jExecute(mockNeo4j, new JsonObject().put("totp", SECRET_B64));

            mfaService.verifyTotpForUser("user-enrolled", code)
                    .onFailure(context::fail)
                    .onSuccess(result -> {
                        context.assertEquals("valid", result.getString("state"),
                                "A correct code for an enrolled user should return state=valid");
                        async.complete();
                    });
        }
    }

    @Test
    public void testVerifyTotpForUser_invalidCode(TestContext context) throws Exception {
        final Async async = context.async();

        try (MockedStatic<Neo4j> neo4jMock = Mockito.mockStatic(Neo4j.class)) {
            final Neo4j mockNeo4j = Mockito.mock(Neo4j.class);
            neo4jMock.when(Neo4j::getInstance).thenReturn(mockNeo4j);
            mockNeo4jExecute(mockNeo4j, new JsonObject().put("totp", SECRET_B64));

            mfaService.verifyTotpForUser("user-enrolled", "000000")
                    .onFailure(context::fail)
                    .onSuccess(result -> {
                        context.assertEquals("invalid", result.getString("state"),
                                "A wrong code for an enrolled user should return state=invalid");
                        async.complete();
                    });
        }
    }

    @Test
    public void testVerifyTotpForUser_notEnrolled(TestContext context) {
        final Async async = context.async();

        try (MockedStatic<Neo4j> neo4jMock = Mockito.mockStatic(Neo4j.class)) {
            final Neo4j mockNeo4j = Mockito.mock(Neo4j.class);
            neo4jMock.when(Neo4j::getInstance).thenReturn(mockNeo4j);
            // User exists but has no totp field
            mockNeo4jExecute(mockNeo4j, new JsonObject());

            mfaService.verifyTotpForUser("user-not-enrolled", "123456")
                    .onFailure(context::fail)
                    .onSuccess(result -> {
                        context.assertEquals("not.enrolled", result.getString("state"),
                                "A user without a TOTP secret should return state=not.enrolled");
                        async.complete();
                    });
        }
    }

    @Test
    public void testVerifyTotpForUser_userNotFound(TestContext context) {
        final Async async = context.async();

        try (MockedStatic<Neo4j> neo4jMock = Mockito.mockStatic(Neo4j.class)) {
            final Neo4j mockNeo4j = Mockito.mock(Neo4j.class);
            neo4jMock.when(Neo4j::getInstance).thenReturn(mockNeo4j);
            // Empty result = user not found
            mockNeo4jExecute(mockNeo4j, null);

            mfaService.verifyTotpForUser("unknown-user", "123456")
                    .onFailure(err -> {
                        context.assertEquals("user.not.found", err.getMessage(),
                                "An unknown userId should fail with user.not.found");
                        async.complete();
                    })
                    .onSuccess(result -> context.fail("Expected failure for unknown user, got: " + result));
        }
    }
}
