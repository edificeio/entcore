package org.entcore.feeder.dictionary.structures;

import io.vertx.core.json.JsonObject;
import org.entcore.feeder.Feeder;
import org.entcore.feeder.utils.TransactionHelper;

public class Subject {

    public static void createManualSubject(JsonObject subject, TransactionHelper transactionHelper) {

        JsonObject params = new JsonObject()
                .put("structureId", subject.getString("structureId"))
                .put("label", subject.getString("label"))
                .put("code", subject.getString("code"))
                .put("source", Feeder.SUBJECT_SOURCE)
                .put("code", subject.getString("code"));


        String query = "MATCH (s: Structure {id : {structureId} })" +
                "MERGE (s)<-[r: SUBJECT]-(sub:Subject { code: {code}, label: {label}, source: {source}, " +
                "externalId: s.externalId + '$M' + {code} }) " +
                "ON CREATE SET sub.id = id(sub) + '-' + timestamp()" +
                "RETURN sub.id as id, sub.label as label, sub.code as code, sub.source as source;";

        transactionHelper.add(query, params);
    }

    public static void updateManualSubject (JsonObject subject, TransactionHelper transactionHelper) {

        JsonObject params = new JsonObject()
                .put("id", subject.getString("id"))
                .put("label", subject.getString("label"))
                .put("code", subject.getString("code"))
                .put("code", subject.getString("code"));

        String query = "MATCH (sub: Subject {id : {id} })-[r: SUBJECT] ->(s : Structure)" +
                "SET sub.label = {label}, sub.code = {code}, sub.externalId = s.externalId + '$M' + {code} " +
                "RETURN sub.id as id, sub.label as label, sub.code as code, sub.source as source;";
        transactionHelper.add(query, params);
    }

    public static void deleteManualSubject (String subjectId, TransactionHelper transactionHelper) {
        JsonObject params = new JsonObject()
                .put("id", subjectId);
        String query = "MATCH (s: Subject {id : {id} }) " +
                "DETACH DELETE s ";
        transactionHelper.add(query, params);
    }
}
