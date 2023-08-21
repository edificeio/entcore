package org.entcore.directory;

import fr.wseduc.webutils.security.SecureHttpServerRequest;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.entcore.directory.security.UserbookCsrfFilter;
import org.entcore.test.TestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

@RunWith(VertxUnitRunner.class)
public class UserbookCsrfFilterTest {
    private static final UserbookCsrfFilter filter = new UserbookCsrfFilter(
            TestHelper.helper().vertx().eventBus(),
            Collections.emptySet()
    ) {
        @Override
        protected void compareToken(final HttpServerRequest request, final Handler<Boolean> handler) {
            super.compareToken(request, handler);
        }
    };
    /**
     * Userbook requests coming from the mobile app should not be filtered.
     */
    @Test
    public void testUserBookRequestFromMobileAppNotFiltered(final TestContext context) {
        final Async async = context.async();
        filter.canAccess(new DummySecuredRequest("test", "/userbook/api/edit", HttpMethod.GET),
                e -> {
            context.assertTrue(e, "Should have allowed a call with a client_id pass");
            async.complete();
        });
    }
    /**
     * Userbook requests coming from the Web should be filtered.
     */
    @Test
    public void testUserBookRequestFromWebShouldBeFiltered(final TestContext context) {
        final Async async = context.async();
        AtomicBoolean hasComparisonMethodBeenCalled = new AtomicBoolean(false);
        final UserbookCsrfFilter filter = new UserbookCsrfFilter(
                TestHelper.helper().vertx().eventBus(),
                Collections.emptySet()
        ) {
            @Override
            protected void compareToken(final HttpServerRequest request, final Handler<Boolean> handler) {
                hasComparisonMethodBeenCalled.set(true);
                handler.handle(false);
            }
        };
        filter.canAccess(new DummySecuredRequest(null, "/userbook/api/edit", HttpMethod.GET),
                e -> {
                    context.assertTrue(hasComparisonMethodBeenCalled.get(), "Should have compare tokens");
                    async.complete();
                });
    }

    private static class DummySecuredRequest extends SecureHttpServerRequest {
        private final String clientId;
        private final String path;
        private final HttpMethod method;
        public DummySecuredRequest(final String clientId, final String path,
                                   final HttpMethod method) {
            super(null);
            this.clientId = clientId;
            this.path = path;
            this.method = method;
        }
        public HttpMethod method() {return method;}

        @Override
        public String getAttribute(final String attr) {
            if("client_id".equals(attr)) {
                return clientId;
            }
            return null;
        }

        @Override
        public String uri() {
            return path;
        }
    }
}
