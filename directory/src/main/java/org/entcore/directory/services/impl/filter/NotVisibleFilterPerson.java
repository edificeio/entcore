package org.entcore.directory.services.impl.filter;

import com.google.common.collect.ImmutableSet;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.Set;

/**
 * Filtre les informations sensibles d'un utilisateur provenant de Neo4j.
 * Préserve la structure JsonArray et les clés pour maintenir un contrat API consistant.
 */
public class NotVisibleFilterPerson {

    private final JsonArray userInfos;
    private final boolean isFilterEnabled;
    private static final Set<String> AUTHORIZED_FIELD =
            ImmutableSet.of("id", "displayName", "type");

    /**
     * Constructeur pour filtrer les informations d'un utilisateur.
     * @param userInfos JsonArray contenant un seul objet utilisateur
     * @param isFilterEnabled true pour appliquer le filtre, false pour retourner les données originales
     * @throws IllegalArgumentException si userInfos est null, vide ou ne contient pas un JsonObject
     */
    public NotVisibleFilterPerson(JsonArray userInfos, boolean isFilterEnabled) {
        validateInput(userInfos);
        this.userInfos = userInfos;
        this.isFilterEnabled = isFilterEnabled;
    }

    private void validateInput(JsonArray userInfos) {
        if (userInfos == null) {
            throw new IllegalArgumentException("User infos cannot be null");
        }
        try {
            userInfos.getJsonObject(0);
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("First element must be a JsonObject", e);
        }
    }

    public JsonArray apply() {
        JsonArray results = new JsonArray();
        for (int i = 0; i < userInfos.size(); i++) {
            results.add(userInfos.getJsonObject(i).copy());
        }

        if (isFilterEnabled) {
            for (int i = 0; i < results.size(); i++) {
                JsonObject userInfo = results.getJsonObject(i);
                for (String field : userInfo.getMap().keySet()) {
                    if (!AUTHORIZED_FIELD.contains(field)) {
                        userInfo.putNull(field);
                    }
                }
            }
        }

        return results;
    }
}
