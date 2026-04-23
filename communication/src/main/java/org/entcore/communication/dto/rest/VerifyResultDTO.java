package org.entcore.communication.dto.rest;

public class VerifyResultDTO {

    private boolean canCommunicate;

    public VerifyResultDTO() {}

    public VerifyResultDTO(boolean canCommunicate) {
        this.canCommunicate = canCommunicate;
    }

    public boolean isCanCommunicate() {
        return canCommunicate;
    }

    public void setCanCommunicate(boolean canCommunicate) {
        this.canCommunicate = canCommunicate;
    }
}