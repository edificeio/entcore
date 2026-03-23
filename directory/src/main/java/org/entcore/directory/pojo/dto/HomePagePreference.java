package org.entcore.directory.pojo.dto;

public class HomePagePreference implements ApplicationPreference {

    private boolean betaEnabled;

    public boolean isBetaEnabled() {
        return betaEnabled;
    }

    public HomePagePreference setBetaEnabled(boolean betaEnabled) {
        this.betaEnabled = betaEnabled;
        return this;
    }


}
