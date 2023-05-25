package org.entcore.directory.pojo;

public class TransversalSearchQuery {
    public static final TransversalSearchQuery EMPTY = new TransversalSearchQuery(null);
    private final String email;
    private final String lastName;
    private final String firstName;
    private final TransversalSearchType searchType;

    public TransversalSearchQuery(final String lastName, final String firstName) {
        this.lastName = lastName;
        this.firstName = firstName;
        this.searchType = TransversalSearchType.NAME;
        this.email = null;
    }

    public TransversalSearchQuery(final String email) {
        this.email = email;
        this.lastName = null;
        this.firstName = null;
        this.searchType = TransversalSearchType.EMAIL;
    }

    public String getEmail() {
        return email;
    }

    public String getLastName() {
        return lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public TransversalSearchType getSearchType() {
        return searchType;
    }
}
