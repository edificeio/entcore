package org.entcore.communication.dto.rest;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommuniqueWithDTO extends GroupDTO {

    private List<GroupDTO> communiqueWith;

    public List<GroupDTO> getCommuniqueWith() {
        return communiqueWith;
    }

    public CommuniqueWithDTO setCommuniqueWith(List<GroupDTO> communiqueWith) {
        this.communiqueWith = communiqueWith;
        return this;
    }
}