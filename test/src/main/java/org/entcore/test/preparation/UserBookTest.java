package org.entcore.test.preparation;

public class UserBookTest {
    private final String userId;
    private final String ine;
    private final long quota;
    private final long storage;

    public UserBookTest(final String userId, final String ine, final long quota, final long storage) {
        this.userId = userId;
        this.ine = ine;
        this.quota = quota;
        this.storage = storage;
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

}
