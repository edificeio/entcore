package org.entcore.common.schema.users;

import org.entcore.common.utils.ExternalId;

public class Personnel extends User<PersonnelProfile>
{
    public Personnel(String id)
    {
        super(Profile.PERSONNEL, id, null);
    }

    public Personnel(ExternalId<User> externalId)
    {
        super(Profile.PERSONNEL, null, externalId);
    }
}
