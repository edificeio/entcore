package org.entcore.auth;

import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.Collectors;

import org.entcore.auth.users.DefaultUserAuthAccount;
import org.entcore.auth.users.UserAuthAccount;
import org.entcore.common.email.EmailFactory;
import org.entcore.common.events.EventStore;
import org.entcore.common.events.EventStoreFactory;
import org.entcore.test.DirectoryTestHelper;
import org.entcore.test.TestHelper;

import fr.wseduc.webutils.collections.SharedDataHelper;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.Neo4jContainer;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
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
            new JsonObject().put("postgresql", new JsonObject()
                            .put("host", "localhost")
                            .put("database", "test")
                            .put("user", "test")
                            .put("password", "test"))
                    .put("email", "ne-pas-repondre@cg77.fr")
                    .put("host", "http://localhost:8090")
                    .put("type", "postgresql"));
    static EventStore eStore;
    static UserAuthAccount authAccount;

    @BeforeClass
    public static void setUp(TestContext context) throws Exception {
        EventStoreFactory.getFactory().setVertx(test.vertx());
        eStore = EventStoreFactory.getFactory().getEventStore(Auth.class.getSimpleName());
        // EmailFactory is a singleton whose config is only populated through build(); without this the
        // sender falls back to SmtpSender and NPEs on an uninitialized SharedDataHelper.
        SharedDataHelper.getInstance().init(test.vertx());
        final Async async = context.async();
        EmailFactory.build(test.vertx(), authAccountConfig).onComplete(res -> {
            context.assertTrue(res.succeeded());
            authAccount = new DefaultUserAuthAccount(test.vertx(), authAccountConfig, eStore, new HashMap<>());
            test.database().initNeo4j(context, neo4jContainer);
            async.complete();
        });
    }

    @Test
    public void testAccountShouldActivate(TestContext context) {
        final Async async = context.async();
        test.directory().createInactiveUser("user0", "activationCode0", "User0@test.com").onComplete(resAcUser -> {
            context.assertTrue(resAcUser.succeeded());
            final HttpServerRequest request = test.http().post("/auth/activation");
            authAccount.activateAccount("user0", "activationCode0", "password0", "User0@test.com", "0666666666",
                    "theme1", request, resActiv -> {
                        context.assertTrue(resActiv.isRight());
                        final String id = resActiv.right().getValue();
                        test.directory().fetchOneUser(id).onComplete(resUser -> {
                            context.assertTrue(resUser.succeeded());
                            context.assertNull(resUser.result().getString("activationCode"));
                            context.assertNotNull(resUser.result().getString("password"));
                            context.assertEquals("0666666666", resUser.result().getString("mobile"));
                            context.assertEquals("User0@test.com", resUser.result().getString("email"));
                            context.assertEquals("user0@test.com", resUser.result().getString("emailSearchField"));
                            async.complete();
                        });
                    });
        });
    }

    @Test
    public void testAccountShouldActivateAndRevalidateTerms(TestContext context) {
        final Async async = context.async();
        test.directory().createInactiveUser("user1", "activationCode1", "user1@test.com").onComplete(resAcUser -> {
            context.assertTrue(resAcUser.succeeded());
            final HttpServerRequest request = test.http().post("/auth/activation");
            authAccount.activateAccountWithRevalidateTerms("user1", "activationCode1", "password1", "user1@test.com",
                    "0666666666", "theme1", request, resActiv -> {
                        context.assertTrue(resActiv.isRight());
                        final String id = resActiv.right().getValue();
                        test.directory().fetchOneUser(id).onComplete(resUser -> {
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
        test.directory().createInactiveUser("user2", "activationCode2", "user2@test.com").onComplete(resAcUser -> {
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
        test.directory().createActiveUser("user3", "password3", "user3@test.com").onComplete(resAcUser -> {
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
                .onComplete(resAcUser -> {
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
                .onComplete(resAcUser -> {
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
        test.directory().createInactiveUser("user6", "activationCode6", "user6@test.com")
        .onComplete(resAcUser -> {
            context.assertTrue(resAcUser.succeeded());
            authAccount.matchActivationCode("user6", "activationCode6", resActiv -> {
                context.assertTrue(resActiv.isRight());
                async.complete();
            });
        });
    }

    @Test
    public void testAccountShouldMatchActivationCodeByAlias(TestContext context) {
        final Async async = context.async();
        test.directory().createInactiveUser("user7", "userAlias7", "activationCode7", "user7@test.com")
        .onComplete(resAcUser -> {
            context.assertTrue(resAcUser.succeeded());
            authAccount.matchActivationCodeByLoginAlias("userAlias7", "activationCode7", resActiv -> {
                context.assertTrue(resActiv.isRight());
                async.complete();
            });
        });
    }

    @Test
    public void testAccountShouldMatchActivationCodeAndFlattenLevelsAcrossStructures(TestContext context) {
        final Async async = context.async();
        final DirectoryTestHelper dir = test.directory();
        // A user attached to several structures used to break the query (non.unique.result -> not.found).
        // The levels of every structure must now be flattened and de-duplicated into a single array.
        final JsonObject ids = new JsonObject();
        dir.createInactiveUser("user14", "activationCode14", "user14@test.com")
            .compose(userId -> {
                ids.put("user", userId);
                return dir.createStructure("struct14a", "UAI14A");
            })
            .compose(structA -> {
                ids.put("structA", structA);
                return dir.setLevelsOfEducation(structA, new JsonArray().add(1));
            })
            .compose(v -> dir.createStructure("struct14b", "UAI14B"))
            .compose(structB -> {
                ids.put("structB", structB);
                // Overlaps struct14a on level 1 to also exercise de-duplication.
                return dir.setLevelsOfEducation(structB, new JsonArray().add(1).add(2));
            })
            .compose(v -> dir.createProfileGroup("group14a"))
            .compose(groupA -> {
                ids.put("groupA", groupA);
                return dir.attachGroupToStruct(groupA, ids.getString("structA"));
            })
            .compose(v -> dir.attachUserToGroup(ids.getString("user"), ids.getString("groupA")))
            .compose(v -> dir.createProfileGroup("group14b"))
            .compose(groupB -> {
                ids.put("groupB", groupB);
                return dir.attachGroupToStruct(groupB, ids.getString("structB"));
            })
            .compose(v -> dir.attachUserToGroup(ids.getString("user"), ids.getString("groupB")))
            .onComplete(setup -> {
                context.assertTrue(setup.succeeded());
                authAccount.matchActivationCode("user14", "activationCode14", resActiv -> {
                    context.assertTrue(resActiv.isRight());
                    final JsonArray levels = resActiv.right().getValue().getJsonArray("levels");
                    context.assertNotNull(levels);
                    context.assertEquals(2, levels.size());
                    context.assertTrue(levels.stream().map(o -> ((Number) o).intValue())
                            .collect(Collectors.toSet()).containsAll(Arrays.asList(1, 2)));
                    async.complete();
                });
            });
    }

    @Test
    public void testAccountShouldNotMatchActivationCode(TestContext context) {
        final Async async = context.async();
        test.directory().createInactiveUser("user8", "activationCode8", "user8@test.com")
        .onComplete(resAcUser -> {
            context.assertTrue(resAcUser.succeeded());
            authAccount.matchActivationCode("user8", "bad", resActiv -> {
                context.assertFalse(resActiv.isRight());
                async.complete();
            });
        });
    }

    @Test
    public void testAccountShouldMatchResetCode(TestContext context) {
        final Async async = context.async();
        test.directory().createActiveUser("user9", "activationCode9", "user9@test.com")
        .compose(resAcUser -> test.directory().resetUser(resAcUser, "resetCode9"))
        .onComplete(resAcUser -> {
            context.assertTrue(resAcUser.succeeded());
            authAccount.matchResetCode("user9", "resetCode9", resActiv -> {
                context.assertTrue(resActiv.isRight());
                async.complete();
            });
        });
    }

    @Test
    public void testAccountShouldMatchResetCodeByAlias(TestContext context) {
        final Async async = context.async();
        test.directory().createActiveUser("user10", "userAlias10", "activationCode10", "user10@test.com")
        .compose(resAcUser -> test.directory().resetUser(resAcUser, "resetCode10"))
        .onComplete(resAcUser -> {
            context.assertTrue(resAcUser.succeeded());
            authAccount.matchResetCodeByLoginAlias("userAlias10", "resetCode10", resActiv -> {
                context.assertTrue(resActiv.isRight());
                async.complete();
            });
        });
    }

    @Test
    public void testAccountShouldNotMatchResetCode(TestContext context) {
        final Async async = context.async();
        test.directory().createActiveUser("user11", "activationCode11", "user11@test.com")
        .compose(resAcUser -> test.directory().resetUser(resAcUser, "resetCode11"))
        .onComplete(resAcUser -> {
            context.assertTrue(resAcUser.succeeded());
            authAccount.matchResetCode("user11", "bad", resActiv -> {
                context.assertFalse(resActiv.isRight());
                async.complete();
            });
        });
    }

    @Test
    public void testAccountShouldResetPassword(TestContext context) {
        final Async async = context.async();
        test.directory()
                .createActiveUser("user12", "activationCode12", "user12@test.com")
                .compose(resAcUser -> test.directory().resetUser(resAcUser, "resetCode12"))
                .onComplete(resAcUser -> {
                    context.assertTrue(resAcUser.succeeded());
                    authAccount.resetPassword("user12", "resetCode12", "password12", null, resActiv -> {
                        context.assertTrue(resActiv != null);
                        async.complete();
                    });
                });
    }

  @Test
  public void testAccountShouldNotBeAbleToResetPasswordWithSamePassword(TestContext context) {
    final Async async = context.async();
    test.directory()
      .createActiveUser("user23", "activationCode12", "user12@test.com")
      .compose(resAcUser -> test.directory().resetUser(resAcUser, "resetCode12"))
      .onComplete(resAcUser -> {
        context.assertTrue(resAcUser.succeeded());
        authAccount.resetPassword("user23", "resetCode12", "password12", null, resActiv -> {
          context.assertTrue(resActiv != null);
          authAccount.resetPassword("user23", "resetCode12", "password12", null, resActiv2 -> {
            context.assertFalse(resActiv2 != null);
            async.complete();
          });
        });
      });
  }

    @Test
    public void testAccountShouldNotResetPassword(TestContext context) {
        final Async async = context.async();
        test.directory().createActiveUser("user13", "activationCode13", "user13@test.com").compose(resAcUser -> {
            return test.directory().resetUser(resAcUser, "resetCode13");
        }).onComplete(resAcUser -> {
            context.assertTrue(resAcUser.succeeded());
            authAccount.resetPassword("user13", "bad", "password13", null, resActiv -> {
                context.assertFalse(resActiv != null);
                async.complete();
            });
        });
    }
}