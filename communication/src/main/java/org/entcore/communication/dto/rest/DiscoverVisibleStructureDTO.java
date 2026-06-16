package org.entcore.communication.dto.rest;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class DiscoverVisibleStructureDTO {

    private final String id;
    private final String type;
    private final String label;
    private final boolean checked;

    public DiscoverVisibleStructureDTO(String id, String type, String label, boolean checked) {
        this.id = id;
        this.type = type;
        this.label = label;
        this.checked = checked;
    }

    public String getId() { return id; }
    public String getType() { return type; }
    public String getLabel() { return label; }
    public boolean isChecked() { return checked; }
}