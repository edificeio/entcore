package org.entcore.test.preparation;

import com.google.common.collect.ImmutableList;

import java.util.List;

public class UserBookTest {

    private final String userId;
    private final String ine;
    private final long quota;
    private final long storage;
    private final List<String> visibleInfos;

    public UserBookTest(final String userId, final String ine, final long quota, final long storage) {
        this.userId = userId;
        this.ine = ine;
        this.quota = quota;
        this.storage = storage;
        this.visibleInfos = ImmutableList.of();
    }

    public UserBookTest(final String userId, String[] visibleInfos) {
        this.userId = userId;
        this.ine = "";
        this.quota = 0;
        this.storage = 0;
        this.visibleInfos = ImmutableList.copyOf(visibleInfos);
    }

    public String getUserId() {
        return userId;
    }

    public String getIne() {
        return ine;
    }

    public long getQuota() {
        return quota;
    }

    public long getStorage() {
        return storage;
    }

    public List<String> getVisibleInfos() { return visibleInfos; }

}
