package org.entcore.test.preparation;

public class UserTest {
    private final String id;
    private final String login;
    private final String firstName;
    private final String lastName;
    private final Profile profile;
    private final UserBookTest userBook;

    public UserTest(final String id, final String login, final String firstName, final String lastName,
                    final Profile profile, final UserBookTest userBook) {
        this.id = id;
        this.login = login;
        this.firstName = firstName;
        this.lastName = lastName;
        this.profile = profile;
        this.userBook = userBook;
    }

    public String getId() {
        return id;
    }

    public String getLogin() {
        return login;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public UserBookTest getUserBook() {
        return userBook;
    }

    public Profile getProfile() {
        return profile;
    }
}
