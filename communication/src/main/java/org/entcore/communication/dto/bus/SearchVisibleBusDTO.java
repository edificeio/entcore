package org.entcore.communication.dto.bus;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@DataObject
@JsonGen
public class SearchVisibleBusDTO {

    private String userId;
    private List<String> userIds;
    private String action;
    private String schoolId;
    private List<String> expectedTypes = new ArrayList<>();
    private String userProfile;
    private String preFilter;
    private String customReturn;
    private boolean reverseUnion;
    private Map<String, Object> additionnalParams;
    // fields from the former SearchVisibleRequestDTO parent
    private String search;
    private boolean itSelf;
    private boolean myGroup;
    private boolean profile = true;

    public SearchVisibleBusDTO() {}

    public SearchVisibleBusDTO(JsonObject json) {
        this();
        SearchVisibleBusDTOConverter.fromJson(json, this);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        SearchVisibleBusDTOConverter.toJson(this, json);
        return json;
    }

    public String getUserId() { return userId; }
    public SearchVisibleBusDTO setUserId(String userId) { this.userId = userId; return this; }

    public List<String> getUserIds() { return userIds; }
    public SearchVisibleBusDTO setUserIds(List<String> userIds) { this.userIds = userIds; return this; }

    public String getAction() { return action; }
    public SearchVisibleBusDTO setAction(String action) { this.action = action; return this; }

    public String getSchoolId() { return schoolId; }
    public SearchVisibleBusDTO setSchoolId(String schoolId) { this.schoolId = schoolId; return this; }

    public List<String> getExpectedTypes() { return expectedTypes; }
    public SearchVisibleBusDTO setExpectedTypes(List<String> expectedTypes) {
        this.expectedTypes = expectedTypes != null ? expectedTypes : new ArrayList<>();
        return this;
    }

    public String getUserProfile() { return userProfile; }
    public SearchVisibleBusDTO setUserProfile(String userProfile) { this.userProfile = userProfile; return this; }

    public String getPreFilter() { return preFilter; }
    public SearchVisibleBusDTO setPreFilter(String preFilter) { this.preFilter = preFilter; return this; }

    public String getCustomReturn() { return customReturn; }
    public SearchVisibleBusDTO setCustomReturn(String customReturn) { this.customReturn = customReturn; return this; }

    public boolean isReverseUnion() { return reverseUnion; }
    public SearchVisibleBusDTO setReverseUnion(boolean reverseUnion) { this.reverseUnion = reverseUnion; return this; }

    public Map<String, Object> getAdditionnalParams() { return additionnalParams; }
    public SearchVisibleBusDTO setAdditionnalParams(Map<String, Object> additionnalParams) { this.additionnalParams = additionnalParams; return this; }

    public String getSearch() { return search; }
    public SearchVisibleBusDTO setSearch(String search) { this.search = search; return this; }

    public boolean isItSelf() { return itSelf; }
    public SearchVisibleBusDTO setItSelf(boolean itSelf) { this.itSelf = itSelf; return this; }

    public boolean isMyGroup() { return myGroup; }
    public SearchVisibleBusDTO setMyGroup(boolean myGroup) { this.myGroup = myGroup; return this; }

    /** Defaults to {@code true} when absent from the JSON payload. */
    public boolean isProfile() { return profile; }
    public SearchVisibleBusDTO setProfile(boolean profile) { this.profile = profile; return this; }
}