package org.entcore.feeder.utils;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.entcore.feeder.utils.OrgaPedagoGroups.STUDENT_GROUP_SEGMENTS;
import static org.entcore.feeder.utils.OrgaPedagoGroups.TEACHER_GROUP_SEGMENTS;

@RunWith(VertxUnitRunner.class)
public class OrgaPedagoGroupsTest {

    @Test
    public void testConvertStudentValue(final TestContext context) {
        // modality and dates dropped
        context.assertEquals("3382$3E2_ESP LV2",
                JsonUtil.convert("3382$3E2_ESP LV2$O$01/01/2026$03/01/2027", "array-orga-pedago-group", null));

        // academy prefix on structure
        context.assertEquals("ACA-3382$3E2_GROUPEA",
                JsonUtil.convert("3382$3E2_GROUPEA$O$01/01/2026$03/01/2027", "array-orga-pedago-group", "ACA-"));

        // unexpected format: kept as is, ignored when merging
        context.assertEquals("3382$3E2_GROUPEA$O",
                JsonUtil.convert("3382$3E2_GROUPEA$O", "array-orga-pedago-group", null));
    }

    @Test
    public void testConvertTeacherValue(final TestContext context) {
        context.assertEquals("3459$31P1$020700",
                JsonUtil.convert("3459$31P1$NATIONALE$FRANC$020700$10/10/2026$31/08/2027",
                        "array-orga-pedago-group-fieldOfStudy", null));

        // academy prefix on structure and field of study
        context.assertEquals("ACA-3459$31P1$ACA-020700",
                JsonUtil.convert("3459$31P1$NATIONALE$FRANC$020700$10/10/2026$31/08/2027",
                        "array-orga-pedago-group-fieldOfStudy", "ACA-"));
        context.assertEquals(
                JsonUtil.convert("3459$31P1$020700", "array-classe-group-fieldOfStudy", "ACA-"),
                JsonUtil.convert("3459$31P1$NATIONALE$FRANC$020700$10/10/2026$31/08/2027",
                        "array-orga-pedago-group-fieldOfStudy", "ACA-"));

        // empty segments preserved
        context.assertEquals("3459$31P1$020700",
                JsonUtil.convert("3459$31P1$$$020700$10/10/2026$31/08/2027",
                        "array-orga-pedago-group-fieldOfStudy", null));
    }

    @Test
    public void testOrgaPedagoReplacesLegacyInSameStructure(final TestContext context) {
        // renamed group
        context.assertEquals(new JsonArray().add("3119$3AGL2 31 34"),
                OrgaPedagoGroups.prioritizeByStructure(
                        new JsonArray().add("3119$3AGL22"),
                        new JsonArray().add("3119$3AGL2 31 34"),
                        STUDENT_GROUP_SEGMENTS));

        // group change
        context.assertEquals(new JsonArray().add("3382$3E4_GROUPEA"),
                OrgaPedagoGroups.prioritizeByStructure(
                        new JsonArray().add("3382$3E4_GROUPE B"),
                        new JsonArray().add("3382$3E4_GROUPEA"),
                        STUDENT_GROUP_SEGMENTS));

        // field of study change
        context.assertEquals(new JsonArray().add("3091$3AGL41$030204"),
                OrgaPedagoGroups.prioritizeByStructure(
                        new JsonArray().add("3091$3AGL41$030201"),
                        new JsonArray().add("3091$3AGL41$030204"),
                        TEACHER_GROUP_SEGMENTS));
    }

    @Test
    public void testLegacyKeptForUncoveredStructures(final TestContext context) {
        // multi-structure teacher: 3767 has no OrgaPedago groups
        context.assertEquals(
                new JsonArray().add("3767$5AP FRANCAIS 2$020700").add("3119$_62_ALL2$030102"),
                OrgaPedagoGroups.prioritizeByStructure(
                        new JsonArray().add("3767$5AP FRANCAIS 2$020700").add("3119$62_ALL2$030102"),
                        new JsonArray().add("3119$_62_ALL2$030102"),
                        TEACHER_GROUP_SEGMENTS));

        // no OrgaPedago groups
        final JsonArray legacy = new JsonArray().add("3382$3E2_ESP LV2").add("3382$3E2_GROUPEA");
        context.assertEquals(legacy, OrgaPedagoGroups.prioritizeByStructure(legacy, null, STUDENT_GROUP_SEGMENTS));
        context.assertEquals(legacy, OrgaPedagoGroups.prioritizeByStructure(legacy, new JsonArray(), STUDENT_GROUP_SEGMENTS));
    }

    @Test
    public void testOrgaPedagoOnly(final TestContext context) {
        context.assertEquals(new JsonArray().add("3541$3EME_GPE 2 AVEC GREC"),
                OrgaPedagoGroups.prioritizeByStructure(
                        null,
                        new JsonArray().add("3541$3EME_GPE 2 AVEC GREC"),
                        STUDENT_GROUP_SEGMENTS));
    }

    @Test
    public void testMalformedOrgaPedagoValuesIgnored(final TestContext context) {
        // malformed values don't override legacy groups
        context.assertEquals(new JsonArray().add("3382$3E2_GROUPEA"),
                OrgaPedagoGroups.prioritizeByStructure(
                        new JsonArray().add("3382$3E2_GROUPEA"),
                        new JsonArray().add("3382$3E2_GROUPEA$O").add("3382$"),
                        STUDENT_GROUP_SEGMENTS));
    }

    @Test
    public void testDuplicatesRemoved(final TestContext context) {
        context.assertEquals(new JsonArray().add("3382$3E2_GROUPEA"),
                OrgaPedagoGroups.prioritizeByStructure(
                        new JsonArray().add("3382$3E2_GROUPEA"),
                        new JsonArray().add("3382$3E2_GROUPEA").add("3382$3E2_GROUPEA"),
                        STUDENT_GROUP_SEGMENTS));
    }

    @Test
    public void testApplyTo(final TestContext context) {
        final JsonObject student = new JsonObject()
                .put("groups", new JsonArray().add("3382$3E2_ESP LV2").add("3382$MFRCS L2"))
                .put("groupsMembershipPeriod", new JsonArray().add("3382$3E2_ESP LV2").add("3382$3E3_GROUPEA_11092026"));
        OrgaPedagoGroups.applyTo(student, STUDENT_GROUP_SEGMENTS);
        context.assertEquals(new JsonArray().add("3382$3E2_ESP LV2").add("3382$3E3_GROUPEA_11092026"),
                student.getJsonArray("groups"));

        // untouched without OrgaPedago groups (same checksum)
        final JsonArray legacy = new JsonArray().add("3382$3E2_ESP LV2");
        final JsonObject untouched = new JsonObject().put("groups", legacy);
        OrgaPedagoGroups.applyTo(untouched, STUDENT_GROUP_SEGMENTS);
        context.assertTrue(legacy == untouched.getJsonArray("groups"));
        context.assertFalse(untouched.containsKey("groupsMembershipPeriod"));
    }

}
