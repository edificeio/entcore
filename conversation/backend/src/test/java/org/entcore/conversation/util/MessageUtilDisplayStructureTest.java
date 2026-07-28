package org.entcore.conversation.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.Test;

/**
 * Tests the resolution of the establishment displayed for a message sender, and its application on a
 * formatted message.
 * <p>
 * Each scenario of US-1 has its counterpart here, plus the degradation paths that must never throw:
 * the structure index is fed from another module and may be incomplete or oddly shaped.
 */
public class MessageUtilDisplayStructureTest {

    private static final String READER_ID = "reader";
    private static final String SENDER_ID = "sender";

    private static JsonObject structure(final String id, final String name) {
        return new JsonObject().put("id", id).put("name", name);
    }

    /** An entry of the structure index, as returned by the directory module. */
    private static JsonObject entry(final String userId, final String preferredStructureId, final JsonObject... structures) {
        final JsonObject entry = new JsonObject()
                .put("id", userId)
                .put("structures", new JsonArray(java.util.Arrays.asList(structures)));
        if (preferredStructureId != null) {
            entry.put("preferredStructureId", preferredStructureId);
        }
        return entry;
    }

    private static String resolvedId(final JsonObject sender, final JsonObject reader) {
        final JsonObject resolved = MessageUtil.resolveDisplayStructure(sender, reader);
        return resolved == null ? null : resolved.getString("id");
    }

    // --- US-1 scenarios ------------------------------------------------------------------------------

    @Test
    public void singleStructure_shouldDisplayIt() {
        final JsonObject sender = entry(SENDER_ID, null, structure("s1", "Collège Jean Moulin"));
        final JsonObject reader = entry(READER_ID, null, structure("s9", "Académie de Lyon"));

        assertEquals("s1", resolvedId(sender, reader));
    }

    @Test
    public void oneCommonStructure_shouldPreferItOverThePreferredOne() {
        // Explicitly required by US-1: the common establishment wins even if the preferred one differs.
        final JsonObject sender = entry(SENDER_ID, "s2",
                structure("s1", "Collège Jean Moulin"), structure("s2", "Lycée Ampère"));
        final JsonObject reader = entry(READER_ID, null, structure("s1", "Collège Jean Moulin"));

        assertEquals("s1", resolvedId(sender, reader));
    }

    @Test
    public void severalCommonStructures_shouldBeSettledByThePreferredOne() {
        final JsonObject sender = entry(SENDER_ID, "s2",
                structure("s1", "Collège Jean Moulin"), structure("s2", "Lycée Ampère"));
        final JsonObject reader = entry(READER_ID, null,
                structure("s1", "Collège Jean Moulin"), structure("s2", "Lycée Ampère"));

        assertEquals("s2", resolvedId(sender, reader));
    }

    @Test
    public void severalCommonStructures_withPreferredOutsideThem_shouldFallBackAlphabetically() {
        // The preferred one is not a candidate here: it is not shared with the reader.
        final JsonObject sender = entry(SENDER_ID, "s3",
                structure("s1", "Lycée Ampère"), structure("s2", "Collège Jean Moulin"),
                structure("s3", "Académie de Lyon"));
        final JsonObject reader = entry(READER_ID, null,
                structure("s1", "Lycée Ampère"), structure("s2", "Collège Jean Moulin"));

        assertEquals("s2", resolvedId(sender, reader));
    }

    @Test
    public void noCommonStructure_shouldDisplayThePreferredOne() {
        final JsonObject sender = entry(SENDER_ID, "s2",
                structure("s1", "Collège Jean Moulin"), structure("s2", "Lycée Ampère"));
        final JsonObject reader = entry(READER_ID, null, structure("s9", "Académie de Lyon"));

        assertEquals("s2", resolvedId(sender, reader));
    }

    @Test
    public void noCommonAndNoPreferredStructure_shouldFallBackAlphabetically() {
        final JsonObject sender = entry(SENDER_ID, null,
                structure("s1", "Lycée Ampère"), structure("s2", "Collège Jean Moulin"));
        final JsonObject reader = entry(READER_ID, null, structure("s9", "Académie de Lyon"));

        assertEquals("s2", resolvedId(sender, reader));
    }

    // --- Resolution is relative to the reader --------------------------------------------------------

    @Test
    public void sameSender_readByTwoReaders_mayResolveToDifferentStructures() {
        final JsonObject sender = entry(SENDER_ID, "s1",
                structure("s1", "Collège Jean Moulin"), structure("s2", "Lycée Ampère"));

        assertEquals("s1", resolvedId(sender, entry("reader-a", null, structure("s1", "Collège Jean Moulin"))));
        assertEquals("s2", resolvedId(sender, entry("reader-b", null, structure("s2", "Lycée Ampère"))));
    }

    // --- Alphabetical fallback ordering --------------------------------------------------------------

    @Test
    public void alphabeticalFallback_shouldIgnoreAccentsAndCase() {
        // "École" must come before "Zebre" despite the accent, and "collège" before "Lycée" despite the case.
        final JsonObject sender = entry(SENDER_ID, null,
                structure("s1", "Zebre"), structure("s2", "École du centre"));
        assertEquals("s2", resolvedId(sender, null));

        final JsonObject sender2 = entry(SENDER_ID, null,
                structure("s1", "Lycée Ampère"), structure("s2", "collège Jean Moulin"));
        assertEquals("s2", resolvedId(sender2, null));
    }

    @Test
    public void alphabeticalFallback_shouldPutUnnamedStructuresLast() {
        final JsonObject sender = entry(SENDER_ID, null,
                structure("s1", null), structure("s2", "Zebre"));

        assertEquals("s2", resolvedId(sender, sender));
    }

    // --- Degradation ---------------------------------------------------------------------------------

    @Test
    public void unknownSender_shouldResolveToNothing() {
        assertNull(MessageUtil.resolveDisplayStructure(null, entry(READER_ID, null, structure("s1", "A"))));
    }

    @Test
    public void senderWithoutAnyStructure_shouldResolveToNothing() {
        assertNull(MessageUtil.resolveDisplayStructure(entry(SENDER_ID, null), entry(READER_ID, null, structure("s1", "A"))));
        assertNull(MessageUtil.resolveDisplayStructure(new JsonObject().put("id", SENDER_ID), null));
    }

    @Test
    public void unknownReader_shouldStillResolveTheSenderStructure() {
        // A reader absent from the index means "no common establishment", not "no establishment at all".
        final JsonObject sender = entry(SENDER_ID, "s2",
                structure("s1", "Collège Jean Moulin"), structure("s2", "Lycée Ampère"));

        assertEquals("s2", resolvedId(sender, null));
    }

    @Test
    public void preferredStructureNoLongerAttached_shouldFallBackAlphabetically() {
        // The directory does not guarantee that preferredStructureId is one of the structures.
        final JsonObject sender = entry(SENDER_ID, "gone",
                structure("s1", "Lycée Ampère"), structure("s2", "Collège Jean Moulin"));

        assertEquals("s2", resolvedId(sender, null));
    }

    @Test
    public void malformedStructureEntries_shouldBeIgnoredRatherThanThrow() {
        final JsonObject sender = new JsonObject()
                .put("id", SENDER_ID)
                .put("structures", new JsonArray()
                        .add("not an object")
                        .add(new JsonObject().put("name", "No id at all"))
                        .add(structure("s1", "Collège Jean Moulin")));

        assertEquals("s1", resolvedId(sender, null));
    }

    @Test
    public void structuresOfUnexpectedType_shouldResolveToNothing() {
        assertNull(MessageUtil.resolveDisplayStructure(
                new JsonObject().put("id", SENDER_ID).put("structures", "not an array"), null));
        assertNull(MessageUtil.resolveDisplayStructure(
                new JsonObject().put("id", SENDER_ID).put("structures", 42), null));
    }

    @Test
    public void preferredStructureIdOfUnexpectedType_shouldBeIgnoredNotCoerced() {
        // A number would otherwise be coerced into a String by Vert.x and match nothing, silently
        // shifting the result instead of falling back to the alphabetical order.
        final JsonObject sender = new JsonObject()
                .put("id", SENDER_ID)
                .put("preferredStructureId", 42)
                .put("structures", new JsonArray()
                        .add(structure("s1", "Lycée Ampère"))
                        .add(structure("s2", "Collège Jean Moulin")));

        assertEquals("s2", resolvedId(sender, null));
    }

    @Test
    public void structureIdOrNameOfUnexpectedType_shouldNotThrow() {
        final JsonObject sender = new JsonObject()
                .put("id", SENDER_ID)
                .put("structures", new JsonArray()
                        .add(new JsonObject().put("id", 1).put("name", "Numeric id"))
                        .add(new JsonObject().put("id", "s2").put("name", new JsonObject()))
                        .add(structure("s3", "Collège Jean Moulin")));

        // The only fully usable entry wins; the odd ones are either dropped or sort last.
        assertEquals("s3", resolvedId(sender, null));
    }

    // --- Application on a formatted message ----------------------------------------------------------

    @Test
    public void applySenderDisplayStructure_shouldSetIdAndNameOnFrom() {
        final JsonObject message = new JsonObject().put("from",
                new JsonObject().put("id", SENDER_ID).put("displayName", "Dupont Marie"));
        final JsonObject index = new JsonObject()
                .put(SENDER_ID, entry(SENDER_ID, null, structure("s1", "Collège Jean Moulin")))
                .put(READER_ID, entry(READER_ID, null, structure("s1", "Collège Jean Moulin")));

        MessageUtil.applySenderDisplayStructure(message, index, READER_ID);

        final JsonObject displayed = message.getJsonObject("from").getJsonObject("displayStructure");
        assertEquals("s1", displayed.getString("id"));
        assertEquals("Collège Jean Moulin", displayed.getString("name"));
        assertEquals(2, displayed.size());
    }

    @Test
    public void applySenderDisplayStructure_whenUnresolved_shouldLeaveFromUntouched() {
        final JsonObject message = new JsonObject().put("from",
                new JsonObject().put("id", SENDER_ID).put("displayName", "Dupont Marie"));

        MessageUtil.applySenderDisplayStructure(message, new JsonObject(), READER_ID);

        assertFalse(message.getJsonObject("from").containsKey("displayStructure"));
    }

    @Test
    public void applySenderDisplayStructure_onDeletedSender_shouldLeaveFromUntouched() {
        // formatRecipients blanks the id of a deleted sender: there is no account to resolve.
        final JsonObject message = new JsonObject().put("from",
                new JsonObject().put("id", "").put("displayName", "Ancien Utilisateur"));
        final JsonObject index = new JsonObject().put("", entry("", null, structure("s1", "A")));

        MessageUtil.applySenderDisplayStructure(message, index, READER_ID);

        assertFalse(message.getJsonObject("from").containsKey("displayStructure"));
    }

    @Test
    public void applySenderDisplayStructure_withoutAnyFrom_shouldNotThrow() {
        final JsonObject message = new JsonObject().put("subject", "No sender");

        MessageUtil.applySenderDisplayStructure(message, new JsonObject(), READER_ID);

        assertFalse(message.containsKey("from"));
    }

    @Test
    public void applySenderDisplayStructure_shouldNotDecorateTheSameUserSeenAsRecipient() {
        // The sender of a message may also be a recipient of another one. formatRecipients hands out a copy
        // of the indexed user precisely so that the label cannot leak onto the recipient lists.
        final JsonObject sharedUser = new JsonObject().put("id", SENDER_ID).put("displayName", "Dupont Marie");
        final JsonObject message = new JsonObject().put("from", sharedUser.copy());
        final JsonObject otherMessageRecipients = new JsonObject()
                .put("users", new JsonArray().add(sharedUser));
        final JsonObject index = new JsonObject()
                .put(SENDER_ID, entry(SENDER_ID, null, structure("s1", "Collège Jean Moulin")));

        MessageUtil.applySenderDisplayStructure(message, index, READER_ID);

        assertEquals("s1", message.getJsonObject("from").getJsonObject("displayStructure").getString("id"));
        assertFalse("the label leaked onto a recipient",
                otherMessageRecipients.getJsonArray("users").getJsonObject(0).containsKey("displayStructure"));
    }
}