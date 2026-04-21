package org.entcore.communication.dto.rest;

public class CountResultDTO {

    public int count;

    public CountResultDTO(int count) {
        this.count = count;
    }

    public CountResultDTO() {
        //for serialization
    }

}
