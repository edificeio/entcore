package org.entcore.test.preparation;

public enum Profile {
    Teacher("Teacher", "PROFILE_TEACHER"),
    Personnel("Personnel", "PROFILE_PERSONNEL"),
    Relative("Relative", "PROFILE_RELATIVE"),
    Student("Student", "PROFILE_STUDENT"),
    Guest("Guest", "PROFILE_GUEST");
    public final String name;
    public final String externalId;
    public final String id;

    Profile(final String name, final String externalId) {
        this.name = name;
        this.externalId = externalId;
        this.id = externalId;
    }

}
