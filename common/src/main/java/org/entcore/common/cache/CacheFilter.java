package org.entcore.common.cache;

import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.Utils;
import fr.wseduc.webutils.http.Binding;
import fr.wseduc.webutils.request.HttpServerRequestWithBuffering;
import fr.wseduc.webutils.request.filter.Filter;
import fr.wseduc.webutils.security.SecureHttpServerRequest;
import fr.wseduc.webutils.security.WrappedHttpServerRequest;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.http.filter.CsrfFilter;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.entcore.common.utils.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.regex.Matcher;

public class CacheFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(CacheFilter.class);
    private final Set<Binding> bindings;
    private final CacheService cacheService;
    private final EventBus eb;
    private Map<String,JsonObject> cacheConfig;
    private final Set<Binding> cachedBindings=new HashSet<>();

    public CacheFilter(EventBus eb, Set<Binding> bindings, CacheService cacheService) {
        this.eb = eb;
        this.bindings = bindings;
        this.cacheService = cacheService;
    }

    private void loadCacheConfig() {
        cacheConfig = new HashMap<>();
        InputStream is = CacheFilter.class.getClassLoader().getResourceAsStream(Cache.class.getSimpleName() + ".json");
        if (is != null) {
            BufferedReader r = null;
            try {
                r = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                String line;
                while((line = r.readLine()) != null) {
                    final JsonObject cache = new JsonObject(line);
                    final String method = cache.getString("method", "");
                    if(!StringUtils.isEmpty(method)){
                        for (Binding binding : bindings) {
                            if (binding != null && method.equals(binding.getServiceMethod())) {
                                cachedBindings.add(binding);
                                cacheConfig.put(method, cache);
                                break;
                            }
                        }
                    }
                }
            } catch (IOException | DecodeException e) {
                log.error("Unable to load cacheCOnfig", e);
            } finally {
                if (r != null) {
                    try {
                        r.close();
                    } catch (IOException e) {
                        log.error("Close inputstream error", e);
                    }
                }
            }
        }
    }

    private Binding requestBinding(HttpServerRequest request) {
        for (Binding binding: cachedBindings) {
            if (!request.method().name().equals(binding.getMethod().name())) {
                continue;
            }
            Matcher m = binding.getUriPattern().matcher(request.path());
            if (m.matches()) {
                return binding;
            }
        }
        return null;
    }

    private void saveToCache(String key, String value, CacheScope cacheScope, Integer ttl, HttpServerRequest request){
        switch(cacheScope){
            case GLOBAL:
                cacheService.upsert(key,value, ttl, res->{
                    if(res.succeeded()){
                        log.debug("[CacheFilter.saveToCache] response saved to global cache key="+key);
                    }else{
                        log.error("[CacheFilter.saveToCache] failed to save into global cache key="+key, res.cause());
                    }
                });
                break;
            case LANG:
                UserUtils.getUserInfos(eb,request, resUser -> {
                    if (resUser == null) {
                        return;
                    }
                    final String lang = Utils.getOrElse(I18n.acceptLanguage(request), "fr");
                    cacheService.upsertForLang(lang, key, value, ttl, res -> {
                        if (res.succeeded()) {
                            log.debug("[CacheFilter.saveToCache] response saved to lang cache key=" + key);
                        } else {
                            log.error("[CacheFilter.saveToCache] failed to save into lang cache key=" + key, res.cause());
                        }
                    });
                });
                break;
            case USER:
                UserUtils.getUserInfos(eb,request, resUser -> {
                    if (resUser == null) {
                        return;
                    }
                    cacheService.upsertForUser(resUser, key, value, ttl, res -> {
                        if (res.succeeded()) {
                            log.debug("[CacheFilter.saveToCache] response saved to user cache key=" + key);
                        } else {
                            log.error("[CacheFilter.saveToCache] failed to save into user cache key=" + key, res.cause());
                        }
                    });
                });
                break;
        }
    }

    private String generateKeyWithQuery(HttpServerRequest request, boolean userQuery, String key){
        if(userQuery){
            final MultiMap params = request.params();
            if(params.isEmpty()){
                return key;
            }else{
                final List<String> keys = new ArrayList<>(params.names());
                keys.sort(Comparator.comparing(String::toString));
                final List<String> parts = new ArrayList<>();
                for(String k : keys){
                    final List<String> values = new ArrayList<>(request.params().getAll(k));
                    if(!values.isEmpty()){
                        values.sort(Comparator.comparing(String::toString));
                        parts.add(k+"="+String.join(",",values));
                    }
                }
                if(parts.isEmpty()){
                    return key;
                }else{
                    return key + "?"+String.join("&", parts);
                }
            }
        }else{
            return key;
        }
    }

    private void cache(final Binding binding, final HttpServerRequest request, final Handler<Boolean> originalHandler){
        request.pause();
        final Handler<Boolean> handler = (res)->{
            request.resume();
            originalHandler.handle(res);
        };
        final JsonObject config = cacheConfig.get(binding.getServiceMethod());
        final String originalKey = config.getString("key", binding.getServiceMethod());
        final String scope = config.getString("scope");
        final Integer ttlMinutes = config.getInteger("ttl", -1);
        final Integer ttl = ttlMinutes * 60;
        final CacheOperation operation = CacheOperation.valueOf(config.getString("operation", "CACHE"));
        final CacheScope cacheScope = CacheScope.valueOf(scope);
        final boolean usePath = config.getBoolean("usePath", false);
        final boolean useQueryParams = config.getBoolean("useQueryParams", false);
        final String realKey = generateKeyWithQuery(request, useQueryParams,usePath?request.path():originalKey);
        if (CacheOperation.CACHE.equals(operation)) {
            //=== get or create from cache
            final Future<Optional<String>> result = Future.future();
            switch(cacheScope){
                case GLOBAL:
                    cacheService.get(realKey, resCache -> {
                        if(resCache.succeeded()){
                            result.complete(resCache.result());
                        }else{
                            result.fail(resCache.cause());
                        }
                    });
                    break;
                case LANG:
                    UserUtils.getUserInfos(eb,request, resUser -> {
                        if(resUser == null){
                            result.fail("User not found");
                            return;
                        }
                        final String lang = Utils.getOrElse(I18n.acceptLanguage(request), "fr");
                        cacheService.getForLang(lang, realKey, resCache ->{
                            if(resCache.succeeded()){
                                result.complete(resCache.result());
                            }else{
                                result.fail(resCache.cause());
                            }
                        });
                    });
                    break;
                case USER:
                    UserUtils.getUserInfos(eb,request, resUser -> {
                        if(resUser == null){
                            result.fail("User not found");
                            return;
                        }
                        cacheService.getForUser(resUser, realKey, resCache -> {
                            if(resCache.succeeded()){
                                result.complete(resCache.result());
                            }else{
                                result.fail(resCache.cause());
                            }
                        });
                    });
                    break;
            }
            //=== after get => send response if founded
            result.setHandler(res -> {
                if(res.succeeded() && res.result().isPresent()){
                    request.response().end(res.result().get());
                }else{
                    //=== continue and save response into cache
                    if(request instanceof HttpServerRequestWithBuffering){
                        final HttpServerRequestWithBuffering tmp = ((HttpServerRequestWithBuffering)request);
                        tmp.enableResponseBuffering();
                        request.response().endHandler(resEnd -> {
                            final Optional<Buffer> buffer = tmp.getBodyResponseBuffered();
                            if(buffer.isPresent()){
                                saveToCache(realKey, buffer.get().toString(),cacheScope,ttl,request);
                            }else{
                                log.warn("[CacheFilter].cache Could not get buffer response: " + tmp);
                            }
                        });
                    } else {
                        log.warn("[CacheFilter].cache Request is not bufferable: " + request);
                    }
                    //TODO add param 'useQueryParams'
                    handler.handle(true);
                }
            });
        } else {
            //=== invalidate cache
            switch(cacheScope){
                case GLOBAL:
                    cacheService.remove(realKey, resCache->{});
                    break;
                case LANG:
                    UserUtils.getUserInfos(eb,request, resUser -> {
                        if (resUser == null) { return; }
                        final String lang = Utils.getOrElse(I18n.acceptLanguage(request), "fr");
                        cacheService.removeForLang(lang, realKey, resCache -> { });
                    });
                    break;
                case USER:
                    UserUtils.getUserInfos(eb,request, resUser -> {
                        if (resUser == null) { return; }
                        cacheService.removeForUser(resUser, realKey, resCache -> { });
                    });
                    break;
            }
            //=== then continue
            handler.handle(true);
        }
    }

    @Override
    public void canAccess(final HttpServerRequest request, final Handler<Boolean> handler) {
        if (cacheConfig == null) {
            loadCacheConfig();
        }
        final Binding binding = requestBinding(request);
        if (binding !=null && cacheConfig.containsKey(binding.getServiceMethod())) {
            cache(binding, request, handler);
        } else {
            handler.handle(true);
        }
    }

    @Override
    public void deny(HttpServerRequest httpServerRequest) {}
}
