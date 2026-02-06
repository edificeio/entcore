package org.entcore.broker.api.dto.directory.structure;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.entcore.broker.api.dto.directory.clazz.ClassDTOClassAdmin;
import java.util.List;

public class SchoolDTOClassAdmin {
    private final List<ClassDTOClassAdmin> classes;
    private final String name;
    private final String id;
    private final String source;

    @JsonCreator
    public SchoolDTOClassAdmin(
            @JsonProperty("classes") List<ClassDTOClassAdmin> classes,
            @JsonProperty("name") String name,
            @JsonProperty("id") String id,
            @JsonProperty("source") String source) {
        this.classes = classes;
        this.name = name;
        this.id = id;
        this.source = source;
    }

    public List<ClassDTOClassAdmin> getClasses() {
        return classes;
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }

    public String getSource() {
        return source;
    }
}