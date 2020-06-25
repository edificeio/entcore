package org.entcore.auth.services.impl;

import java.net.URI;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.entcore.auth.services.SafeRedirectionService;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;

import fr.wseduc.webutils.http.Renders;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class DefaultSafeRedirectionService implements SafeRedirectionService {
    static Logger logger = LoggerFactory.getLogger(DefaultSafeRedirectionService.class);
    private Set<String> defaultDomainsWhiteList = new HashSet<>();
    private Set<String> domainsWhiteList = new HashSet<>();
    private Set<String> internalHosts = new HashSet<>();
    private Future<Void> onReady = Future.future();
    private final Neo4j neo = Neo4j.getInstance();
    private boolean inited = false;

    public void init(Vertx vertx, JsonObject config) {
        if (inited)
            return;
        final long delay = config.getLong("delayInMinutes", 30l);
        final Map<String, String> skins = vertx.sharedData().getLocalMap("skins");
        internalHosts.addAll(skins.keySet());
        defaultDomainsWhiteList.addAll(config.getJsonArray("defaultDomains", new JsonArray()).stream()
                .map(String.class::cast).collect(Collectors.toSet()));
        loadDomains();
        vertx.setPeriodic(TimeUnit.MINUTES.toMillis(delay), event -> {
            loadDomains();
        });
        inited = true;
    }

    private void ensureInit() {
        if (!inited) {
            logger.error("SafeRedirectionService not inited");
        }
    }

    private Optional<String> extractHost(String address) {
        try {
            if (address.startsWith("http:") || address.startsWith("https:")) {
                // uri with scheme
                return Optional.ofNullable(URI.create(address).getHost());
            } else if (address.startsWith("/")) {
                // path only
                return Optional.empty();
            } else {
                // uri without scheme
                return Optional.ofNullable(URI.create("https://" + address).getHost());
            }
        } catch (Exception e) {
            logger.warn("Cannot parse host : " + address);
            return Optional.empty();
        }
    }

    private void loadDomains() {
        final String query = "MATCH (a:Application) WHERE HAS(a.address) RETURN a.address as address";
        neo.execute(query, new JsonObject(), Neo4jResult.validResultHandler(res -> {
            if (res.isRight()) {
                final Set<String> addresses = res.right().getValue().stream().map(e -> (JsonObject) e)
                        .map(e -> e.getString("address")).collect(Collectors.toSet());
                domainsWhiteList.clear();
                // ent hosts
                for (final String allowed : internalHosts) {
                    final Optional<String> extractedHost = extractHost(allowed);
                    if (extractedHost.isPresent())
                        domainsWhiteList.add(extractedHost.get());
                }
                // configurable hosts
                for (final String def : defaultDomainsWhiteList) {
                    final Optional<String> extractedHost = extractHost(def);
                    if (extractedHost.isPresent())
                        domainsWhiteList.add(extractedHost.get());
                }
                // application hosts
                for (final String addr : addresses) {
                    final Optional<String> extractedHost = extractHost(addr);
                    if (extractedHost.isPresent())
                        domainsWhiteList.add(extractedHost.get());
                }
                if (!onReady.isComplete()) {
                    onReady.complete();
                }
            } else {
                logger.error("Cannot get application addresses : ", res.left().getValue());
            }
        }));

    }

    public void canRedirectTo(String uri, Handler<Boolean> handler) {
        ensureInit();
        if (!onReady.isComplete()) {
            logger.warn("Trying to checkRedirect but whitelist is not loaded yet : " + uri);
        }
        onReady.setHandler(ready -> {
            if (ready.succeeded()) {
                final Optional<String> extractedHost = extractHost(uri);
                if (!extractedHost.isPresent()) {
                    logger.warn("Cannot parse destination uri : " + uri);
                    handler.handle(false);
                } else if (!domainsWhiteList.contains(extractedHost.get())) {
                    logger.warn("Redirection not authorized : " + uri);
                    handler.handle(false);
                } else {
                    handler.handle(true);
                }
            } else {
                logger.error("RedirectionService failed to init : ", ready.cause());
                handler.handle(false);
            }
        });
    }

    public void redirect(HttpServerRequest request, String path) {
        redirect(request, null, path, "/");
    }

    public void redirect(HttpServerRequest request, String host, String path) {
        redirect(request, host, path, "/");
    }

    public void redirect(HttpServerRequest request, String host, String path, String fallbackPath) {
        ensureInit();
        if (!onReady.isComplete()) {
            logger.warn("Trying to redirect but whitelist is not loaded yet : " + host + path);
        }
        onReady.setHandler(ready -> {
            if (host == null) {
                // missing host => ent host
                Renders.redirect(request, path);
            } else {
                // if not ent host
                final String uri = host + path;
                canRedirectTo(uri, res -> {
                    if (res) {
                        Renders.redirect(request, host, path);
                    } else {
                        Renders.redirect(request, fallbackPath);
                    }
                });
            }
        });
    }

}