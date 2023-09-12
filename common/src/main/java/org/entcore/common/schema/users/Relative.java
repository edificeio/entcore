package org.entcore.common.schema.users;

import org.entcore.common.utils.ExternalId;

public class Relative extends User<RelativeProfile>
{
    public Relative(String id)
    {
        super(Profile.RELATIVE, id, null);
    }

    public Relative(ExternalId<User> externalId)
    {
        super(Profile.RELATIVE, null, externalId);
    }
}
