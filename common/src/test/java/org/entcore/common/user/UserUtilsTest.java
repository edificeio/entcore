package org.entcore.common.user;

import fr.wseduc.webutils.request.CookieHelper;
import fr.wseduc.webutils.security.HmacSha1;
import fr.wseduc.webutils.security.SecureHttpServerRequest;
import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.DefaultCookie;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import static org.entcore.common.http.filter.AppOAuthResourceProvider.getTokenId;
import org.entcore.common.session.SessionRecreationRequest;
import static org.entcore.common.user.UserUtils.SESSION_ADDRESS;
import org.entcore.test.TestHelper;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@RunWith(VertxUnitRunner.class)
public class UserUtilsTest {
    protected static final TestHelper test = TestHelper.helper();
    private static final Map<String, JsonObject> sessionStore = new HashMap<>();
    private static final AtomicInteger counterOAuthValid = new AtomicInteger();
    private static final AtomicInteger counterFindSession = new AtomicInteger();
    private static final AtomicInteger counterRecreateSession = new AtomicInteger();

    @Before
    public void reset() {
        sessionStore.clear();
        counterFindSession.set(0);
        counterRecreateSession.set(0);
        counterOAuthValid.set(0);
    }
    @BeforeClass
    public static void setUp(final TestContext context) throws Exception {
        CookieHelper.getInstance().init("test", LoggerFactory.getLogger(UserUtilsTest.class));
        /*******************************************
         * Listen for events to mock
         * - oauth creds retrieval
         * - session retrieval
         * - session recreation
         *******************************************/
        final EventBus eb = test.vertx().eventBus();
        eb.consumer("wse.oauth").handler(message -> {
            counterOAuthValid.incrementAndGet();
            message.reply(new JsonObject()
                    .put("status", "ok")
                    .put("client_id", "test")
                    .put("remote_user", "test_user")
                    .put("scope", "test_scope"));
        });
        eb.consumer(SESSION_ADDRESS).handler(message -> {
            final String action = ((JsonObject)message.body()).getString("action");
            switch (action) {
                case "find":
                    counterFindSession.incrementAndGet();
                    final String sessionId = ((JsonObject)message.body()).getString("sessionId");
                    final JsonObject findReply = sessionStore.containsKey(sessionId) ?
                            new JsonObject()
                                    .put("status", "ok")
                                    .put("session", sessionStore.get(sessionId)) :
                            new JsonObject().put("status", "error").put("message", "Session not found. 4");
                    message.reply(findReply);
                    break;
                case "recreate":
                    counterRecreateSession.incrementAndGet();
                    final SessionRecreationRequest request = ((JsonObject)message.body()).mapTo(SessionRecreationRequest.class);
                    final JsonObject session = new JsonObject()
                            .put("_id", request.getSessionId())
                            .put("userId", request.getUserId());
                    sessionStore.put(request.getSessionId(), session);
                    message.reply(session);
                    break;
                default:
                    final JsonObject json = new JsonObject().put("status", "error").put("message", "action.unknown");
                    message.reply(json);
            }
        });
    }
    /**
     * Test that a session is recreated for an oauthenticated user who comes with a valid access_token but no session in
     * Redis.
     *
     * @param context Test context
     */
    @Test
    public void testGetSessionCanRecreateSessionForOAuthUserIfSessionIsNotFoundInStore(final TestContext context) {
        final Async async = context.async();
        final EventBus eb = test.vertx().eventBus();
        sessionStore.clear();
        /*******************************************
         * Create a fake http request with an OAuth
         * token and check that getSession retrieves
         * a session.
         *******************************************/
        final String oauthToken = UUID.randomUUID().toString();
        final HttpServerRequest request = test.http().get("/test").withOAuthToken();
        UserUtils.getSession(eb, request, true, session -> {
            context.assertNotNull(session, "Session should have been recreated");
            final Optional<String> tokenId = getTokenId((SecureHttpServerRequest) request);
            final String actualSessionId = session.getString("_id");
            context.assertEquals(tokenId.get(), actualSessionId);
            context.assertTrue(counterFindSession.get() >= 1, "Should heave at least tried to find session once");
            context.assertTrue(counterRecreateSession.get() >= 1, "Should heave at least tried to recreate the session once");
            context.assertEquals(1, counterOAuthValid.get(), "Should heave at least tried to validate oauth credentials");
            async.complete();
        });
    }


    /**
     * Test that a session is not recreated for a web user who comes with a session id but no session in
     * the store.
     *
     * @param context Test context
     */
    @Test
    public void testGetSessionReturnNullIfSessionIsNotFound(final TestContext context) throws Exception {
        final Async async = context.async();
        final EventBus eb = test.vertx().eventBus();
        sessionStore.clear();
        /*******************************************
         * Create a fake http request with an OAuth
         * token and check that getSession retrieves
         * a session.
         *******************************************/
        final String oauthToken = signCookie(new DefaultCookie("oneSessionId", UUID.randomUUID().toString()));
        final UserInfos user = test.http().sessionUser();
        final HttpServerRequest request = test.http().get("/test");
        request.headers().add("Cookie", "oneSessionId=" + oauthToken);
        UserUtils.getSession(eb, request, true, session -> {
            context.assertNull(session, "Session should not have been recreated");
            context.assertEquals(1, counterFindSession.get(), "Should heave at least tried to find session once");
            context.assertEquals(0, counterRecreateSession.get(), "Should not have tried to recreate the session");
            context.assertEquals(0, counterOAuthValid.get(), "Should not have tried to validate oauth credentials");
            async.complete();
        });
    }


    /**
     * Test that a session is returned for a web user who has a session in the store.
     *
     * @param context Test context
     */
    @Test
    public void testGetSessionReturnSessionIfWebSessionIsFound(final TestContext context) throws Exception {
        final Async async = context.async();
        final EventBus eb = test.vertx().eventBus();
        /*******************************************
         * Create a fake http request with an OAuth
         * token and check that getSession retrieves
         * a session.
         *******************************************/
        final String oneSessionId = UUID.randomUUID().toString();
        final String oauthToken = signCookie(new DefaultCookie("oneSessionId", oneSessionId));
        final UserInfos user = test.http().sessionUser();
        final HttpServerRequest request = test.http().get("/test");
        request.headers().add("Cookie", "oneSessionId=" + oauthToken);
        sessionStore.put(oneSessionId, new JsonObject().put("_id", oneSessionId).put("userId", user.getUserId()));
        UserUtils.getSession(eb, request, true, session -> {
            context.assertNotNull(session, "Session should have been found");
            context.assertEquals(oneSessionId, session.getString("_id"), "Session should not have changed");
            context.assertEquals(1, counterFindSession.get(), "Should heave at least tried to find session once");
            context.assertEquals(0, counterRecreateSession.get(), "Should not have tried to recreate the session");
            context.assertEquals(0, counterOAuthValid.get(), "Should not have tried to validate oauth credentials");
            async.complete();
        });
    }
    /**
     * Test that a session is returned for an oauthenticated user whose session still exists.
     *
     * @param context Test context
     */
    @Test
    public void testGetSessionReturnSessionIfFoundInStoreForOauthUsers(final TestContext context) {
        final Async async = context.async();
        final EventBus eb = test.vertx().eventBus();
        sessionStore.clear();
        /*******************************************
         * Create a fake http request with an OAuth
         * token and check that getSession retrieves
         * a session.
         *******************************************/
        final HttpServerRequest request = test.http().get("/test").withOAuthToken();
        final String tokenId = getTokenId((SecureHttpServerRequest) request).get();
        sessionStore.put(tokenId, new JsonObject().put("_id", tokenId).put("userId", "test"));
        UserUtils.getSession(eb, request, true, session -> {
            context.assertNotNull(session, "Session should have been found");
            final String actualSessionId = session.getString("_id");
            context.assertEquals(tokenId, actualSessionId);
            context.assertEquals(1, counterFindSession.get(), "Should heave at least tried to find session once");
            context.assertEquals(0, counterRecreateSession.get(), "Should not have tried to recreate the session");
            context.assertEquals(1, counterOAuthValid.get(), "Should have tried to validate oauth credentials");
            async.complete();
        });
    }

    private String signCookie(Cookie cookie)
            throws InvalidKeyException, NoSuchAlgorithmException,
            IllegalStateException, UnsupportedEncodingException {
        String signature = HmacSha1.sign(
                cookie.getDomain()+cookie.getName()+
                        "/"+cookie.getValue(), "test");
        return cookie.getValue() + ":" + signature;
    }

}
