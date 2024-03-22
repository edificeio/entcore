package org.entcore.test.preparation;

public final class UserBookTestBuilder {
    private String userId;
    private String ine;
    private long quota;
    private long storage;

    private UserBookTestBuilder() {
    }

    public static UserBookTestBuilder anUserBookTest() {
        return new UserBookTestBuilder();
    }

    public UserBookTestBuilder userId(String userId) {
        this.userId = userId;
        return this;
    }

    public UserBookTestBuilder ine(String ine) {
        this.ine = ine;
        return this;
    }

    public UserBookTestBuilder quota(long quota) {
        this.quota = quota;
        return this;
    }

    public UserBookTestBuilder storage(long storage) {
        this.storage = storage;
        return this;
    }

    public UserBookTest build() {
        return new UserBookTest(userId, ine, quota, storage);
    }
}
