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

    private final JsonObject userInfos;
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
        this.userInfos = userInfos.getJsonObject(0);
        this.isFilterEnabled = isFilterEnabled;
    }

    private void validateInput(JsonArray userInfos) {
        if (userInfos == null) {
            throw new IllegalArgumentException("User infos cannot be null");
        }
        if (userInfos.isEmpty()) {
            throw new IllegalArgumentException("User infos array cannot be empty");
        }
        try {
            userInfos.getJsonObject(0);
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("First element must be a JsonObject", e);
        }
    }

    public JsonArray apply() {
        JsonObject filteredPerson = userInfos.copy();
        JsonArray result = new JsonArray();
        result.add(filteredPerson);
        if(!isFilterEnabled) {
            return result;
        }
        for(String field : filteredPerson.getMap().keySet()) {
            if(!AUTHORIZED_FIELD.contains(field)) {
                filteredPerson.putNull(field);
            }
        }
        return result;
    }

}
