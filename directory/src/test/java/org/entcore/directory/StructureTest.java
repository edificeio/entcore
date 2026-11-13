package org.entcore.directory;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.entcore.directory.services.SchoolService;
import org.entcore.directory.services.impl.DefaultSchoolService;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.entcore.test.TestHelper;
import org.testcontainers.containers.Neo4jContainer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@RunWith(VertxUnitRunner.class)
public class StructureTest {
    private static final TestHelper test = TestHelper.helper();
    private static SchoolService schoolService;
    private static JsonObject duplicatesJson;
    private static String sourceStructureId;
    private static List<String> targetStructureIds = new ArrayList<>();
    private static JsonArray targetUAIs = new JsonArray();
    private static List<String> appRoleIds = new ArrayList<>();
    private static List<String> widgetRoleIds = new ArrayList<>();
    private static String targetProfileGroupId = "";
    private static String targetFunctionGroupId = "";

    @ClassRule
    public static Neo4jContainer<?> neo4jContainer = test.database().createNeo4jContainer();

    @BeforeClass
    public static void setUp(TestContext context) throws Exception {
        test.database().initNeo4j(context, neo4jContainer);
        schoolService = new DefaultSchoolService(test.vertx().eventBus());
        duplicatesJson = test.file().jsonFromResource("structures/duplicates.json");
    }

    private Future<Void> initDuplicateStructureSettingsData(TestContext context) {
        final JsonObject sourceStructure = duplicatesJson.getJsonObject("sourceStructure");

        Promise<Void> promise = Promise.promise();

        test.directory()
            // Create Structure to duplicate settings from
            .createStructure(sourceStructure.getString("name"), sourceStructure.getString("UAI"))
            // Create appRoles to duplicate
            .compose(resStructureId -> {
                sourceStructureId = resStructureId;

                List<Future> futures = new ArrayList<>();
                Iterator appRolesIt = sourceStructure.getJsonArray("appRoles").iterator();
                while (appRolesIt.hasNext()) {
                    JsonObject role = (JsonObject) appRolesIt.next();
                    futures.add(test.directory().createRole(role.getString("name"), role.getString("type")));
                }
                return CompositeFuture.all(futures);
            })
            // Create widgetRoles to duplicate
            .compose(resAppRoleIds -> {
                appRoleIds = resAppRoleIds.list();

                List<Future> futures = new ArrayList<>();
                Iterator widgetRolesIt = sourceStructure.getJsonArray("widgetRoles").iterator();
                while (widgetRolesIt.hasNext()) {
                    JsonObject role = (JsonObject) widgetRolesIt.next();
                    futures.add(test.directory().createRole(role.getString("name"), role.getString("type")));
                }
                return CompositeFuture.all(futures);
            })
            // Create Profile Group to attach to previously created Roles
            .compose(resWidgetRoleIds -> {
                widgetRoleIds = resWidgetRoleIds.list();
                return test.directory().createProfileGroup("SourceProfileGroup-Student");
            })
            // Attach roles to Profile Group and attach Profile Groups to Structure
            .compose(resProfileGroupId -> {
                List<Future> futures = new ArrayList<>();
                for (String roleId: appRoleIds) {
                    futures.add(test.directory().attachRoleToGroup(roleId, resProfileGroupId));
                }
                for (String roleId: widgetRoleIds) {
                    futures.add(test.directory().attachRoleToGroup(roleId, resProfileGroupId));
                }
                futures.add(test.directory().attachGroupToStruct(resProfileGroupId, sourceStructureId));
                return CompositeFuture.all(futures);
            })
            // Create Function Group to attach to previously created Roles
            .compose(event -> test.directory().createFunctionGroup("SourceFunctionGroup-AdminLocal"))
            // Attach roles to Function Group and attach Profile Group to source Structure
            .compose(resFunctionGroupId -> {
                List<Future> futures = new ArrayList<>();
                for (String roleId: appRoleIds) {
                    futures.add(test.directory().attachRoleToGroup(roleId, resFunctionGroupId));
                }
                for (String roleId: widgetRoleIds) {
                    futures.add(test.directory().attachRoleToGroup(roleId, resFunctionGroupId));
                }
                futures.add(test.directory().attachGroupToStruct(resFunctionGroupId, sourceStructureId));
                return CompositeFuture.all(futures);
            })
            // Create target Structures where to duplicate settings
            .compose(event -> {
                List<Future> futures = new ArrayList<>();
                Iterator targetStructuresIterator = duplicatesJson.getJsonArray("targetStructures").iterator();
                while (targetStructuresIterator.hasNext()) {
                    JsonObject targetStructure = (JsonObject) targetStructuresIterator.next();
                    futures.add(test.directory().createStructure(targetStructure.getString("name"), targetStructure.getString("UAI")));
                    targetUAIs.add(targetStructure.getString("UAI"));
                }
                return CompositeFuture.all(futures);
            })
            // Create target Profile Group
            .compose(resStructureIds -> {
                targetStructureIds = resStructureIds.list();
                return test.directory().createProfileGroup("TargetProfileGroup-Student");
            })
            // Attach target Profile Group to target Structures
            .compose(resTargetProfileGroupId -> {
                targetProfileGroupId = resTargetProfileGroupId;
                List<Future> futures = new ArrayList<>();
                for (String targetStructureId: targetStructureIds) {
                    futures.add(test.directory().attachGroupToStruct(resTargetProfileGroupId, targetStructureId));
                }
                return CompositeFuture.all(futures);
            })
            // Create target Function Groups
            .compose(event -> test.directory().createFunctionGroup("TargetFunctionGroup-AdminLocal"))
            .compose(resTargetFunctionGroupId -> {
                targetFunctionGroupId = resTargetFunctionGroupId;
                List<Future> futures = new ArrayList<>();
                for (String targetStructureId: targetStructureIds) {
                    futures.add(test.directory().attachGroupToStruct(resTargetFunctionGroupId, targetStructureId));
                }
                return CompositeFuture.all(futures);
            })
            // Set HasApp for Source Structure
            .compose(event -> test.directory().setHasApp(sourceStructureId, sourceStructure.getBoolean("hasApp")))
            // Set Distributions to Source Structure
            .compose(event -> test.directory().setDistributions(sourceStructureId, sourceStructure.getJsonArray("distributions")))
            // Set Education Level to Source Structure
            .compose(event -> test.directory().setLevelsOfEducation(sourceStructureId, sourceStructure.getJsonArray("levelsOfEducation")))
            .onComplete(event -> {
                context.assertTrue(event.succeeded());
                promise.complete();
            });
        return promise.future();
    }

    @Test
    public void testDuplicateStructureSettings(TestContext context) {
        final JsonObject options = duplicatesJson.getJsonObject("options");
        final JsonObject sourceStructure = duplicatesJson.getJsonObject("sourceStructure");

        final Async async = context.async();
        this.initDuplicateStructureSettingsData(context)
            .compose(event -> {
                final Promise<Void> promise = Promise.promise();
                schoolService.duplicateStructureSettings(sourceStructureId, targetUAIs, options,
                    res -> {
                        context.assertTrue(res.isRight());

                        // Check ProfileGroup Has Roles
                        test.directory().groupHasRoles(targetProfileGroupId, "ProfileGroup", appRoleIds)
                            .compose(resProfileGroupHasRoles -> {
                                context.assertTrue(resProfileGroupHasRoles);

                                // Check FunctionGroup Has Roles
                                return test.directory().groupHasRoles(targetFunctionGroupId, "FunctionGroup", appRoleIds);
                            })
                            .compose(resFunctionGroupHasRoles -> {
                                context.assertTrue(resFunctionGroupHasRoles);

                                // Check Distributions
                                List<Future> checkFutures = new ArrayList<>();
                                for (String targetStructureId: targetStructureIds) {
                                    if (options.getBoolean("setDistribution")) {
                                        checkFutures.add(test.directory().structureHasDistributions(targetStructureId, sourceStructure.getJsonArray("distributions")));
                                    }
                                }
                                return CompositeFuture.all(checkFutures);
                            })
                            .compose(resHasDistributions -> {
                                for (Object result: resHasDistributions.result().list()) {
                                    context.assertTrue((Boolean) result);
                                }

                                // Check LevelsOfEducation
                                List<Future> checkFutures = new ArrayList<>();
                                for (String targetStructureId: targetStructureIds) {
                                    if (options.getBoolean("setEducation")) {
                                        checkFutures.add(test.directory().structureHasLevelsOfEducation(targetStructureId, sourceStructure.getJsonArray("levelsOfEducation")));
                                    }
                                }
                                return CompositeFuture.all(checkFutures);
                            }).compose(resHasEducation -> {
                                for (Object result: resHasEducation.result().list()) {
                                    context.assertTrue((Boolean) result);
                                }

                                // Check hasApp
                                List<Future> checkFutures = new ArrayList<>();
                                for (String targetStructureId: targetStructureIds) {
                                    if (options.getBoolean("setHasApp")) {
                                        checkFutures.add(test.directory().structureHasApp(targetStructureId, sourceStructure.getBoolean("hasApp")));
                                    }
                                }
                                return CompositeFuture.all(checkFutures);
                            })
                            .onComplete(checkFuturesResult -> {
                                for (int i = 0; i < checkFuturesResult.result().list().size(); i++) {
                                    context.assertTrue(checkFuturesResult.result().resultAt(i));
                                }
                                promise.complete();
                            });
                    });
                return promise.future();
            })
            .onComplete(event -> {
                context.assertTrue(event.succeeded());
                async.complete();
            });
    }

    /**
     * La structure modèle peut figurer dans la liste des UAI cibles : elle doit garder son paramétrage
     * et le transmettre aux autres cibles (IMPULS-6455).
     */
    @Test
    public void testDuplicateStructureSettingsWithSourceInTargets(TestContext context) {
        final JsonObject options = new JsonObject()
                .put("setApplications", true)
                .put("setWidgets", true)
                .put("setDistribution", false)
                .put("setEducation", false)
                .put("setHasApp", false);
        final JsonArray uais = new JsonArray().add("5555555A").add("6666666B");
        CompositeFuture.all(
                test.directory().createStructure("Self Source Structure", "5555555A"),
                test.directory().createStructure("Self Target Structure", "6666666B"),
                test.directory().createProfileGroup("SelfSourceProfileGroup-Teacher"),
                test.directory().createProfileGroup("SelfTargetProfileGroup-Teacher"),
                test.directory().createRole("SelfRoleApp", "Role"),
                test.directory().createRole("SelfRoleWidget", "Widget"))
            .compose(created -> {
                final String sourceId = created.resultAt(0);
                final String targetId = created.resultAt(1);
                final String sourceGroupId = created.resultAt(2);
                final String targetGroupId = created.resultAt(3);
                final JsonArray roleIds = new JsonArray().add(created.<String>resultAt(4)).add(created.<String>resultAt(5));
                return CompositeFuture.all(
                        test.directory().attachGroupToStruct(sourceGroupId, sourceId),
                        test.directory().attachGroupToStruct(targetGroupId, targetId),
                        test.database().executeNeo4j(
                                "MATCH (g:Group {id:{groupId}}), (r) WHERE r.id IN {roleIds} MERGE (g)-[:AUTHORIZED]->(r)",
                                new JsonObject().put("groupId", sourceGroupId).put("roleIds", roleIds)))
                    .compose(attached -> {
                        final Promise<Void> duplicated = Promise.promise();
                        schoolService.duplicateStructureSettings(sourceId, uais, options, res -> {
                            if (res.isRight()) {
                                duplicated.complete();
                            } else {
                                duplicated.fail(res.left().getValue());
                            }
                        });
                        return duplicated.future();
                    })
                    .compose(duplicated -> CompositeFuture.all(
                            countAuthorized(sourceGroupId, roleIds),
                            countAuthorized(targetGroupId, roleIds)));
            })
            .onComplete(context.asyncAssertSuccess(counts -> {
                context.assertEquals(2, counts.resultAt(0), "La structure modèle doit garder son paramétrage");
                context.assertEquals(2, counts.resultAt(1), "La cible doit recevoir le paramétrage de la structure modèle");
            }));
    }

    private Future<Integer> countAuthorized(String groupId, JsonArray roleIds) {
        return test.database().executeNeo4jWithUniqueResult(
                "MATCH (g:Group {id:{groupId}})-[a:AUTHORIZED]->(r) WHERE r.id IN {roleIds} RETURN COUNT(a) AS nb",
                new JsonObject().put("groupId", groupId).put("roleIds", roleIds))
            .map(res -> res.getInteger("nb"));
    }
}
