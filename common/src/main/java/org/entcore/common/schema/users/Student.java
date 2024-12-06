package org.entcore.common.schema.users;

import org.entcore.common.utils.ExternalId;

public class Student extends User<StudentProfile>
{
    public Student(String id)
    {
        super(Profile.STUDENT, id, null);
    }

    public Student(ExternalId<User> externalId)
    {
        super(Profile.STUDENT, null, externalId);
    }
}
