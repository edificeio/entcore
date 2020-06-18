package org.entcore.auth;

import org.entcore.auth.users.DefaultUserAuthAccount;
import org.entcore.auth.users.UserAuthAccount;
import org.entcore.test.TestHelper;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.Neo4jContainer;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class UserAuthAccountTest {
    private static final TestHelper test = TestHelper.helper();

    @ClassRule
    public static Neo4jContainer<?> neo4jContainer = test.database().createNeo4jContainer();
    static final JsonObject authAccountConfig = new JsonObject().put("emailConfig",
            new JsonObject().put("email", "ne-pas-repondre@cg77.fr").put("host", "http://localhost:8090"));
    static UserAuthAccount authAccount = new DefaultUserAuthAccount(test.vertx(), authAccountConfig);

    @BeforeClass
    public static void setUp(TestContext context) throws Exception {
        test.database().initNeo4j(context, neo4jContainer);
    }

    @Test
    public void testAccountShouldActivate(TestContext context) {
        final Async async = context.async();
        test.directory().createInactiveUser("user0", "activationCode0", "user0@test.com").setHandler(resAcUser -> {
            context.assertTrue(resAcUser.succeeded());
            final HttpServerRequest request = test.http().post("/auth/activation");
            authAccount.activateAccount("user0", "activationCode0", "password0", "user0@test.com", "0666666666",
                    "theme1", request, resActiv -> {
                        context.assertTrue(resActiv.isRight());
                        final String id = resActiv.right().getValue();
                        test.directory().fetchOneUser(id).setHandler(resUser -> {
                            context.assertTrue(resUser.succeeded());
                            context.assertNull(resUser.result().getString("activationCode"));
                            context.assertNotNull(resUser.result().getString("password"));
                            context.assertEquals("0666666666", resUser.result().getString("mobile"));
                            async.complete();
                        });
                    });
        });
    }

    @Test
    public void testAccountShouldActivateAndRevalidateTerms(TestContext context) {
        final Async async = context.async();
        test.directory().createInactiveUser("user1", "activationCode1", "user1@test.com").setHandler(resAcUser -> {
            context.assertTrue(resAcUser.succeeded());
            final HttpServerRequest request = test.http().post("/auth/activation");
            authAccount.activateAccountWithRevalidateTerms("user1", "activationCode1", "password1", "user1@test.com",
                    "0666666666", "theme1", request, resActiv -> {
                        context.assertTrue(resActiv.isRight());
                        final String id = resActiv.right().getValue();
                        test.directory().fetchOneUser(id).setHandler(resUser -> {
                            context.assertTrue(resUser.succeeded());
                            context.assertEquals(true, resUser.result().getBoolean("needRevalidateTerms"));
                            async.complete();
                        });
                    });
        });
    }

    @Test
    public void testAccountShouldNotActivateOnBadActivationCode(TestContext context) {
        final Async async = context.async();
        test.directory().createInactiveUser("user2", "activationCode2", "user2@test.com").setHandler(resAcUser -> {
            context.assertTrue(resAcUser.succeeded());
            final HttpServerRequest request = test.http().post("/auth/activation");
            authAccount.activateAccount("user2", "fake", "password2", "user2@test.com", "0666666666", "theme1", request,
                    resActiv -> {
                        context.assertTrue(resActiv.isLeft());
                        context.assertEquals("activation.error", resActiv.left().getValue());
                        async.complete();
                    });
        });
    }

    @Test
    public void testAccountShouldNotActivateButLogin(TestContext context) {
        final Async async = context.async();
        test.directory().createActiveUser("user3", "password3", "user3@test.com").setHandler(resAcUser -> {
            context.assertTrue(resAcUser.succeeded());
            final HttpServerRequest request = test.http().post("/auth/activation");
            authAccount.activateAccount("user3", "fake", "password3", "user3@test.com", "0666666666", "theme1", request,
                    resActiv -> {
                        context.assertTrue(resActiv.isRight());
                        async.complete();
                    });
        });
    }

    @Test
    public void testAccountShouldActivateByAlias(TestContext context) {
        final Async async = context.async();
        test.directory().createInactiveUser("user4", "userAlias4", "activationCode4", "user4@test.com")
                .setHandler(resAcUser -> {
                    context.assertTrue(resAcUser.succeeded());
                    final HttpServerRequest request = test.http().post("/auth/activation");
                    authAccount.activateAccountByLoginAlias("userAlias4", "activationCode4", "password4",
                            "user4@test.com", "0666666666", "theme1", request, resActiv -> {
                                context.assertTrue(resActiv.isRight());
                                async.complete();
                            });
                });
    }

    @Test
    public void testAccountShouldNotActivateButLoginByAlias(TestContext context) {
        final Async async = context.async();
        test.directory().createActiveUser("user5", "userAlias5", "password5", "user5@test.com")
                .setHandler(resAcUser -> {
                    context.assertTrue(resAcUser.succeeded());
                    final HttpServerRequest request = test.http().post("/auth/activation");
                    authAccount.activateAccountByLoginAlias("userAlias5", "fake", "password5", "user5@test.com",
                            "0666666666", "theme1", request, resActiv -> {
                                context.assertTrue(resActiv.isRight());
                                async.complete();
                            });
                });
    }

    @Test
    public void testAccountShouldMatchActivationCode(TestContext context) {
        final Async async = context.async();
        test.directory().createInactiveUser("user6", "activationCode6", "user6@test.com").setHandler(resAcUser -> {
            context.assertTrue(resAcUser.succeeded());
            authAccount.matchActivationCode("user6", "activationCode6", resActiv -> {
                context.assertTrue(resActiv);
                async.complete();
            });
        });
    }

    @Test
    public void testAccountShouldMatchActivationCodeByAlias(TestContext context) {
        final Async async = context.async();
        test.directory().createInactiveUser("user7", "userAlias7", "activationCode7", "user7@test.com")
                .setHandler(resAcUser -> {
                    context.assertTrue(resAcUser.succeeded());
                    authAccount.matchActivationCodeByLoginAlias("userAlias7", "activationCode7", resActiv -> {
                        context.assertTrue(resActiv);
                        async.complete();
                    });
                });
    }

    @Test
    public void testAccountShouldNotMatchActivationCode(TestContext context) {
        final Async async = context.async();
        test.directory().createInactiveUser("user8", "activationCode8", "user8@test.com").setHandler(resAcUser -> {
            context.assertTrue(resAcUser.succeeded());
            authAccount.matchActivationCode("user8", "bad", resActiv -> {
                context.assertFalse(resActiv);
                async.complete();
            });
        });
    }

    @Test
    public void testAccountShouldMatchResetCode(TestContext context) {
        final Async async = context.async();
        test.directory().createActiveUser("user9", "activationCode9", "user9@test.com").compose(resAcUser -> {
            return test.directory().resetUser(resAcUser, "resetCode9");
        }).setHandler(resAcUser -> {
            context.assertTrue(resAcUser.succeeded());
            authAccount.matchResetCode("user9", "resetCode9", resActiv -> {
                context.assertTrue(resActiv);
                async.complete();
            });
        });
    }

    @Test
    public void testAccountShouldMatchResetCodeByAlias(TestContext context) {
        final Async async = context.async();
        test.directory().createActiveUser("user10", "userAlias10", "activationCode10", "user10@test.com")
                .compose(resAcUser -> {
                    return test.directory().resetUser(resAcUser, "resetCode10");
                }).setHandler(resAcUser -> {
                    context.assertTrue(resAcUser.succeeded());
                    authAccount.matchResetCodeByLoginAlias("userAlias10", "resetCode10", resActiv -> {
                        context.assertTrue(resActiv);
                        async.complete();
                    });
                });
    }

    @Test
    public void testAccountShouldNotMatchResetCode(TestContext context) {
        final Async async = context.async();
        test.directory().createActiveUser("user11", "activationCode11", "user11@test.com").compose(resAcUser -> {
            return test.directory().resetUser(resAcUser, "resetCode11");
        }).setHandler(resAcUser -> {
            context.assertTrue(resAcUser.succeeded());
            authAccount.matchResetCode("user11", "bad", resActiv -> {
                context.assertFalse(resActiv);
                async.complete();
            });
        });
    }

    @Test
    public void testAccountShouldResetPassword(TestContext context) {
        final Async async = context.async();
        test.directory().createActiveUser("user12", "activationCode12", "user12@test.com").compose(resAcUser -> {
            return test.directory().resetUser(resAcUser, "resetCode12");
        }).setHandler(resAcUser -> {
            context.assertTrue(resAcUser.succeeded());
            authAccount.resetPassword("user12", "resetCode12", "password12", resActiv -> {
                context.assertTrue(resActiv);
                async.complete();
            });
        });
    }

    @Test
    public void testAccountShouldNotResetPassword(TestContext context) {
        final Async async = context.async();
        test.directory().createActiveUser("user13", "activationCode13", "user13@test.com").compose(resAcUser -> {
            return test.directory().resetUser(resAcUser, "resetCode13");
        }).setHandler(resAcUser -> {
            context.assertTrue(resAcUser.succeeded());
            authAccount.resetPassword("user13", "bad", "password13", resActiv -> {
                context.assertFalse(resActiv);
                async.complete();
            });
        });
    }
}