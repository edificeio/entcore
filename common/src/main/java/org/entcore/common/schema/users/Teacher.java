package org.entcore.common.schema.users;

import org.entcore.common.utils.ExternalId;

public class Teacher extends User<TeacherProfile>
{
    public Teacher(String id)
    {
        super(Profile.TEACHER, id, null);
    }

    public Teacher(ExternalId<User> externalId)
    {
        super(Profile.TEACHER, null, externalId);
    }
}
