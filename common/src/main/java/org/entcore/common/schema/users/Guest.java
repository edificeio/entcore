package org.entcore.common.schema.users;

import org.entcore.common.utils.ExternalId;

public class Guest extends User<GuestProfile>
{
    public Guest(String id)
    {
        super(Profile.GUEST, id, null);
    }

    public Guest(ExternalId<User> externalId)
    {
        super(Profile.GUEST, null, externalId);
    }
}
