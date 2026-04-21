package org.entcore.communication.dto.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SearchVisibleBusDTO extends SearchVisibleRestDTO {

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

    public String getUserId() {
        return userId;
    }

    public SearchVisibleBusDTO setUserId(String userId) {
        this.userId = userId;
        return this;
    }

    public List<String> getUserIds() {
        return userIds;
    }

    public SearchVisibleBusDTO setUserIds(List<String> userIds) {
        this.userIds = userIds;
        return this;
    }

    public String getAction() {
        return action;
    }

    public SearchVisibleBusDTO setAction(String action) {
        this.action = action;
        return this;
    }

    public String getSchoolId() {
        return schoolId;
    }

    public SearchVisibleBusDTO setSchoolId(String schoolId) {
        this.schoolId = schoolId;
        return this;
    }

    public List<String> getExpectedTypes() {
        return expectedTypes;
    }

    public SearchVisibleBusDTO setExpectedTypes(List<String> expectedTypes) {
        this.expectedTypes = expectedTypes;
        return this;
    }

    public String getUserProfile() {
        return userProfile;
    }

    public SearchVisibleBusDTO setUserProfile(String userProfile) {
        this.userProfile = userProfile;
        return this;
    }

    public String getPreFilter() {
        return preFilter;
    }

    public SearchVisibleBusDTO setPreFilter(String preFilter) {
        this.preFilter = preFilter;
        return this;
    }

    public String getCustomReturn() {
        return customReturn;
    }

    public SearchVisibleBusDTO setCustomReturn(String customReturn) {
        this.customReturn = customReturn;
        return this;
    }

    public boolean isReverseUnion() {
        return reverseUnion;
    }

    public SearchVisibleBusDTO setReverseUnion(boolean reverseUnion) {
        this.reverseUnion = reverseUnion;
        return this;
    }

    public Map<String, Object> getAdditionnalParams() {
        return additionnalParams;
    }

    public SearchVisibleBusDTO setAdditionnalParams(Map<String, Object> additionnalParams) {
        this.additionnalParams = additionnalParams;
        return this;
    }
}
