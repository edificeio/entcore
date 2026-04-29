package org.entcore.communication.dto.rest;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class DiscoverVisibleStructureDTO {

    private String id;
    private String type;
    private String label;
    private boolean checked;

    public DiscoverVisibleStructureDTO() {}

    public DiscoverVisibleStructureDTO(String id, String type, String label, boolean checked) {
        this.id = id;
        this.type = type;
        this.label = label;
        this.checked = checked;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public boolean isChecked() { return checked; }
    public void setChecked(boolean checked) { this.checked = checked; }
}