package org.entcore.directory.pojo.structure;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;
import java.util.Map;

public class DefaultAuthModeConfig {

    private final Map<Profile, DefaultAuthMode> defaultAuthModes = new HashMap<>();

    public Map<Profile, DefaultAuthMode> getDefaultAuthModes() {
        return defaultAuthModes;
    }

    public enum DefaultAuthMode {
        ENT,
        FEDERATED;
    }

    public enum Profile {

        PERSONNEL("Personnel"),
        STUDENT("Student"),
        TEACHER("Teacher"),
        RELATIVE("Relative"),
        GUEST("Guest");

        private final String neo4jName;

        Profile(String neo4jName) {
            this.neo4jName = neo4jName;
        }

        @JsonCreator
        public static Profile fromNeo4j(String profile) {
            for (Profile p : Profile.values()) {
                if (p.neo4jName.equals(profile)) {
                    return p;
                }
            }
            return null;
        }

        @JsonValue
        public String getNeo4jName(){
            return neo4jName;
        }


    }
}
