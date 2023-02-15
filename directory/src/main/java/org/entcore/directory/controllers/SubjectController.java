package org.entcore.directory.controllers;

import fr.wseduc.rs.Delete;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.rs.Put;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.MfaProtected;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.BaseController;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import org.entcore.common.http.filter.AdminFilter;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.entcore.directory.services.SubjectService;

import static fr.wseduc.webutils.request.RequestUtils.bodyToJson;
import static org.entcore.common.http.response.DefaultResponseHandler.*;

public class SubjectController extends BaseController {

    private SubjectService subjectService;

    public void setSubjectService(SubjectService subjectService) {
        this.subjectService = subjectService;
    }


    @Get("/subject/admin/list")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    @ResourceFilter(AdminFilter.class)
    @MfaProtected()
    public void listAdmin(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(UserInfos user) {
                if (user != null) {
                    String structureId = request.params().get("structureId");
                    subjectService.getSubjects(structureId, arrayResponseHandler(request));
                } else {
                    unauthorized(request);
                }
            }
        });
    }

    @Post("/subject")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdminFilter.class)
    @MfaProtected()
    public void create(final HttpServerRequest request) {
        bodyToJson(request, pathPrefix + "createManualSubject", new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject body) {
                subjectService.createManual(body, notEmptyResponseHandler(request, 201));
            }
        });
    }

    @Put("/subject/:subjectId")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdminFilter.class)
    @MfaProtected()
    public void update(final HttpServerRequest request) {
        final String subjectId = request.params().get("subjectId");
        if (subjectId != null && !subjectId.trim().isEmpty()) {
            bodyToJson(request, pathPrefix + "updateManualSubject", new Handler<JsonObject>() {
                @Override
                public void handle(JsonObject body) {
                    body.put("id", subjectId);
                    subjectService.updateManual(body, notEmptyResponseHandler(request, 201));
                }
            });
        }
    }

    @Delete("/subject/:subjectId")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdminFilter.class)
    @MfaProtected()
    public void delete(final HttpServerRequest request) {
        final String subjectId = request.params().get("subjectId");
        if (subjectId != null && !subjectId.trim().isEmpty()) {
            subjectService.deleteManual(subjectId, defaultResponseHandler(request, 204));
        }
    }
}