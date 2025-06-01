package org.entcore.common.http.filter;

import fr.wseduc.webutils.Utils;
import fr.wseduc.webutils.http.Binding;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.CookieHelper;
import fr.wseduc.webutils.request.filter.Filter;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import org.entcore.common.utils.StringUtils;

public class WebviewFilter implements Filter {
    private static long COOKIE_TTL_DEFAULT = 24 * 60 * 60;
    private static final String XML_HTTP_REQUEST = "XMLHttpRequest";
    private static final String HEADER_REQUESTED_WITH = "X-Requested-With";
    private static final String COOKIE_WEBVIEW_DETECTED = "webviewdetected";
    private static final String COOKIE_WEBVIEW_SECURE = "webviewsecure";
    private static final String COOKIE_WEBVIEW_IGNORED = "webviewignored";
    private static final String COOKIE_WEBVIEW_LOCATION = "webviewlocation";
    private static final Logger log = LoggerFactory.getLogger(CsrfFilter.class);
    private final EventBus eb;
    private final Vertx vertx;
    private JsonObject config;

    public WebviewFilter(Vertx vertx, EventBus eb) {
        this.eb = eb;
        this.vertx = vertx;
        vertx.sharedData().<String, String>getAsyncMap("server")
            .compose(serverMap -> serverMap.get("webviewConfig"))
			.onSuccess(webviewConfig -> {
				if (webviewConfig != null) {
                    this.config = new JsonObject(webviewConfig);
                }else{
                    this.config = new JsonObject();
                }
            }).onFailure(ex -> log.error("Error when get config of WebviewFilter", ex));
    }

    protected String getIllegalWebpage(){
        return this.config.getString("illegal-page", "/illegal-app.html");
    }

    protected boolean isEnabled(){
        return this.config.getBoolean("enabled", false);
    }

    protected Long getCookieTtl(){
        return this.config.getLong("cookie-ttl", COOKIE_TTL_DEFAULT);
    }

    protected boolean isWhitelist(final String header){
        if(StringUtils.isEmpty(header) || XML_HTTP_REQUEST.equalsIgnoreCase(header)){
            return true;
        }
        final JsonArray whitelist = this.config.getJsonArray("whitelist", new JsonArray());
        if(whitelist.isEmpty()){
            return true;
        }
        for(final Object o : whitelist){
            if(o.toString().equalsIgnoreCase(header)){
                return true;
            }
        }
        return false;
    }

    protected boolean isBlackList(final String header){
        if(StringUtils.isEmpty(header) || XML_HTTP_REQUEST.equalsIgnoreCase(header)){
            return false;
        }
        final JsonArray blacklist = this.config.getJsonArray("blacklist", new JsonArray());
        if(blacklist.isEmpty()){
            return false;
        }
        for(final Object o : blacklist){
            if(o.toString().equalsIgnoreCase(header)){
                return true;
            }
        }
        return false;
    }

    @Override
    public void canAccess(HttpServerRequest request, Handler<Boolean> handler) {
        if(!isEnabled()){
            CookieHelper.getInstance().setSigned(COOKIE_WEBVIEW_IGNORED, "true",getCookieTtl(), request, false);
            handler.handle(true);
            return;
        }
        final String ua = request.headers().get("User-Agent");
        final String xRequestedWith = request.getHeader(HEADER_REQUESTED_WITH);
        final String detected = CookieHelper.get(COOKIE_WEBVIEW_DETECTED, request);
        final String secure = CookieHelper.getInstance().getSigned(COOKIE_WEBVIEW_SECURE, request);
        final boolean isWebview = "true".equals(detected);
        if("true".equals(secure)) {//is safe cookie secure
            handler.handle(true);
        } else if(!StringUtils.isEmpty(xRequestedWith)){//authorized webview
            if(isWhitelist(xRequestedWith) && !isBlackList(xRequestedWith)){
                CookieHelper.getInstance().setSigned(COOKIE_WEBVIEW_SECURE, "true",getCookieTtl(), request, false);
                handler.handle(true);
            } else { //unauthorized webview
                handler.handle(false);
                log.warn("Unauthorized webview : xRequestedWith="+xRequestedWith+ " isWebview="+isWebview+ " UserAgent:"+ua);
            }
        } else if(isWebview){//unidentified webview
            CookieHelper.getInstance().setSigned(COOKIE_WEBVIEW_LOCATION, getIllegalWebpage(), getCookieTtl(), request, false);
            handler.handle(false);
            log.warn("Unidentified webview : xRequestedWith="+xRequestedWith+ " isWebview="+isWebview+ " UserAgent:"+ua);
        } else {//not in wevbiew
            handler.handle(true);
        }
    }

    @Override
    public void deny(HttpServerRequest httpServerRequest) {
        Renders.redirect(httpServerRequest, getIllegalWebpage());
    }
}
