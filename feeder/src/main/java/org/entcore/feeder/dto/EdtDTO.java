package org.entcore.feeder.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

@JsonInclude(JsonInclude.Include.NON_NULL)
@DataObject
@JsonGen
public class EdtDTO {

    private String uai;
    private String path;
    private String language = "fr";
    private Boolean updateGroups = true;
    private Boolean updateTimetable = true;
    private Boolean isManualImport = false;

    public EdtDTO() {}

    public EdtDTO(JsonObject json) {
        EdtDTOConverter.fromJson(json, this);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        EdtDTOConverter.toJson(this, json);
        return json;
    }

    public String getUai() { return uai; }
    public EdtDTO setUai(String uai) { this.uai = uai; return this; }

    public String getPath() { return path; }
    public EdtDTO setPath(String path) { this.path = path; return this; }

    public String getLanguage() { return language; }
    public EdtDTO setLanguage(String language) { this.language = language; return this; }

    public Boolean getUpdateGroups() { return updateGroups; }
    public EdtDTO setUpdateGroups(Boolean updateGroups) { this.updateGroups = updateGroups; return this; }

    public Boolean getUpdateTimetable() { return updateTimetable; }
    public EdtDTO setUpdateTimetable(Boolean updateTimetable) { this.updateTimetable = updateTimetable; return this; }

    public Boolean getIsManualImport() { return isManualImport; }
    public EdtDTO setIsManualImport(Boolean isManualImport) { this.isManualImport = isManualImport; return this; }
}