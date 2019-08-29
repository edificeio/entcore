package org.entcore.admin.controllers;

import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Post;
import fr.wseduc.security.SecuredAction;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.mongodb.MongoDbControllerHelper;

public class BlockProfileTraceController extends MongoDbControllerHelper {

    public BlockProfileTraceController() {
        super("adminv2");
    }

    @Post("/trace")
    @ApiDoc("Add trace.")
    public void addTrace(HttpServerRequest request) {
        create(request);
    }
}
