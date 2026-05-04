package org.entcore.common.user.dto;

public class HomePagePreference implements Preference {

    private boolean betaEnabled;
    private String closeBetaSwitch;

    public boolean isBetaEnabled() {
        return betaEnabled;
    }

    public HomePagePreference setBetaEnabled(boolean betaEnabled) {
        this.betaEnabled = betaEnabled;
        return this;
    }

    public String getCloseBetaSwitch() {
        return closeBetaSwitch;
    }

    public HomePagePreference setCloseBetaSwitch(String closeBetaSwitch) {
        this.closeBetaSwitch = closeBetaSwitch;
        return this;
    }
}
