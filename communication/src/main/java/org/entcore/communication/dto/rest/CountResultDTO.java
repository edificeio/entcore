package org.entcore.communication.dto.rest;

public class CountResultDTO {

    private final int count;

    public CountResultDTO(int count) {
        this.count = count;
    }

    public int getCount() {
        return count;
    }
}