package org.entcore.communication.dto.rest;

import java.util.List;

public class InitDefaultRulesDTO {

    private List<String> structures;

    public List<String> getStructures() {
        return structures;
    }

    public InitDefaultRulesDTO setStructures(List<String> structures) {
        this.structures = structures;
        return this;
    }
}