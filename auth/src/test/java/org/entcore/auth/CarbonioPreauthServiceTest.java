package org.entcore.auth;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.entcore.auth.services.CarbonioPreauthService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@RunWith(VertxUnitRunner.class)
public class CarbonioPreauthServiceTest {

    private static final String USER_ID = "0b6c7d8e-1f2a-4b3c-9d4e-5f6a7b8c9d0e";
    private static final String EXPECTED_AUTH = "Basic " + Base64.getEncoder()
            .encodeToString("carbonio:secret".getBytes(StandardCharsets.UTF_8));

    private Vertx vertx;
    private HttpClient httpClient;
    private HttpServer server;
    private int status;
    private String responseBody;
    private long responseDelay;

    @Before
    public void setUp(TestContext context) {
        vertx = Vertx.vertx();
        httpClient = vertx.createHttpClient();
        status = 200;
        responseBody = "{\"unread\":0}";
        responseDelay = 0;
        server = vertx.createHttpServer().requestHandler(req -> req.body().onSuccess(body -> {
            final boolean validRequest = "POST".equals(req.method().name())
                    && "/unread/carbonio".equals(req.path())
                    && EXPECTED_AUTH.equals(req.getHeader("Authorization"))
                    && USER_ID.equals(new JsonObject(body.toString()).getString("userId"));
            final Runnable reply = () -> req.response()
                    .setStatusCode(validRequest ? status : 400)
                    .end(validRequest ? responseBody : "{\"error\":\"invalid request\"}");
            if (responseDelay > 0) {
                vertx.setTimer(responseDelay, t -> reply.run());
            } else {
                reply.run();
            }
        }));
        server.listen(0, "localhost").onComplete(context.asyncAssertSuccess());
    }

    @After
    public void tearDown(TestContext context) {
        vertx.close().onComplete(context.asyncAssertSuccess());
    }

    private CarbonioPreauthService service() {
        return new CarbonioPreauthService("https://mail.example.com", "domain-key", httpClient,
                "http://localhost:" + server.actualPort() + "/", "carbonio", "secret", 1000L);
    }

    @Test
    public void testReturnsUnreadCount(TestContext context) {
        responseBody = "{\"unread\":7}";
        service().getUnreadCount(USER_ID).onComplete(context.asyncAssertSuccess(count -> {
            context.assertEquals(7, count);
        }));
    }

    @Test
    public void testUnknownAccountReturnsZero(TestContext context) {
        status = 404;
        responseBody = "{\"error\":\"not found\"}";
        service().getUnreadCount(USER_ID).onComplete(context.asyncAssertSuccess(count -> {
            context.assertEquals(0, count);
        }));
    }

    @Test
    public void testServerErrorFails(TestContext context) {
        status = 500;
        service().getUnreadCount(USER_ID).onComplete(context.asyncAssertFailure());
    }

    @Test
    public void testUnauthorizedFails(TestContext context) {
        status = 401;
        service().getUnreadCount(USER_ID).onComplete(context.asyncAssertFailure());
    }

    @Test
    public void testTimeoutFails(TestContext context) {
        responseDelay = 3000;
        service().getUnreadCount(USER_ID).onComplete(context.asyncAssertFailure());
    }

    @Test
    public void testUnreadCountDisabledReturnsZero(TestContext context) {
        status = 500;
        new CarbonioPreauthService("https://mail.example.com", "domain-key", httpClient, null, null, null, 1000L)
                .getUnreadCount(USER_ID).onComplete(context.asyncAssertSuccess(count -> {
            context.assertEquals(0, count);
        }));
    }
}
