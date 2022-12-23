package org.entcore.common.mute;

import io.vertx.core.Future;
import org.entcore.common.explorer.to.MuteRequest;
import org.entcore.common.user.UserInfos;

public interface MuteService {

    /**
     *
     * @param muteRequest The mute actions to perform on the resources for the user
     * @param user The user who requested the mute actions
     * @return Succeeds if everything went fine
     */
    Future<Void> setMuteStatus(MuteRequest muteRequest, UserInfos user);

    /**
     * Bus address of the function that retrieves the ids of the users who
     * muted a resource.
     */
    String FETCH_RESOURCE_MUTES_BY_ENTID_ADRESS = "mute.fetch.by.entid";
}
