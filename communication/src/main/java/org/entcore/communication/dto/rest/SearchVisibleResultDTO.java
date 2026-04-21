package org.entcore.communication.dto.rest;

import java.util.List;

public class SearchVisibleResultDTO {

    private final List<GroupDTO> groups;
    private final List<UserDTO> users;

    public SearchVisibleResultDTO(List<GroupDTO> groups, List<UserDTO> users) {
        this.groups = groups;
        this.users = users;
    }

    public List<GroupDTO> getGroups() {
        return groups;
    }

    public List<UserDTO> getUsers() {
        return users;
    }
}