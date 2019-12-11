package org.entcore.admin.controllers;

import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.admin.services.BlockProfileTraceService;
import org.entcore.common.http.filter.AdminFilter;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.mongodb.MongoDbControllerHelper;

import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;

public class BlockProfileTraceController extends MongoDbControllerHelper {
    private BlockProfileTraceService blockProfileTraceService;

    public BlockProfileTraceController(String collection) {
        super(collection);
    }

    @Post("/api/block/trace")
    @ApiDoc("Add a Profile blocking trace.")
    @SecuredAction(type = ActionType.RESOURCE, value = "")
    @ResourceFilter(AdminFilter.class)
    public void addTrace(HttpServerRequest request) {
        create(request);
    }

    @Get("/api/block/traces/:structureId")
    @ApiDoc("Get Profile blocking traces.")
    @SecuredAction(type = ActionType.RESOURCE, value = "")
    @ResourceFilter(AdminFilter.class)
    public void getTraces(HttpServerRequest request) {
        final String structureId = request.params().get("structureId");
        if (structureId == null) {
            badRequest(request, "structureId is missing");
            return;
        }
        this.blockProfileTraceService.listByStructureId(structureId, arrayResponseHandler(request));
    }

    public void setBlockProfileTraceService(BlockProfileTraceService blockProfileTraceService) {
        this.blockProfileTraceService = blockProfileTraceService;
    }
}
