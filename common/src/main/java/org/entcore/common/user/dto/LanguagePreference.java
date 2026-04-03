package org.entcore.common.user.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;
import java.util.Map;

public class LanguagePreference implements Preference {

    Map<String, String> languages =  new HashMap<>();

    @JsonAnySetter
    public void add(String key, String value) {
        languages.put(key, value);
    }

    @JsonValue
    public Map<String, String> getLanguages() {
        return languages;
    }

}
