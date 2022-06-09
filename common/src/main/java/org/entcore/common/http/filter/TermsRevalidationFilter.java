package org.entcore.common.http.filter;

import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.filter.Filter;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.user.UserUtils;
import static org.entcore.common.user.SessionAttributes.*;

import java.util.Map;

public class TermsRevalidationFilter implements Filter {
    private final EventBus eventBus;

    public TermsRevalidationFilter(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    public void canAccess(HttpServerRequest request, Handler<Boolean> handler) {
        if (!"XMLHttpRequest".equals(request.headers().get("X-Requested-With"))
                && !request.headers().contains("Authorization")) {
            UserUtils.getUserInfos(this.eventBus, request, userInfos -> {
                if (userInfos != null) {
                    //check whether user has validate terms in current session
                    final Object needRevalidateTermsFromSession = userInfos.getAttribute(NEED_REVALIDATE_TERMS);
                    if (needRevalidateTermsFromSession != null) {
                        final Boolean needRevalidateTerms = Boolean.valueOf(needRevalidateTermsFromSession.toString());
                        handler.handle(!needRevalidateTerms);
                    } else {
                        //check whether he has validated previously
                        final Map<String, Object> otherProperties = userInfos.getOtherProperties();
                        if (otherProperties != null && otherProperties.get(NEED_REVALIDATE_TERMS) != null) {
                            handler.handle(!(Boolean) otherProperties.get(NEED_REVALIDATE_TERMS));
                        } else {
                            handler.handle(false);
                        }
                    }
                } else {
                    handler.handle(false);
                }
            });
        } else {
            handler.handle(true);
        }
    }

    @Override
    public void deny(HttpServerRequest request) {
        Renders.redirect(request, "/auth/revalidate-terms");
    }
}
