package org.entcore.commonuser.position.impl;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.entcore.common.user.UserInfos;
import org.entcore.directory.services.impl.DefaultPreferenceCacheService;
import org.entcore.directory.services.impl.DefaultPreferenceService;
import org.entcore.test.TestHelper;
import org.entcore.test.preparation.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.testcontainers.containers.Neo4jContainer;

@RunWith(VertxUnitRunner.class)
public class DefaultPreferenceServiceTest {

    private static final TestHelper test = TestHelper.helper();
    @ClassRule
    public static Neo4jContainer<?> neo4jContainer = test.database().createNeo4jContainer();
    private DefaultPreferenceCacheService cacheService;

    private static DefaultPreferenceService service;
    private static DataHelper dataHelper;

    static final UserTest adml = UserTestBuilder.anUserTest().id("user.adml")
            .login("user-adml")
            .firstName("Adée").lastName("Émelle")
            .profile(Profile.Personnel)
            .userBook(new UserBookTest("user.adml", "ine.user.adml", 1000, 0)).build();

    static final UserTest corruptedPreferenceUser = UserTestBuilder.anUserTest().id("user.corrupted")
            .login("user-corrupted")
            .firstName("Jean").lastName("Quille")
            .profile(Profile.Personnel)
            .build();

    @BeforeClass
    public static void setUp(TestContext context) {
        final Vertx vertx = test.vertx();
        service = new DefaultPreferenceService(new DefaultPreferenceCacheService(vertx.eventBus()));
        dataHelper = DataHelper.init(context, neo4jContainer);
        prepareData().onComplete(context.asyncAssertSuccess());
    }

    @Before
    public void setUp() {
        cacheService = Mockito.mock(DefaultPreferenceCacheService.class);
        service = new DefaultPreferenceService(cacheService);
    }

    @Test
    public void testGetPreferences_WithThemePreference_shouldReturnCg77(final TestContext testContext) {
        final Async async = testContext.async();

        UserInfos userInfos = new UserInfos();
        userInfos.setUserId(adml.getId());

        service.getPreferences(userInfos, new JsonObject())
                .onFailure(testContext::fail)
                .onSuccess(userPreferenceDto -> {
                    testContext.assertNotNull(userPreferenceDto.getTheme());
                    testContext.assertEquals(userPreferenceDto.getTheme().getTheme(), "cg77", "Theme should be cg77");
                    Mockito.verify(cacheService, Mockito.atLeastOnce()).refreshPreferences(userInfos, userPreferenceDto);
                    async.complete();
                });

    }


    @Test
    public void testGetPreferences_WithDefaultLanguage_shouldReturnFr(final TestContext testContext) {
        final Async async = testContext.async();

        UserInfos userInfos = new UserInfos();
        userInfos.setUserId(adml.getId());

        service.getPreferences(userInfos, new JsonObject())
                .onFailure(testContext::fail)
                .onSuccess(userPreferenceDto -> {
                    testContext.assertNotNull(userPreferenceDto.getLanguage());
                    testContext.assertNotNull(userPreferenceDto.getLanguage().getLanguages());
                    String defaultLanguage = userPreferenceDto.getLanguage().getLanguages().get("default-domain");
                    testContext.assertEquals(defaultLanguage, "fr", "Default language should be fr");
                    Mockito.verify(cacheService, Mockito.atLeastOnce()).refreshPreferences(userInfos, userPreferenceDto);
                    async.complete();
                });

    }

    @Test
    public void testGetPreferences_WithHomePage_shouldReturnBetaTrue(final TestContext testContext) {
        final Async async = testContext.async();

        UserInfos userInfos = new UserInfos();
        userInfos.setUserId(adml.getId());

        service.getPreferences(userInfos, new JsonObject())
                .onFailure(testContext::fail)
                .onSuccess(userPreferenceDto -> {
                    testContext.assertNotNull(userPreferenceDto.getHomePage());
                    testContext.assertTrue(userPreferenceDto.getHomePage().isBetaEnabled(),
                            "Beta should be enabled");
                    Mockito.verify(cacheService, Mockito.atLeastOnce()).refreshPreferences(userInfos, userPreferenceDto);
                    async.complete();
                });

    }

    @Test
    public void testGetPreferences_WithApplicationPreference_shouldReturnBookmarksAndApplications(final TestContext testContext) {
        final Async async = testContext.async();

        UserInfos userInfos = new UserInfos();
        userInfos.setUserId(adml.getId());

        service.getPreferences(userInfos, new JsonObject())
                .onFailure(testContext::fail)
                .onSuccess(userPreferenceDto -> {
                    testContext.assertNotNull(userPreferenceDto.getApps());
                    testContext.assertNotNull(userPreferenceDto.getApps().getBookmarks());
                    testContext.assertNotNull(userPreferenceDto.getApps().getApplications());
                    testContext.assertTrue(userPreferenceDto.getApps().getBookmarks().size() == 1);
                    testContext.assertTrue(userPreferenceDto.getApps().getApplications().size() == 2);
                    testContext.assertTrue(userPreferenceDto.getApps().getApplications().contains("news"),
                            "Applications should contain news");
                    Mockito.verify(cacheService, Mockito.atLeastOnce()).refreshPreferences(userInfos, userPreferenceDto);
                    async.complete();
                });
    }

    @Test
    public void testGetPreferences_WithCorruptedLanguagePreference_shouldReturnNullLanguage(final TestContext testContext) {
        final Async async = testContext.async();

        UserInfos userInfos = new UserInfos();
        userInfos.setUserId(corruptedPreferenceUser.getId());

        service.getPreferences(userInfos, new JsonObject())
                .onFailure(testContext::fail)
                .onSuccess(userPreferenceDto -> {
                    testContext.assertNotNull(userPreferenceDto.getApps());
                    testContext.assertNotNull(userPreferenceDto.getHomePage());
                    testContext.assertNotNull(userPreferenceDto.getTheme());
                    testContext.assertNull(userPreferenceDto.getLanguage(), "Language should be null");
                    Mockito.verify(cacheService, Mockito.atLeastOnce()).refreshPreferences(userInfos, userPreferenceDto);
                    async.complete();
                });
    }

    @Test
    public void testGetPreferences_WithCache_shouldReturnFromTheCache(final TestContext testContext) {
        final Async async = testContext.async();

        UserInfos userInfos = new UserInfos();
        userInfos.setUserId(adml.getId());

        service.getPreferences(userInfos, new JsonObject().put("cache",
                        new JsonObject().put("preferences",
                                new JsonObject().put("language", "{\"default-domain\" : \"en\"}"))))
                .onFailure(testContext::fail)
                .onSuccess(userPreferenceDto -> {
                    testContext.assertNotNull(userPreferenceDto.getLanguage(), "Language should be null");
                    testContext.assertEquals(userPreferenceDto.getLanguage()
                            .getLanguages().get("default-domain"), "en", "Language should be null");
                    Mockito.verify(cacheService, Mockito.never()).refreshPreferences(userInfos, userPreferenceDto);
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
            .withPreference(adml, "language", "{\"default-domain\" : \"fr\"}")
            .withPreference(adml, "homePage", "{\"betaEnabled\" : true}")
            .withPreference(adml, "theme", "cg77")
            .withPreference(adml, "apps", "{\"bookmarks\" : [\"form\"], \"applications\":[\"form\", \"news\"]}")
            .withStructureLink("my-structure-02", "my-structure-01")
            .withUser(corruptedPreferenceUser)
            .withPreference(corruptedPreferenceUser, "language", "{\\\"default-domain\\\" : \"fr\"}")
            .withPreference(corruptedPreferenceUser, "homePage", "{\"betaEnabled\" : true}")
            .withPreference(corruptedPreferenceUser, "theme", "cg77")
            .withPreference(corruptedPreferenceUser, "apps", "{\"bookmarks\" : [\"form\"], \"applications\":[\"form\", \"news\"]}")
        ;
        return dataHelper.execute();
    }

}
