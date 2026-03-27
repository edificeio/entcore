package org.entcore.common.user.dto;

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
