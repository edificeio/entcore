package org.entcore.commonuser.position.impl;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.entcore.common.events.EventStoreFactory;
import org.entcore.common.user.DefaultFunctions;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.position.UserPosition;
import org.entcore.common.user.position.impl.DefaultUserPositionService;
import org.entcore.test.TestHelper;
import org.entcore.test.preparation.ClassTest;
import org.entcore.test.preparation.DataHelper;
import org.entcore.test.preparation.Profile;
import org.entcore.test.preparation.StructureTest;
import org.entcore.test.preparation.UserBookTest;
import org.entcore.test.preparation.UserTest;
import org.entcore.test.preparation.UserTestBuilder;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.Neo4jContainer;

@RunWith(VertxUnitRunner.class)
public class DefaultUserPositionServiceTest {
    private static final TestHelper test = TestHelper.helper();
    @ClassRule
    public static Neo4jContainer<?> neo4jContainer = test.database().createNeo4jContainer();

    private static DefaultUserPositionService service;

    private static final String DIRECTOR_USER_POS = "Directeur / Chef Etablissement";

    private static DataHelper dataHelper;

    static final UserTest adml = UserTestBuilder.anUserTest().id("user.adml")
            .login("user-adml")
            .firstName("Adée").lastName("Émelle")
            .profile(Profile.Personnel)
            .userBook(new UserBookTest("user.adml", "ine.user.adml", 1000, 0)).build();

    @BeforeClass
    public static void setUp(TestContext context) {
        final Vertx vertx = test.vertx();
        EventStoreFactory.getFactory().setVertx(vertx);
        service = new DefaultUserPositionService(vertx.eventBus(), false);
        dataHelper = DataHelper.init(context, neo4jContainer);
        prepareData().onComplete(context.asyncAssertSuccess());
    }

    /**
     * <h1>Goal</h1>
     * <p>Ensures that the function returns an empty result when the user does not exist.</p>
     */
    @Test
    public void testGetUserPosition_WithExistingPositionOnStructure_ShouldReturnUserPosition(final TestContext testContext) {
        final Async async = testContext.async();

        UserInfos userInfos = new UserInfos();
        userInfos.setStructures(Lists.newArrayList("my-structure-01"));
        userInfos.setUserId(adml.getId());
        userInfos.setFunctions(ImmutableMap.of(DefaultFunctions.ADMIN_LOCAL, new UserInfos.Function()));

        service.getUserPositions(DIRECTOR_USER_POS, null, userInfos , false)
                .onFailure(testContext::fail)
                .onSuccess(userPositions -> {
                    testContext.assertNotNull(userPositions, "userPositions should not be null");
                    testContext.assertEquals(userPositions.size(), 1, "userPositions should contains 1 user position");
                    testContext.assertTrue(userPositions.stream()
                                            .map(UserPosition::getName)
                                            .anyMatch( n -> n.equals(DIRECTOR_USER_POS.toUpperCase())));
                    async.complete();
                });

    }

    @Test
    public void testGetUserPosition_WithExistingPositionInHierarchy_IncludingSubStruct_shouldPass(final TestContext testContext) {
        final Async async = testContext.async();

        UserInfos userInfos = new UserInfos();
        userInfos.setStructures(Lists.newArrayList("my-structure-01", "my-structure-02"));
        userInfos.setUserId(adml.getId());
        userInfos.setFunctions(ImmutableMap.of(DefaultFunctions.SUPER_ADMIN, new UserInfos.Function()));

        service.getUserPositions(DIRECTOR_USER_POS, "my-structure-02", userInfos , true)
                .onFailure(testContext::fail)
                .onSuccess(userPositions -> {
                    testContext.assertNotNull(userPositions, "userPositions should not be null");
                    testContext.assertEquals(userPositions.size(), 1, "userPositions should contains 1 user position");
                    testContext.assertTrue(userPositions.stream()
                            .map(UserPosition::getName)
                            .anyMatch( n -> n.equals(DIRECTOR_USER_POS.toUpperCase())));
                    async.complete();
                });

    }

    @Test
    public void testGetUserPosition_WithExistingPositionInHierarchy_NotIncludingSubStruct_ShouldFail(final TestContext testContext) {
        final Async async = testContext.async();

        UserInfos userInfos = new UserInfos();
        userInfos.setStructures(Lists.newArrayList("my-structure-01", "my-structure-02"));
        userInfos.setUserId(adml.getId());
        userInfos.setFunctions(ImmutableMap.of(DefaultFunctions.SUPER_ADMIN, new UserInfos.Function()));

        service.getUserPositions(DIRECTOR_USER_POS, "my-structure-02", userInfos , true)
                .onFailure(testContext::fail)
                .onSuccess(userPositions -> {
                    testContext.assertNotNull(userPositions, "userPositions should not be null");
                    testContext.assertEquals(userPositions.size(), 1, "userPositions should contains 1 user position");
                    testContext.assertTrue(userPositions.stream()
                            .map(UserPosition::getName)
                            .anyMatch( n -> n.equals(DIRECTOR_USER_POS.toUpperCase())));
                    async.complete();
                });

    }

    public static Future<Void> prepareData() {
        dataHelper
            .start()
            .withStructure(new StructureTest("my-structure-01", "my structure 01"))
            .withStructure(new StructureTest("my-structure-02", "my structure 02"))
            .withUser(adml)
                .adml(adml.getId(), "my-structure-01")
            .withUserPosition(DIRECTOR_USER_POS.toLowerCase(), adml.getId(), "my-structure-01")
            .withStructureLink("my-structure-02", "my-structure-01");
        return dataHelper.execute();
    }

}
