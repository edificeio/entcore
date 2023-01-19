package org.entcore.directory;

import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

import org.entcore.common.emailstate.EmailState;
import org.entcore.common.emailstate.DataStateUtils;
import org.entcore.directory.emailstate.UserValidationHandler;
import org.entcore.directory.services.impl.DefaultMailValidationService;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.entcore.test.TestHelper;
import org.testcontainers.containers.Neo4jContainer;

@RunWith(VertxUnitRunner.class)
public class EmailStateTest {
    private static final TestHelper test = TestHelper.helper();
    private static String VALID_MAIL = "valid-email@test.com";
    private static String userId;

    @ClassRule
    public static Neo4jContainer<?> neo4jContainer = test.database().createNeo4jContainer();

    @BeforeClass
    public static void setUp(TestContext context) throws Exception {
        test.initSharedData();
        test.database().initNeo4j(context, neo4jContainer);

        // Instanciate an email validation service
		test.vertx().eventBus().localConsumer(EmailState.BUS_ADDRESS, new UserValidationHandler(
			new JsonObject(),
			new DefaultMailValidationService(null)
		));

        final Async async = context.async();
        
        test.directory().createActiveUser("login", "password", "email@test.com")
        .onComplete(res -> {
            context.assertTrue(res.succeeded());
            userId = res.result();
            async.complete();
        });
    }

    @Test
    public void testEmailValidationScenario(TestContext context) {
        final Async async = context.async();
        final EventBus eb = test.vertx().eventBus();

        EmailState.getDetails(eb, userId)
        // 1) check email is not verified
        .compose( details -> {
            final JsonObject emailState = details.getJsonObject("emailState");
            context.assertEquals(details.getString("email"), "email@test.com");
            context.assertNotNull(details.getInteger("waitInSeconds"));
            // Before first validation check, emailState does not exist in neo4j and is null here.
            context.assertNull(emailState);
            context.assertEquals(DataStateUtils.getState(emailState), DataStateUtils.UNCHECKED);

            // 2) try verifying it
            return EmailState.setPending(eb, userId, VALID_MAIL);
        })
        // 3) check pending data
        .map( emailState -> {
            context.assertNotNull(emailState);
            context.assertEquals(DataStateUtils.getState(emailState), DataStateUtils.PENDING);
            context.assertNotNull(DataStateUtils.getValid(emailState));
            context.assertEquals(DataStateUtils.getPending(emailState), VALID_MAIL);
            return DataStateUtils.getKey(emailState);
        })
        // 4) try a wrong code once
        .compose( validCode -> {
            return EmailState.tryValidate(eb, userId, "DEADBEEF")
            .map( result -> {
                final String s = result.getString("state");
                context.assertNotEquals(s, "unchecked");
                context.assertNotEquals(s, "valid");
                final Integer tries = result.getInteger("tries");
                context.assertTrue( tries == 4 );
                return validCode;
            });
        })
        // 5) try the correct code
        .compose( validCode -> {
            context.assertNotNull(validCode);
            context.assertEquals(validCode.length(), 6);
            return EmailState.tryValidate(eb, userId, validCode);
        })
        .compose( result -> {
            final String s = result.getString("state");
            context.assertEquals(s, "valid");
            return EmailState.getDetails(eb, userId);
        })
        // 6) check email is verified
        .compose( details -> {
            final JsonObject emailState = details.getJsonObject("emailState");
            context.assertEquals(details.getString("email"), VALID_MAIL);
            context.assertEquals(DataStateUtils.getValid(emailState), VALID_MAIL);
            context.assertEquals(DataStateUtils.getState(emailState), DataStateUtils.VALID);
            return EmailState.isValid(eb, userId);
        })
        // 7) confirm email is now valid
        .map( valid -> {
            String state = valid.getString("state");
            context.assertEquals(state, "valid");
            return true;
        })
        .onComplete( res -> {
            if( res.failed() ) {
                res.cause().printStackTrace();
            }
            context.assertTrue( res.succeeded() );
            async.complete();
        });
    }
}
