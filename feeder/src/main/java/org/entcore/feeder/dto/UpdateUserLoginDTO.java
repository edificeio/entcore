package org.entcore.feeder.dto;

public class UpdateUserLoginDTO {

    private String userId;
    private String login;

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }
}