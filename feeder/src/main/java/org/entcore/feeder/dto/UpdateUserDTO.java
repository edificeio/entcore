package org.entcore.feeder.dto;

public class UpdateUserDTO {

    private String userId;
    private String callerId;
    private UpdateUserDataDTO data;

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getCallerId() { return callerId; }
    public void setCallerId(String callerId) { this.callerId = callerId; }

    public UpdateUserDataDTO getData() { return data; }
    public void setData(UpdateUserDataDTO data) { this.data = data; }
}