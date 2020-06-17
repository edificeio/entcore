package org.entcore.auth;

import org.entcore.auth.controllers.AuthController;
import org.entcore.auth.security.AuthResourcesProvider;
import org.entcore.common.neo4j.Neo;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.user.UserInfos;
import org.entcore.test.TestHelper;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.Neo4jContainer;

import fr.wseduc.webutils.http.Binding;
import fr.wseduc.webutils.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class AuthResourceProviderTest {
    private static TestHelper test = TestHelper.helper();
    @ClassRule
    public static Neo4jContainer<?> neo4jContainer = test.database().createNeo4jContainer();
    static AuthResourcesProvider authProvider;

    @BeforeClass
    public static void setUp(TestContext context) throws Exception {
        test.database().initNeo4j(context, neo4jContainer);
        authProvider = new AuthResourcesProvider(new Neo(test.vertx(), test.vertx().eventBus(), null));
    }

    @Test
    public void testAuthProviderShouldNotAllowNonAdmlToBlockUser(TestContext context) {
        final HttpServerRequest req = test.http().put("/auth/block/1");
        final Binding binding = test.http().binding(HttpMethod.POST, AuthController.class, "blockUser");
        final UserInfos user = test.http().sessionUser();
        final Async async = context.async();
        authProvider.authorize(req, binding, user, res -> {
            context.assertFalse(res);
            async.complete();
        });
    }
}