package org.entcore.common.http.filter;

import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.filter.Filter;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;

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
                    Object needRevalidateTermsFromSession = userInfos.getAttribute("needRevalidateTerms");
                    if (needRevalidateTermsFromSession != null) {
                        Boolean needRevalidateTerms = Boolean.valueOf((String) needRevalidateTermsFromSession);
                        handler.handle(!needRevalidateTerms);
                    } else {
                        Map<String, Object> otherProperties = userInfos.getOtherProperties();
                        if (otherProperties != null && otherProperties.get("needRevalidateTerms") != null) {
                            handler.handle(!(Boolean) otherProperties.get("needRevalidateTerms"));
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
