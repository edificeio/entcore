package org.entcore.common.http.filter;

import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.filter.Filter;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.user.UserUtils;

import java.util.Map;

public class TermsRevalidationFilter implements Filter {
    private final EventBus eventBus;

    public TermsRevalidationFilter(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    public void canAccess(HttpServerRequest request, Handler<Boolean> handler) {
        if (!"XMLHttpRequest".equals(request.headers().get("X-Requested-With"))) {
            UserUtils.getUserInfos(this.eventBus, request, userInfos -> {
                if (userInfos != null) {
                    boolean needRevalidateTerms = false;
                    Map<String, Object> otherProperties = userInfos.getOtherProperties();
                    if (otherProperties != null && otherProperties.get("needRevalidateTerms") != null) {
                        needRevalidateTerms = (Boolean) otherProperties.get("needRevalidateTerms");
                    }
                    if (needRevalidateTerms) {
                        handler.handle(false);
                    } else {
                        handler.handle(true);
                    }
                } else {
                    handler.handle(true);
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
