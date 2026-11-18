package org.entcore.workspace.controllers;

import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.MfaProtected;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.BaseController;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.http.filter.SuperAdminFilter;
import org.entcore.workspace.service.TransferService;

public class TransferController extends BaseController {

    private static final Logger log = LoggerFactory.getLogger(TransferController.class);

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    /**
     * CM2 documents zip of a 6th grade student, called by a partner ENT.
     * Basic auth clients need TransferController|getArchive in their scope; user sessions are limited to ADMC with MFA.
     */
    @Get("/transfer/:ine")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(SuperAdminFilter.class)
    @MfaProtected()
    public void getArchive(final HttpServerRequest request) {
        final String ine = request.params().get("ine");
        log.info("[Workspace.transfer] Archive requested for ine " + ine);
        transferService.sendArchive(ine, request).onFailure(error -> {
            if (error instanceof IllegalArgumentException) {
                badRequest(request, error.getMessage());
            } else {
                log.error("[Workspace.transfer] Error sending archive for ine " + ine, error);
                renderError(request, new JsonObject().put("error", "Transfer service unavailable"));
            }
        });
    }
}
