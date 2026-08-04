/*
 * Copyright © "Open Digital Education", 2026
 *
 * This program is published by "Open Digital Education".
 * You must indicate the name of the software and the company in any production /contribution
 * using the software and indicate on the home page of the software industry in question,
 * "powered by Open Digital Education" with a reference to the website: https://opendigitaleducation.com/.
 *
 * This program is free software, licensed under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation, version 3 of the License.
 *
 * You can redistribute this application and/or modify it since you respect the terms of the GNU Affero General Public License.
 * If you modify the source code and then use this modified source code in your creation, you must make available the source code of your modifications.
 *
 * You should have received a copy of the GNU Affero General Public License along with the software.
 * If not, please see : <http://www.gnu.org/licenses/>. Full compliance requires reading the terms of this license and following its directives.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package org.entcore.conversation;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;

import fr.wseduc.transformer.IContentTransformerClient;
import fr.wseduc.transformer.to.ContentTransformerResponse;
import org.entcore.common.editor.IContentTransformerEventRecorder;
import org.entcore.common.user.UserInfos;
import org.entcore.common.utils.Config;
import org.entcore.conversation.service.ConversationService;
import org.entcore.conversation.service.impl.SqlConversationService;
import org.entcore.test.TestHelper;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.PostgreSQLContainer;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

/**
 * Covers what stops the absence mechanism, which writing {@code enabled = false} does not guarantee on its
 * own : the reply is only emitted when the detection stage returns the absent user, and that stage is what
 * these tests pin down.
 */
@RunWith(VertxUnitRunner.class)
public class AbsenceDeactivationTest {

    private static final TestHelper test = TestHelper.helper();
    static final String schema = "conversation";

    @ClassRule
    public static PostgreSQLContainer<?> pgContainer = test.database().createPostgreSQLContainer();

    static ConversationService conversationService;

    @BeforeClass
    public static void setUp(TestContext context) throws Exception {
        Config.getInstance().setConfig(new JsonObject());
        conversationService = new SqlConversationService(test.vertx(), schema, stubTransformer(),
                IContentTransformerEventRecorder.noop);
        test.database().initPostgreSQL(context, pgContainer, schema);
    }

    /**
     * Returns what the real transformer returns for a JSON input : the sanitized JSON in cleanJson and the
     * converted HTML in htmlContent. The noop client would leave both null, which the upsert rejects.
     */
    private static IContentTransformerClient stubTransformer() {
        return request -> Future.succeededFuture(new ContentTransformerResponse(
                1, "<p>Absent.</p>", null, null, null,
                new JsonObject().put("type", "doc").getMap()));
    }

    private UserInfos userWithId(String id) {
        final UserInfos user = new UserInfos();
        user.setUserId(id);
        return user;
    }

    private JsonObject absence(boolean enabled, Instant startAt, Instant endAt) {
        return new JsonObject()
                .put("enabled", enabled)
                .put("startAt", startAt.toString())
                .put("endAt", endAt.toString())
                .put("bodyJson", new JsonObject().put("type", "doc"));
    }

    private boolean containsUser(JsonArray absences, String userId) {
        for (Object absence : absences) {
            if (userId.equals(((JsonObject) absence).getString("userId"))) {
                return true;
            }
        }
        return false;
    }

    private Instant daysFromNow(int days) {
        return Instant.now().plus(days, ChronoUnit.DAYS);
    }

    /**
     * Guards against the other tests passing for the wrong reason : if detection never returned anyone, every
     * negative assertion below would hold whatever the code does.
     */
    @Test
    public void activeAbsenceShouldBeDetected(TestContext context) {
        final Async async = context.async();
        final String userId = "absence-active";
        conversationService.upsertAbsence(absence(true, daysFromNow(-1), daysFromNow(1)), userWithId(userId))
                .compose(saved -> conversationService.findActiveAbsences(Collections.singletonList(userId)))
                .onSuccess(absences -> {
                    context.assertTrue(containsUser(absences, userId),
                            "An enabled absence covering the current instant must be detected");
                    async.complete();
                })
                .onFailure(context::fail);
    }

    @Test
    public void disabledAbsenceShouldNotBeDetected(TestContext context) {
        final Async async = context.async();
        final String userId = "absence-disabled";
        final UserInfos user = userWithId(userId);
        conversationService.upsertAbsence(absence(true, daysFromNow(-1), daysFromNow(1)), user)
                .compose(saved -> conversationService.findActiveAbsences(Collections.singletonList(userId)))
                .compose(active -> {
                    context.assertTrue(containsUser(active, userId), "Absence must be detected while enabled");
                    // Same period, only the toggle changes : US-3 keeps the dates and the text.
                    return conversationService.upsertAbsence(absence(false, daysFromNow(-1), daysFromNow(1)), user);
                })
                .compose(saved -> conversationService.findActiveAbsences(Collections.singletonList(userId)))
                .onSuccess(absences -> {
                    context.assertFalse(containsUser(absences, userId),
                            "A disabled absence must not be detected any more");
                    async.complete();
                })
                .onFailure(context::fail);
    }

    /**
     * The trap case of US-3 : an expeditor already replied to today has a row in the anti-spam guard, so the
     * absence could stop for the wrong reason. This asserts the absence is still detected while enabled
     * despite that row — the guard is a separate stage — and stops being detected once disabled.
     */
    @Test
    public void disabledAbsenceShouldNotBeDetectedEvenWhenExpeditorWasAlreadyRepliedToday(TestContext context) {
        final Async async = context.async();
        final String userId = "absence-already-replied";
        final String senderId = "expeditor-of-the-day";
        final UserInfos user = userWithId(userId);
        conversationService.upsertAbsence(absence(true, daysFromNow(-1), daysFromNow(1)), user)
                .compose(saved -> conversationService.claimAbsenceReplySlots(
                        Collections.singletonList(userId), senderId, "Europe/Paris"))
                .compose(claimed -> {
                    context.assertEquals(1, claimed.size(), "The daily slot must be claimed once");
                    return conversationService.findActiveAbsences(Collections.singletonList(userId));
                })
                .compose(active -> {
                    context.assertTrue(containsUser(active, userId),
                            "A spent daily slot must not stop detection : the guard is a later stage");
                    return conversationService.upsertAbsence(absence(false, daysFromNow(-1), daysFromNow(1)), user);
                })
                .compose(saved -> conversationService.findActiveAbsences(Collections.singletonList(userId)))
                .onSuccess(absences -> {
                    context.assertFalse(containsUser(absences, userId),
                            "Disabling must stop replies even for an expeditor already replied to today");
                    async.complete();
                })
                .onFailure(context::fail);
    }

    /**
     * US-1 scenario whose effect belongs to US-3 : moving the end date into the past is a deactivation, with
     * no toggle involved.
     */
    @Test
    public void endDateMovedToThePastShouldBehaveAsDeactivation(TestContext context) {
        final Async async = context.async();
        final String userId = "absence-ended";
        final UserInfos user = userWithId(userId);
        conversationService.upsertAbsence(absence(true, daysFromNow(-2), daysFromNow(2)), user)
                .compose(saved -> conversationService.findActiveAbsences(Collections.singletonList(userId)))
                .compose(active -> {
                    context.assertTrue(containsUser(active, userId), "Absence must be detected while running");
                    // Still enabled : only the end date moves before the current instant.
                    return conversationService.upsertAbsence(absence(true, daysFromNow(-2), daysFromNow(-1)), user);
                })
                .compose(saved -> conversationService.findActiveAbsences(Collections.singletonList(userId)))
                .onSuccess(absences -> {
                    context.assertFalse(containsUser(absences, userId),
                            "An absence whose end date has passed must not be detected, toggle notwithstanding");
                    async.complete();
                })
                .onFailure(context::fail);
    }
}
