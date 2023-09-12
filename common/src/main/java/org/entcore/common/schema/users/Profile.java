package org.entcore.common.schema.users;

import org.entcore.common.utils.ExternalId;

public abstract class Profile
{
    public static final TeacherProfile TEACHER = new TeacherProfile();
    public static final StudentProfile STUDENT = new StudentProfile();
    public static final RelativeProfile RELATIVE = new RelativeProfile();
    public static final PersonnelProfile PERSONNEL = new PersonnelProfile();
    public static final GuestProfile GUEST = new GuestProfile();

    public String name;
    public ExternalId<Profile> externalId;

    public Profile(String name, String externalId)
    {
        this.name = name;
        this.externalId = new ExternalId(externalId);
    }
}
