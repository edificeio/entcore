package org.entcore.registry.services;

import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import org.entcore.common.user.UserInfos;


public interface LibraryService {
    Future<JsonObject> publish(UserInfos user,String locale, MultiMap form, Buffer cover, Buffer teacherAvatar);
}
