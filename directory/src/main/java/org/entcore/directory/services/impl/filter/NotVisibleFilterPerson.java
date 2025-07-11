package org.entcore.directory.services.impl.filter;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;

public class NotVisibleFilterPerson {

    private final JsonObject userInfos;
    private final boolean applyFilter;
    private static final List<String> authorizedInfosForNotVisible =
            Lists.newArrayList(new String[] {"id", "displayName", "type"});

    public NotVisibleFilterPerson(JsonArray userInfos, boolean applyFilter) {
        this.userInfos = (JsonObject) userInfos.getValue(0);
        this.applyFilter = applyFilter;
    }

    public JsonArray apply() {
        JsonObject filteredPerson = userInfos.copy();
        JsonArray result = new JsonArray();
        result.add(filteredPerson);
        if(!applyFilter) {
            return result;
        }
        for(String field : filteredPerson.getMap().keySet()) {
            if(!authorizedInfosForNotVisible.contains(field)) {
                filteredPerson.putNull(field);
            }
        }
        return result;
    }

}
