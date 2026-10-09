package org.entcore.registry.util;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Stateless helpers to call the e-sidoc API and read its loans responses.
 */
public final class EsidocUtils {

    /** Path of the e-sidoc route returning the loans of a borrower, followed by the UAI and the borrower id. */
    private static final String LOANS_PATH = "/utilisateurs_externe/prets/edifice/";
    /** e-sidoc bucket of the loans still borrowed and not yet due. */
    private static final String CURRENT_LOANS = "en_cours";
    /** e-sidoc bucket of the loans still borrowed after their return date. */
    private static final String OVERDUE_LOANS = "en_retard";

    private EsidocUtils() {
    }

    /**
     * @return the path of the e-sidoc route returning the loans of a borrower in a structure
     */
    public static String loansPath(String uai, String borrowerId) {
        return LOANS_PATH + encodePathSegment(uai) + "/" + encodePathSegment(borrowerId);
    }

    /**
     * @return the value encoded for an {@code application/x-www-form-urlencoded} body
     */
    public static String encode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // UTF-8 is supported by every JVM
            throw new IllegalStateException(e);
        }
    }

    /**
     * Read the body of an e-sidoc loans response. e-sidoc serializes an empty object as an empty array,
     * which is read as an empty response.
     *
     * @throws DecodeException when the body is neither a JSON object nor an empty JSON array
     */
    public static JsonObject parseLoansResponse(Buffer body) {
        final Object json = Json.decodeValue(body);
        if (json instanceof JsonObject) {
            return (JsonObject) json;
        }
        if (json instanceof JsonArray && ((JsonArray) json).isEmpty()) {
            return new JsonObject();
        }
        throw new DecodeException("Unexpected e-sidoc loans response: " + body);
    }

    /**
     * Books still borrowed in an e-sidoc loans response, as displayed by the library widget:
     * {@code {id, title, dueDate, overdue}}. The returned books are left out of the history.
     *
     * @param uai structure of the response, prefixing the loan ids since a borrower may have loans in several structures
     */
    public static JsonArray currentLoans(JsonObject response, String uai) {
        final JsonObject loans = response.getJsonObject("prets", new JsonObject());
        final JsonObject items = loans.getValue("items") instanceof JsonObject ? loans.getJsonObject("items") : new JsonObject();
        final JsonArray currentLoans = new JsonArray();
        addLoans(currentLoans, items.getValue(OVERDUE_LOANS), uai, true);
        addLoans(currentLoans, items.getValue(CURRENT_LOANS), uai, false);
        return currentLoans;
    }

    /**
     * @return the loans of several structures in a single list, the most urgent first
     */
    public static JsonArray mergeByDueDate(List<JsonArray> loansByStructure) {
        final List<JsonObject> loans = new ArrayList<>();
        for (JsonArray structureLoans : loansByStructure) {
            for (Object loan : structureLoans) {
                if (loan instanceof JsonObject) {
                    loans.add((JsonObject) loan);
                }
            }
        }
        // ISO dates sort chronologically as strings
        loans.sort(Comparator.comparing((JsonObject loan) -> loan.getString("dueDate"),
                Comparator.nullsLast(Comparator.<String>naturalOrder())));
        return new JsonArray(new ArrayList<Object>(loans));
    }

    private static void addLoans(JsonArray currentLoans, Object bucket, String uai, boolean overdue) {
        // e-sidoc returns a bucket as an object keyed by loan id, or as an array when it is empty
        if (bucket instanceof JsonObject) {
            for (Map.Entry<String, Object> loan : (JsonObject) bucket) {
                addLoan(currentLoans, loan.getKey(), loan.getValue(), uai, overdue);
            }
        } else if (bucket instanceof JsonArray) {
            final JsonArray loans = (JsonArray) bucket;
            for (int i = 0; i < loans.size(); i++) {
                addLoan(currentLoans, String.valueOf(i), loans.getValue(i), uai, overdue);
            }
        }
    }

    private static void addLoan(JsonArray currentLoans, String loanId, Object loan, String uai, boolean overdue) {
        if (!(loan instanceof JsonObject)) {
            return;
        }
        final JsonObject esidocLoan = (JsonObject) loan;
        currentLoans.add(new JsonObject()
                .put("id", uai + "-" + loanId)
                .put("title", esidocLoan.getString("titre", ""))
                .put("dueDate", esidocLoan.getString("date_retour"))
                .put("overdue", overdue));
    }

    private static String encodePathSegment(String value) {
        // URLEncoder targets form bodies, where a space is a "+"
        return encode(value).replace("+", "%20");
    }
}
