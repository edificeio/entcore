package org.entcore.communication.dto.rest;

import org.entcore.communication.services.CommunicationService;

public class GroupUsersDirectionDTO {

    private CommunicationService.Direction users;

    public GroupUsersDirectionDTO() {}

    public GroupUsersDirectionDTO(CommunicationService.Direction users) {
        this.users = users;
    }

    public CommunicationService.Direction getUsers() {
        return users;
    }

    public void setUsers(CommunicationService.Direction users) {
        this.users = users;
    }
}