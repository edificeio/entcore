package org.entcore.directory.controllers;

import fr.wseduc.rs.Delete;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.BaseController;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import org.entcore.common.user.UserUtils;
import org.entcore.directory.dto.LinkDTO;
import org.entcore.directory.services.UserLinkService;
import org.entcore.directory.services.UserLinkService.CreateLinkResult;
import org.entcore.directory.services.UserLinkService.DeleteLinkResult;
import org.entcore.directory.util.UserLinkValidator;

import java.util.UUID;

import static fr.wseduc.webutils.request.RequestUtils.bodyToClass;

public class UserLinkController extends BaseController {

    /** Cle d'erreur renvoyee quand l'utilisateur a atteint son quota de liens. */
    private static final String LINK_LIMIT_REACHED = "directory.user.link.limit.reached";

    /** Cle d'erreur renvoyee quand le lien a supprimer est inconnu ou appartient a un autre utilisateur. */
    private static final String LINK_NOT_FOUND = "directory.user.link.not.found";

    private UserLinkService userLinkService;
    private EventBus eb;

    public UserLinkController(EventBus eb, UserLinkService linkService) {
        this.userLinkService = linkService;
        this.eb = eb;
    }

    @Post("/user-links")
    @SecuredAction(value = "auth.user.info", type = ActionType.AUTHENTICATED)
    public void addLink(final HttpServerRequest request) {
        UserUtils.getAuthenticatedUserInfos(eb, request)
                .onSuccess(  user -> {
            bodyToClass(request, LinkDTO.class)
                .onSuccess(link -> {
                    if (link == null) {
                        badRequest(request, "Request body can't be null");
                        return;
                    }
                    final String validationError = UserLinkValidator.validatePayload(link);
                    if (validationError != null) {
                        badRequest(request, validationError);
                        return;
                    }
                    userLinkService.createLink(link, user.getUserId())
                            .onSuccess(result -> {
                                if (result == CreateLinkResult.LIMIT_REACHED) {
                                    renderError(request,
                                            new JsonObject().put("error", LINK_LIMIT_REACHED),
                                            409,
                                            LINK_LIMIT_REACHED);
                                } else {
                                    ok(request);
                                }
                            })
                            .onFailure(t -> {
                                log.error("An error occured during add user link", t);
                                renderError(request);
                            });
                })
                .onFailure(t -> log.error("Error while decoding request body", t));
        }).onFailure( t ->  unauthorized(request));
    }

    @Get("/user-links")
    @SecuredAction(value = "auth.user.info", type = ActionType.AUTHENTICATED)
    public void getUserLinks(HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            if (user == null) {
                unauthorized(request);
                return;
            }
            userLinkService.getLinks(user.getUserId())
                    .onSuccess( links -> render(request, links))
                    .onFailure(t -> {
                        log.error("An error occured during get user link", t);
                        renderError(request);
                    });
        });
    }

    @Delete("/user-links/:id")
    @SecuredAction(value = "auth.user.info", type = ActionType.AUTHENTICATED)
    public void deleteUserLink(HttpServerRequest request) {
        final UUID linkId;
        try {
            linkId = UUID.fromString(request.params().get("id"));
        } catch( Exception e) {
            badRequest(request, "path params should be an UUID");
            return;
        }
        UserUtils.getUserInfos(eb, request, user -> {
            if (user == null) {
                unauthorized(request);
                return;
            }
            userLinkService.deleteLink(linkId, user.getUserId())
                    .onSuccess( result -> {
                        if (result == DeleteLinkResult.NOT_FOUND) {
                            renderError(request,
                                    new JsonObject().put("error", LINK_NOT_FOUND),
                                    400,
                                    LINK_NOT_FOUND);
                        } else {
                            ok(request);
                        }
                    })
                    .onFailure(t -> {
                        log.error("An error occured during delete user link", t);
                        renderError(request);
                    });
        });
    }

}
