package org.entcore.communication.dto.rest;

import org.entcore.communication.services.CommunicationService;

public class RemoveRelationsResultDTO {

    private CommunicationService.Direction sender;
    private CommunicationService.Direction receiver;

    public RemoveRelationsResultDTO() {}

    public RemoveRelationsResultDTO(CommunicationService.Direction sender, CommunicationService.Direction receiver) {
        this.sender = sender;
        this.receiver = receiver;
    }

    public CommunicationService.Direction getSender() {
        return sender;
    }

    public void setSender(CommunicationService.Direction sender) {
        this.sender = sender;
    }

    public CommunicationService.Direction getReceiver() {
        return receiver;
    }

    public void setReceiver(CommunicationService.Direction receiver) {
        this.receiver = receiver;
    }
}