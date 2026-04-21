package org.entcore.communication.dto.rest;

import java.util.ArrayList;
import java.util.List;

public class SearchVisibleResultDTO {

    private List<GroupDTO> groups = new ArrayList<>();
    private List<UserDTO> users = new ArrayList<>();

    public SearchVisibleResultDTO addGroup(GroupDTO group) {
        this.groups.add(group);
        return this;
    }

    public SearchVisibleResultDTO addUser(UserDTO user) {
        this.users.add(user);
        return this;
    }

    public List<UserDTO> getUsers() {
        return users;
    }

    public List<GroupDTO> getGroups() {
        return groups;
    }
}
