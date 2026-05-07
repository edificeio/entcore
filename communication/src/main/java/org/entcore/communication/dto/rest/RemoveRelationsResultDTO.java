package org.entcore.communication.dto.rest;

import org.entcore.communication.services.CommunicationService;

public class RemoveRelationsResultDTO {

    private final CommunicationService.Direction sender;
    private final CommunicationService.Direction receiver;

    public RemoveRelationsResultDTO(CommunicationService.Direction sender, CommunicationService.Direction receiver) {
        this.sender = sender;
        this.receiver = receiver;
    }

    public CommunicationService.Direction getSender() {
        return sender;
    }

    public CommunicationService.Direction getReceiver() {
        return receiver;
    }
}