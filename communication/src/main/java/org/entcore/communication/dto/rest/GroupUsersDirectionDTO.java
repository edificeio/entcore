package org.entcore.communication.dto.rest;

import org.entcore.communication.services.CommunicationService;

public class GroupUsersDirectionDTO {

    private final CommunicationService.Direction users;

    public GroupUsersDirectionDTO(CommunicationService.Direction users) {
        this.users = users;
    }

    public CommunicationService.Direction getUsers() {
        return users;
    }
}