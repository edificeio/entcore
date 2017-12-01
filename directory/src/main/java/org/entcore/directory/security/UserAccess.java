package org.entcore.directory.security;

import fr.wseduc.webutils.http.Binding;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.user.UserInfos;

public class UserAccess extends AnyAdminOfUser {

    @Override
    public void authorize(final HttpServerRequest request, Binding binding, final UserInfos user, final Handler<Boolean> handler) {

        String userId = request.params().get("userId");
        if (userId == null || userId.trim().isEmpty()) {
            handler.handle(false);
            return;
        }
        if (userId.equals(user.getUserId())) {
            handler.handle(true);
            return;
        }
        super.authorize(request, binding, user, new Handler<Boolean>() {
            @Override
            public void handle(Boolean event) {
                if(event){
                    handler.handle(true);
                }else{
                    DirectoryResourcesProvider.isTeacherOf(request, user, handler);
                }
            }
        });
    }
}
