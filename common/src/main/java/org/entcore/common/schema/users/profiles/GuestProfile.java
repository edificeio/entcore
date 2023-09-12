package org.entcore.common.schema.users;

import org.entcore.common.schema.users.Profile;

class GuestProfile extends Profile
{
    protected GuestProfile()
    {
        super("Guest", "PROFILE_GUEST");
    }
}