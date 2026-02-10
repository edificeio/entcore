package org.entcore.broker.api.dto.directory.user;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;


/**
 * We define the type of functions as List<List<Object>> because the structure of this field can be different 
 * based on the user and the source of the data, we can have for example :
 * ###### 1
 * "functions": [ [ "ADMIN_LOCAL", [ "759b4e32-ebcc-431c-8164-1171cba67e19" ] ] ]
 * 
 * ###### 2
 * "functions": [ [ "SUPER_ADMIN", null ] ]
 */
@JsonDeserialize(using = FunctionDTOStructureDeserializer.class)
public class FunctionDTOStructure {
    private final String role;
    private final List<String> structureIds;

    public FunctionDTOStructure(String role, List<String> structureIds) {
        this.role = role;
        this.structureIds = structureIds;
    }

    public String getRole() {
        return role;
    }

    public List<String> getStructureIds() {
        return structureIds;
    }
}