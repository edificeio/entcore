package org.entcore.communication.dto.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DiscoverModifyGroupUsersDTO {

    private List<String> oldUsers = new ArrayList<>();
    private List<String> newUsers;

    public List<String> getOldUsers() {
        return oldUsers;
    }

    public DiscoverModifyGroupUsersDTO setOldUsers(List<String> oldUsers) {
        this.oldUsers = oldUsers != null ? oldUsers : new ArrayList<>();
        return this;
    }

    public List<String> getNewUsers() {
        return newUsers;
    }

    public DiscoverModifyGroupUsersDTO setNewUsers(List<String> newUsers) {
        this.newUsers = newUsers;
        return this;
    }
}