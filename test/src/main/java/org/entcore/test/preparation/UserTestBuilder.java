package org.entcore.test.preparation;

public final class UserTestBuilder {
    private String id;
    private String login;
    private String firstName;
    private String lastName;
    private String displayName;
    private Profile profile;
    private UserBookTest userBook;

    private UserTestBuilder() {
    }

    public static UserTestBuilder anUserTest() {
        return new UserTestBuilder();
    }

    public UserTestBuilder id(String id) {
        this.id = id;
        return this;
    }

    public UserTestBuilder login(String login) {
        this.login = login;
        return this;
    }

    public UserTestBuilder firstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    public UserTestBuilder lastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    public UserTestBuilder displayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    public UserTestBuilder profile(Profile profile) {
        this.profile = profile;
        return this;
    }

    public UserTestBuilder userBook(UserBookTest userBook) {
        this.userBook = userBook;
        return this;
    }

    public UserTest build() {
        return new UserTest(id, login, firstName, lastName, displayName, profile, userBook);
    }
}
