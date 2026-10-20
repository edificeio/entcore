package org.entcore.timeline.util;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.timeline.util.EleaNotificationAdapter.EleaError;
import org.junit.Test;

import static org.junit.Assert.*;

public class EleaNotificationAdapterTest {

    private static JsonObject elea() {
        return new JsonObject()
                .put("school", "0598000t")
                .put("recipient", "1234567")
                .put("sender", "7654321")
                .put("subject", "M. Martin a donné un feedback pour le devoir DM n°2")
                .put("message", "<p><a href=\"https://elea.example/mod/assign/view.php?id=88\">DM n°2</a></p>")
                .put("context", 81);
    }

    // --- parse ---

    @Test
    public void testParse_ValidObject_ReturnsPayload() {
        final JsonObject payload = EleaNotificationAdapter.parse(elea().encode());
        assertNotNull(payload);
        assertEquals("1234567", payload.getString("recipient"));
    }

    @Test
    public void testParse_MalformedJson_ReturnsNull() {
        assertNull(EleaNotificationAdapter.parse("{\"recipient\": "));
    }

    @Test
    public void testParse_JsonArray_ReturnsNull() {
        assertNull(EleaNotificationAdapter.parse("[{\"recipient\": \"1234567\"}]"));
    }

    @Test
    public void testParse_EmptyOrNull_ReturnsNull() {
        assertNull(EleaNotificationAdapter.parse(""));
        assertNull(EleaNotificationAdapter.parse("   "));
        assertNull(EleaNotificationAdapter.parse(null));
    }

    // --- validate ---

    @Test
    public void testValidate_FullPayload_Valid() {
        assertNull(EleaNotificationAdapter.validate(elea()));
    }

    @Test
    public void testValidate_NullPayload_InvalidPayload() {
        assertEquals(EleaError.INVALID_PAYLOAD, EleaNotificationAdapter.validate(null));
    }

    @Test
    public void testValidate_MissingRecipient_InvalidRecipient() {
        final JsonObject payload = elea();
        payload.remove("recipient");
        assertEquals(EleaError.INVALID_RECIPIENT, EleaNotificationAdapter.validate(payload));
    }

    @Test
    public void testValidate_BlankRecipient_InvalidRecipient() {
        assertEquals(EleaError.INVALID_RECIPIENT, EleaNotificationAdapter.validate(elea().put("recipient", "  ")));
    }

    @Test
    public void testValidate_NonStringRecipient_InvalidRecipient() {
        assertEquals(EleaError.INVALID_RECIPIENT, EleaNotificationAdapter.validate(elea().put("recipient", 1234567)));
        assertEquals(EleaError.INVALID_RECIPIENT,
                EleaNotificationAdapter.validate(elea().put("recipient", new JsonArray().add("1234567"))));
    }

    @Test
    public void testValidate_NoSubjectNorMessage_EmptyNotification() {
        final JsonObject payload = elea().put("subject", " ").putNull("message");
        assertEquals(EleaError.EMPTY_NOTIFICATION, EleaNotificationAdapter.validate(payload));
    }

    @Test
    public void testValidate_SubjectOnly_Valid() {
        final JsonObject payload = elea();
        payload.remove("message");
        assertNull(EleaNotificationAdapter.validate(payload));
    }

    @Test
    public void testValidate_MessageOnly_Valid() {
        final JsonObject payload = elea();
        payload.remove("subject");
        assertNull(EleaNotificationAdapter.validate(payload));
    }

    @Test
    public void testValidate_OptionalFieldsMissingOrNull_Valid() {
        final JsonObject payload = elea().putNull("context").putNull("sender");
        payload.remove("school");
        assertNull(EleaNotificationAdapter.validate(payload));
    }

    @Test
    public void testValidate_UnknownFieldsAndUnexpectedTypes_Valid() {
        final JsonObject payload = elea()
                .put("context", "81")
                .put("school", new JsonObject().put("uai", "0598000t"))
                .put("futureField", new JsonArray().add(1));
        assertNull(EleaNotificationAdapter.validate(payload));
    }

    // --- getRecipient ---

    @Test
    public void testGetRecipient_Trimmed() {
        assertEquals("1234567", EleaNotificationAdapter.getRecipient(elea().put("recipient", " 1234567 ")));
    }

    // --- getSender ---

    @Test
    public void testGetSender_Trimmed() {
        assertEquals("7654321", EleaNotificationAdapter.getSender(elea().put("sender", " 7654321 ")));
    }

    @Test
    public void testGetSender_MissingBlankOrNonString_Null() {
        final JsonObject payload = elea();
        payload.remove("sender");
        assertNull(EleaNotificationAdapter.getSender(payload));
        assertNull(EleaNotificationAdapter.getSender(elea().put("sender", "  ")));
        assertNull(EleaNotificationAdapter.getSender(elea().putNull("sender")));
        assertNull(EleaNotificationAdapter.getSender(elea().put("sender", 7654321)));
    }

    // --- toExternalNotification ---

    private static JsonObject sender() {
        return new JsonObject().put("externalId", "7654321").put("id", "sender-ent-id").put("displayName", "Jean Martin");
    }

    @Test
    public void testToExternalNotification_MapsToGenericPayload() {
        final JsonObject payload = elea();
        final JsonObject external = EleaNotificationAdapter.toExternalNotification(payload, "ent-id", sender());

        assertEquals(new JsonArray().add("ent-id"), external.getJsonArray("recipients"));
        assertEquals(payload.getString("subject"), external.getString("subject"));
        assertEquals(payload.getString("message"), external.getString("body"));
        assertTrue(external.getBoolean("pushMobile"));
        assertEquals(EleaNotificationAdapter.ELEA_DISPLAY_NAME, external.getString("mobileTitle"));
        assertEquals(payload.getString("subject"), external.getString("mobileBody"));
    }

    @Test
    public void testToExternalNotification_ResolvedSender_SenderIdAndUsername() {
        final JsonObject external = EleaNotificationAdapter.toExternalNotification(elea(), "ent-id", sender());
        assertEquals("sender-ent-id", external.getString("senderId"));
        assertEquals("Jean Martin", external.getString("username"));
    }

    @Test
    public void testToExternalNotification_UnknownSender_NoSenderTransmitted() {
        final JsonObject external = EleaNotificationAdapter.toExternalNotification(elea(), "ent-id", null);
        assertFalse(external.containsKey("senderId"));
        assertFalse(external.containsKey("username"));
    }

    @Test
    public void testToExternalNotification_SenderWithoutDisplayName_NoSenderTransmitted() {
        final JsonObject external = EleaNotificationAdapter.toExternalNotification(elea(), "ent-id",
                sender().putNull("displayName"));
        assertFalse(external.containsKey("senderId"));
        assertFalse(external.containsKey("username"));
    }

    @Test
    public void testToExternalNotification_NoSubject_PushBodyIsPlainMessage() {
        final JsonObject payload = elea();
        payload.remove("subject");
        final JsonObject external = EleaNotificationAdapter.toExternalNotification(payload, "ent-id", null);
        assertEquals("", external.getString("subject"));
        assertEquals("DM n°2", external.getString("mobileBody"));
    }

    @Test
    public void testToExternalNotification_NoMessage_EmptyBody() {
        final JsonObject payload = elea();
        payload.remove("message");
        final JsonObject external = EleaNotificationAdapter.toExternalNotification(payload, "ent-id", null);
        assertEquals("", external.getString("body"));
        assertEquals(payload.getString("subject"), external.getString("mobileBody"));
    }

    // --- response envelopes ---

    @Test
    public void testSuccess_Envelope() {
        final JsonObject success = EleaNotificationAdapter.success();
        assertTrue(success.getBoolean("success"));
        assertEquals(EleaNotificationAdapter.ELEA_SUCCESS_MESSAGE, success.getString("message"));
    }

    @Test
    public void testError_Envelope() {
        final JsonObject error = EleaError.INVALID_RECIPIENT.toJson();
        assertFalse(error.getBoolean("success"));
        assertFalse(error.getBoolean("retry"));
        assertEquals("INVALID_RECIPIENT", error.getJsonObject("error").getString("code"));
        assertFalse(error.getJsonObject("error").getString("message").isEmpty());
    }

    @Test
    public void testError_StatusAndRetry() {
        assertEquals(400, EleaError.INVALID_PAYLOAD.getStatus());
        assertEquals(400, EleaError.INVALID_RECIPIENT.getStatus());
        assertEquals(400, EleaError.EMPTY_NOTIFICATION.getStatus());
        assertEquals(503, EleaError.SERVICE_UNAVAILABLE.getStatus());
        assertEquals(500, EleaError.INTERNAL_ERROR.getStatus());
        assertFalse(EleaError.INVALID_PAYLOAD.isRetry());
        assertFalse(EleaError.INVALID_RECIPIENT.isRetry());
        assertFalse(EleaError.EMPTY_NOTIFICATION.isRetry());
        assertTrue(EleaError.SERVICE_UNAVAILABLE.isRetry());
        assertTrue(EleaError.INTERNAL_ERROR.isRetry());
    }
}
