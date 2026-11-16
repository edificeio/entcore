package org.entcore.conversation.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import fr.wseduc.webutils.http.Renders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import org.entcore.common.user.UserInfos;
import org.junit.Test;

/**
 * Tests what an absence reply is notified with : the timeline notification must read as if the absent user had
 * answered by hand, which a bare user id and a host-only request do not give.
 */
public class AbsenceReplySenderTest {

    private static final String USER_ID = "absent-user";

    @Test
    public void senderShouldCarryWhatTheNotificationReads() {
        final UserInfos sender = AbsenceReplySender.absentSender(USER_ID,
                new JsonObject().put("username", "Marie Curie").put("type", "Teacher"));
        assertEquals(USER_ID, sender.getUserId());
        assertEquals("Marie Curie", sender.getUsername());
        assertEquals("Teacher", sender.getType());
    }

    /** A user missing from the directory lookup must still get a reply, only with a degraded notification. */
    @Test
    public void senderWithoutIdentityShouldKeepItsIdOnly() {
        final UserInfos sender = AbsenceReplySender.absentSender(USER_ID, null);
        assertEquals(USER_ID, sender.getUserId());
        assertNull(sender.getUsername());
        assertNull(sender.getType());
    }

    @Test
    public void requestShouldCarryTheHeadersTheTimelineCopies() {
        final HttpServerRequest request = AbsenceReplySender.replyRequest(
                new JsonObject().put("host", "ent.example.org").put("proto", "https"), "en");
        assertEquals("127.0.0.1", request.headers().get("X-Forwarded-For"));
        assertEquals("ent.example.org", request.headers().get("Host"));
        assertEquals("https", Renders.getScheme(request));
        assertEquals("en", request.headers().get("Accept-Language"));
    }

    /** Payloads dispatched before the scheme was transported keep the former behaviour. */
    @Test
    public void requestWithoutSchemeShouldFallBackAsBefore() {
        final HttpServerRequest request = AbsenceReplySender.replyRequest(
                new JsonObject().put("host", "ent.example.org"), "fr");
        assertNull(request.headers().get("X-Forwarded-Proto"));
        assertEquals("http", Renders.getScheme(request));
        assertEquals("127.0.0.1", request.headers().get("X-Forwarded-For"));
    }
}
