package org.entcore.directory;

import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

import org.entcore.common.datavalidation.EmailValidation;
import org.entcore.common.datavalidation.MobileValidation;
import org.entcore.common.datavalidation.utils.DataStateUtils;
import org.entcore.common.datavalidation.utils.UserValidationFactory;
import org.entcore.common.email.EmailFactory;
import org.entcore.common.sms.SmsSenderFactory;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.entcore.test.TestHelper;
import org.testcontainers.containers.Neo4jContainer;

@RunWith(VertxUnitRunner.class)
public class ValidationTest {
    private static final TestHelper test = TestHelper.helper();
    private static String VALID_MAIL = "valid-email@test.com";
    private static String VALID_MOBILE = "+33012345678";
    private static String userId;

    @ClassRule
    public static Neo4jContainer<?> neo4jContainer = test.database().createNeo4jContainer();

    @BeforeClass
    public static void setUp(TestContext context) throws Exception {
        test.initSharedData();
        test.database().initNeo4j(context, neo4jContainer);

        // pseudo-emailer config
        JsonObject validationConfig = test.file().jsonFromResource("config/validations.json");

        // Setup validations factory
		UserValidationFactory.build(test.vertx(), validationConfig).onComplete(ar -> {
            final Async async = context.async();

            test.directory().createActiveUser("login", "password", "email@test.com")
            .onComplete(res -> {
                context.assertTrue(res.succeeded());
                userId = res.result();
                async.complete();
            });
        });
    }

    @Test
    public void testEmailValidationScenario(TestContext context) {
        final Async async = context.async();
        final EventBus eb = test.vertx().eventBus();

        EmailValidation.getDetails(eb, userId)
        // 1) check email is not verified
        .compose( details -> {
            final JsonObject emailState = details.getJsonObject("emailState");
            context.assertEquals(details.getString("email"), "email@test.com");
            context.assertNotNull(details.getInteger("waitInSeconds"));
            // Before first validation check, emailState does not exist in neo4j and is null here.
            context.assertNull(emailState);
            context.assertEquals(DataStateUtils.getState(emailState), DataStateUtils.UNCHECKED);

            // 2) try verifying it
            return EmailValidation.setPending(eb, userId, VALID_MAIL);
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
            return EmailValidation.tryValidate(eb, userId, "DEADBEEF")
            .map( result -> {
                final String s = result.getString("state");
                context.assertNotEquals(s, "unchecked");
                context.assertNotEquals(s, "valid");
                final Integer tries = result.getInteger("tries");
                context.assertTrue( tries == 1 );
                return validCode;
            });
        })
        // 5) try the correct code
        .compose( validCode -> {
            context.assertNotNull(validCode);
            context.assertEquals(validCode.length(), 6);
            return EmailValidation.tryValidate(eb, userId, validCode);
        })
        .compose( result -> {
            final String s = result.getString("state");
            context.assertEquals(s, "valid");
            return EmailValidation.getDetails(eb, userId);
        })
        // 6) check email is verified
        .compose( details -> {
            final JsonObject emailState = details.getJsonObject("emailState");
            context.assertEquals(details.getString("email"), VALID_MAIL);
            context.assertEquals(DataStateUtils.getValid(emailState), VALID_MAIL);
            context.assertEquals(DataStateUtils.getState(emailState), DataStateUtils.VALID);
            return EmailValidation.isValid(eb, userId);
        })
        // 7) confirm email is now valid
        .map( valid -> {
            final String state = valid.getString("state");
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

    @Test
    public void testMobileValidationScenario(TestContext context) {
        final Async async = context.async();
        final EventBus eb = test.vertx().eventBus();

        MobileValidation.getDetails(eb, userId)
        // 1) check mobile is not verified
        .compose( details -> {
            final JsonObject mobileState = details.getJsonObject("mobileState");
            context.assertNull(details.getString("mobile"));
            context.assertNotNull(details.getInteger("waitInSeconds"));
            // Before first validation check, mobileState does not exist in neo4j and is null here.
            context.assertNull(mobileState);
            context.assertEquals(DataStateUtils.getState(mobileState), DataStateUtils.UNCHECKED);

            // 2) try verifying it
            return MobileValidation.setPending(eb, userId, VALID_MOBILE);
        })
        // 3) check pending data
        .map( mobileState -> {
            context.assertNotNull(mobileState);
            context.assertEquals(DataStateUtils.getState(mobileState), DataStateUtils.PENDING);
            context.assertNotNull(DataStateUtils.getValid(mobileState));
            context.assertEquals(DataStateUtils.getPending(mobileState), VALID_MOBILE);
            return DataStateUtils.getKey(mobileState);
        })
        // 4) try a wrong code once
        .compose( validCode -> {
            return MobileValidation.tryValidate(eb, null, userId, "DEADBEEF")
            .map( result -> {
                final String s = result.getString("state");
                context.assertNotEquals(s, "unchecked");
                context.assertNotEquals(s, "valid");
                final Integer tries = result.getInteger("tries");
                context.assertTrue( tries == 1 );
                return validCode;
            });
        })
        // 5) try the correct code
        .compose( validCode -> {
            context.assertNotNull(validCode);
            context.assertEquals(validCode.length(), 6);
            return MobileValidation.tryValidate(eb, null, userId, validCode);
        })
        .compose( result -> {
            final String s = result.getString("state");
            context.assertEquals(s, "valid");
            return MobileValidation.getDetails(eb, userId);
        })
        // 6) check mobile is verified
        .compose( details -> {
            final JsonObject mobileState = details.getJsonObject("mobileState");
            context.assertEquals(details.getString("mobile"), VALID_MOBILE);
            context.assertEquals(DataStateUtils.getValid(mobileState), VALID_MOBILE);
            context.assertEquals(DataStateUtils.getState(mobileState), DataStateUtils.VALID);
            return MobileValidation.isValid(eb, userId);
        })
        // 7) confirm mobile is now valid
        .map( valid -> {
            final String state = valid.getString("state");
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
