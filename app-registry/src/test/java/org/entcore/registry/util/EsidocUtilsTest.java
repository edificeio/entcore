package org.entcore.registry.util;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class EsidocUtilsTest {

    /** Response given by the e-sidoc documentation: an empty bucket is an array, a filled one an object keyed by loan id. */
    private static final String DOCUMENTED_RESPONSE = "{" +
            "\"compte\": {\"permalien\": \"https:\\/\\/9990068v.devesidoc.fr\\/moncompte\\/mesprets\"}," +
            "\"prets\": {" +
            "  \"compteur\": {\"en_cours\": 0, \"en_retard\": 3, \"historique\": 0}," +
            "  \"items\": {" +
            "    \"en_cours\": []," +
            "    \"en_retard\": {" +
            "      \"l_2275c9f95b9fc\": {\"exemplaire\": \"1961\", \"date_emprunt\": \"2025-09-29\", \"date_retour\": \"2025-10-14\"," +
            "        \"permalien\": \"https:\\/\\/9990068v.devesidoc.fr\\/document\\/id_9990068v_3170.html\"," +
            "        \"titre\": \"10 séquences pour lire : \\\"L'oeil du loup\\\" de Daniel Pennac\", \"cote\": \"843.0073 COU\"," +
            "        \"support\": \"Livre\", \"notice_id\": \"9990068v_3170\"}," +
            "      \"l_4a1b560223c1f\": {\"exemplaire\": \"1165\", \"date_emprunt\": \"2025-09-25\", \"date_retour\": \"2025-10-25\"," +
            "        \"permalien\": \"https:\\/\\/9990068v.devesidoc.fr\\/document\\/id_9990068v_822.html\"," +
            "        \"titre\": \"Le Jazz et la Java\", \"cote\": \"M NOU\", \"support\": \"Livre\", \"notice_id\": \"9990068v_822\"}," +
            "      \"l_4a54fc3d76fe2\": {\"exemplaire\": \"1105\", \"date_emprunt\": \"2025-03-15\", \"date_retour\": \"2025-03-30\"," +
            "        \"permalien\": \"https:\\/\\/9990068v.devesidoc.fr\\/document\\/id_9990068v_882.html\"," +
            "        \"titre\": \"Vivre au Moyen Age\", \"cote\": \"940.1 LAN\", \"support\": \"Livre\", \"notice_id\": \"9990068v_882\"}" +
            "    }," +
            "    \"historique\": []" +
            "  }" +
            "}" +
            "}";

    @Test
    public void currentLoansReadsTheDocumentedResponse() {
        final JsonArray loans = EsidocUtils.currentLoans(EsidocUtils.parseLoansResponse(Buffer.buffer(DOCUMENTED_RESPONSE)), "9990068V");

        assertEquals(3, loans.size());
        assertEquals(new JsonObject()
                .put("id", "9990068V-l_2275c9f95b9fc")
                .put("title", "10 séquences pour lire : \"L'oeil du loup\" de Daniel Pennac")
                .put("dueDate", "2025-10-14")
                .put("overdue", true), loans.getJsonObject(0));
    }

    @Test
    public void currentLoansKeepsTheBorrowedBooksOnly() {
        final JsonObject response = new JsonObject().put("prets", new JsonObject().put("items", new JsonObject()
                .put("en_cours", new JsonObject().put("l_1", loan("Current", "2099-01-31")))
                .put("en_retard", new JsonObject().put("l_2", loan("Overdue", "2020-01-31")))
                .put("historique", new JsonObject().put("l_3", loan("Returned", "2019-01-31")))));

        final JsonArray loans = EsidocUtils.currentLoans(response, "UAI");

        assertEquals(2, loans.size());
        assertEquals("Overdue", loans.getJsonObject(0).getString("title"));
        assertTrue(loans.getJsonObject(0).getBoolean("overdue"));
        assertEquals("Current", loans.getJsonObject(1).getString("title"));
        assertFalse(loans.getJsonObject(1).getBoolean("overdue"));
    }

    @Test
    public void currentLoansReadsABucketSerializedAsAList() {
        final JsonObject response = new JsonObject().put("prets", new JsonObject().put("items", new JsonObject()
                .put("en_cours", new JsonArray().add(loan("Current", "2099-01-31")))));

        final JsonArray loans = EsidocUtils.currentLoans(response, "UAI");

        assertEquals(1, loans.size());
        assertEquals("UAI-0", loans.getJsonObject(0).getString("id"));
    }

    @Test
    public void currentLoansOfAnUnknownBorrowerIsEmpty() {
        assertTrue(EsidocUtils.currentLoans(new JsonObject(), "UAI").isEmpty());
        assertTrue(EsidocUtils.currentLoans(EsidocUtils.parseLoansResponse(Buffer.buffer("[]")), "UAI").isEmpty());
    }

    @Test(expected = DecodeException.class)
    public void parseLoansResponseRejectsAnUnexpectedBody() {
        EsidocUtils.parseLoansResponse(Buffer.buffer("[\"error\"]"));
    }

    @Test(expected = DecodeException.class)
    public void parseLoansResponseRejectsAnInvalidBody() {
        EsidocUtils.parseLoansResponse(Buffer.buffer("<html>Service unavailable</html>"));
    }

    @Test
    public void mergeByDueDatePutsTheMostUrgentLoansFirst() {
        final JsonArray firstStructure = new JsonArray()
                .add(new JsonObject().put("id", "A-1").put("dueDate", "2025-10-25"))
                .add(new JsonObject().put("id", "A-2"));
        final JsonArray secondStructure = new JsonArray()
                .add(new JsonObject().put("id", "B-1").put("dueDate", "2025-03-30"));

        final JsonArray loans = EsidocUtils.mergeByDueDate(Arrays.asList(firstStructure, secondStructure));

        assertEquals("B-1", loans.getJsonObject(0).getString("id"));
        assertEquals("A-1", loans.getJsonObject(1).getString("id"));
        assertEquals("A-2", loans.getJsonObject(2).getString("id"));
        assertNull(loans.getJsonObject(2).getString("dueDate"));
    }

    @Test
    public void loansPathEncodesItsSegments() {
        assertEquals("/utilisateurs_externe/prets/edifice/0595856V/a%20b%2Fc", EsidocUtils.loansPath("0595856V", "a b/c"));
    }

    private static JsonObject loan(String title, String dueDate) {
        return new JsonObject().put("titre", title).put("date_retour", dueDate);
    }
}
