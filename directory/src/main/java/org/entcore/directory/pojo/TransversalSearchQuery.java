package org.entcore.directory.pojo;

public class TransversalSearchQuery {
    public static final TransversalSearchQuery EMPTY = new TransversalSearchQuery(TransversalSearchType.NONE, null, null, null);
    private final String term;
    private final String lastName;
    private final String firstName;
    private final TransversalSearchType searchType;

    private TransversalSearchQuery(
            final TransversalSearchType type,
            final String lastName, 
            final String firstName, 
            final String term 
            ) {
        this.searchType = type;
        this.lastName = lastName;
        this.firstName = firstName;
        this.term = term;
    }

    public static TransversalSearchQuery searchByFullName(final String lastName, final String firstName) {
        return new TransversalSearchQuery(
            TransversalSearchType.FULL_NAME,
            lastName,
            firstName,
            null
        );
    }

    public static TransversalSearchQuery searchByDisplayName(final String fullname) {
        return new TransversalSearchQuery(
            TransversalSearchType.DISPLAY_NAME,
            null,
            null,
            fullname
        );
    }

    public static TransversalSearchQuery searchByMail(final String email) {
        return new TransversalSearchQuery(
            TransversalSearchType.EMAIL,
            null,
            null,
            email
        );
    }

    public String getEmail() {
        return term;
    }

    public String getDisplayName() {
        return term;
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
