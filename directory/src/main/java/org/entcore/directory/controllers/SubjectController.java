package org.entcore.directory.controllers;

import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.http.BaseController;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.entcore.directory.services.GroupService;
import org.entcore.directory.services.SubjectService;

import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.leftToResponse;

public class SubjectController extends BaseController {

    private SubjectService subjectService;

    public void setSubjectService(SubjectService subjectService) {
        this.subjectService = subjectService;
    }


    @Get("/subject/admin/list")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void listAdmin(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(UserInfos user) {
                if (user != null) {
                    String structureId = request.params().get("structureId");
                    subjectService.listAdmin(structureId, arrayResponseHandler(request));
                } else {
                    unauthorized(request);
                }
            }
        });
    }
}