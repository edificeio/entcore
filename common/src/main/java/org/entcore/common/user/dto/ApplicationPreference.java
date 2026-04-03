package org.entcore.common.user.dto;

import java.util.ArrayList;
import java.util.List;

public class ApplicationPreference implements Preference {

    private List<String> bookmarks = new ArrayList<>();
    private List<String> applications = new ArrayList<>();

    public List<String> getApplications() {
        return applications;
    }

    public void setApplications(List<String> applications) {
        this.applications = applications;
    }

    public List<String> getBookmarks() {
        return bookmarks;
    }

    public void setBookmarks(List<String> bookmarks) {
        this.bookmarks = bookmarks;
    }
}
