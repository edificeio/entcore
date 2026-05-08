package org.entcore.broker.api.dto.directory.user;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Custom deserializer for FunctionDTOStructure to handle array-based JSON format:
 * ["ADMIN_LOCAL", ["759b4e32-ebcc-431c-8164-1171cba67e19"]]
 * or 
 * ["SUPER_ADMIN", null]
 */
public class FunctionDTOStructureDeserializer extends JsonDeserializer<FunctionDTOStructure> {

    @Override
    public FunctionDTOStructure deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        JsonNode node = parser.getCodec().readTree(parser);
        
        if (!node.isArray() || node.size() != 2) {
            throw new IOException("Expected array with 2 elements for FunctionDTOStructure, got: " + node);
        }
        
        // First element is the role
        String role = node.get(0).asText();
        
        // Second element can be an array of structure IDs or null
        JsonNode structureIdsNode = node.get(1);
        List<String> structureIds = null;
        
        if (structureIdsNode != null && !structureIdsNode.isNull()) {
            if (structureIdsNode.isArray()) {
                structureIds = new ArrayList<>();
                for (JsonNode idNode : structureIdsNode) {
                    structureIds.add(idNode.asText());
                }
            } else {
                throw new IOException("Expected array or null for structure IDs, got: " + structureIdsNode);
            }
        }
        
        return new FunctionDTOStructure(role, structureIds);
    }
}