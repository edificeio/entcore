package org.entcore.communication.dto.rest;

import org.entcore.communication.services.CommunicationService;

import java.util.Map;

public class AddLinkDirectionsDTO {

    private Map<String, CommunicationService.Direction> directions;

    public AddLinkDirectionsDTO() {}

    public AddLinkDirectionsDTO(Map<String, CommunicationService.Direction> directions) {
        this.directions = directions;
    }

    public Map<String, CommunicationService.Direction> getDirections() {
        return directions;
    }

    public void setDirections(Map<String, CommunicationService.Direction> directions) {
        this.directions = directions;
    }
}