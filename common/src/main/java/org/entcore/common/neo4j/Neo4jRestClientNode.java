package org.entcore.common.neo4j;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.utils.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

public class Neo4jRestClientNode {
    public enum Type {
        Master, Slave, Any
    }

    private static final Logger logger = LoggerFactory.getLogger(Neo4jRestClientNode.class);
    private final String url;
    private final HttpClient httpClient;
    private final long banDurationSecond;
    private Type type;
    private Boolean available;
    private Boolean readable;
    private LocalDateTime notAvailableFrom;
    private LocalDateTime notReadableFrom;
    private String moduleName = "";

    Neo4jRestClientNode(final String url, final HttpClient httpClient, final long banDurationSecond) {
        this.url = url;
        this.httpClient = httpClient;
        this.banDurationSecond = banDurationSecond;
    }

    public Neo4jRestClientNode(final String url, final Vertx vertx, final long banDurationSecond) throws URISyntaxException {
        final URI uri = new URI(url);
        final HttpClientOptions options = new HttpClientOptions()
                .setDefaultHost(uri.getHost())
                .setDefaultPort(uri.getPort())
                .setKeepAlive(true);
        final HttpClient httpClient = vertx.createHttpClient(options);
        this.url = url;
        this.httpClient = httpClient;
        this.banDurationSecond = banDurationSecond;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public boolean isBanned() {
        final LocalDateTime now = LocalDateTime.now();
        if (this.notAvailableFrom != null) {
            final long diff = ChronoUnit.SECONDS.between(notAvailableFrom, now);
            return Math.abs(diff) < banDurationSecond;
        }
        if (this.notReadableFrom != null) {
            final long diff = ChronoUnit.SECONDS.between(notReadableFrom, now);
            return Math.abs(diff) < banDurationSecond;
        }
        return false;
    }

    public boolean isMaster() {
        return Type.Master.equals(type) || Type.Any.equals(type);
    }

    public boolean isSlave() {
        return Type.Slave.equals(type) || Type.Any.equals(type);
    }

    public boolean isAvailable() {
        return Boolean.TRUE.equals(available) && Boolean.TRUE.equals(readable);
    }

    public boolean isMasterAvailable() {
        return isMaster() && isAvailable();
    }

    public boolean isSlaveAvailable() {
        return isSlave() && isAvailable();
    }

    public String getUrl() {
        return url;
    }

    public HttpClient getHttpClient() {
        return httpClient;
    }

    public Boolean getAvailable() {
        return available;
    }

    public Neo4jRestClientNode setSlave() {
        //change
        final boolean changed = this.type != Type.Slave;
        this.type = Type.Slave;
        this.setAvailable(true);
        if (changed && logger.isDebugEnabled()) {
            logger.debug("Neo4j node became slave (" + this.url + ") for module:" + moduleName);
        }
        return this;
    }

    public Neo4jRestClientNode setMaster() {
        //change
        final boolean changed = this.type != Type.Master;
        this.type = Type.Master;
        this.setAvailable(true);
        if (changed && logger.isDebugEnabled()) {
            logger.debug("Neo4j node became master (" + this.url + ") for module:" + moduleName);
        }
        return this;
    }

    public Neo4jRestClientNode setAnyType() {
        //change
        final boolean changed = this.type != Type.Any;
        this.type = Type.Any;
        this.setAvailable(true);
        if (changed && logger.isDebugEnabled()) {
            logger.debug("Neo4j node became master/slave (" + this.url + ") for module:" + moduleName);
        }
        return this;
    }

    public Neo4jRestClientNode setAvailable(final Boolean available) {
        final boolean changed = available != null && !available.equals(this.available);
        this.available = available;
        //if change keep date
        if (changed) {
            if (Boolean.FALSE.equals(available)) {
                notAvailableFrom = LocalDateTime.now();
                logger.warn("Neo4j node became unavailable (" + this.url + ") for module:" + moduleName);
            } else {
                logger.warn("Neo4j node became available (" + this.url + ") for module:" + moduleName);
                notAvailableFrom = null;
            }
        }
        return this;
    }

    public Neo4jRestClientNode setReadable(Boolean readable) {
        final boolean changed = readable != null && !readable.equals(this.readable);
        this.readable = readable;
        if (changed) {
            if (Boolean.FALSE.equals(readable)) {
                notReadableFrom = LocalDateTime.now();
                logger.warn("Neo4j node became unreadable (" + this.url + ") for module:" + moduleName);
            } else {
                logger.warn("Neo4j node became readable (" + this.url + ") for module:" + moduleName);
                notReadableFrom = null;
            }
        }
        return this;
    }

    public void close() {
        this.httpClient.close();
    }

    public Type getType() {
        return type;
    }

    public String getTypeName() {
        return type != null ? type.name() : "null";
    }

    public void setNotAvailableFrom(LocalDateTime notAvailableFrom) {
        this.notAvailableFrom = notAvailableFrom;
    }

    public void setNotReadableFrom(LocalDateTime notReadableFrom) {
        this.notReadableFrom = notReadableFrom;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Boolean getReadable() {
        return readable;
    }

    public LocalDateTime getNotAvailableFrom() {
        return notAvailableFrom;
    }

    public LocalDateTime getNotReadableFrom() {
        return notReadableFrom;
    }

    public long getBanDurationSecond() {
        return banDurationSecond;
    }

    public void execute(final String query, final Optional<String> authorization, final Handler<Buffer> bodyHandler, final Handler<Throwable> handlerError) {
        final HttpClient client = getHttpClient();
        client.request(HttpMethod.POST, "/db/data/cypher")
        .map(req -> {
            req.headers().add("Content-Type", "application/json").add("Accept", "application/json; charset=UTF-8");
          authorization.ifPresent(s -> prepareRequest(s, req));
            return req;
        })
        .flatMap(req -> req.send(new JsonObject().put("query", query).encode()))
        .onSuccess(resp -> {
            resp.bodyHandler(bodyHandler);
        }).onFailure(handlerError);
    }

    private HttpClientRequest prepareRequest(final String authorizationHeader, final HttpClientRequest request) {
        if (!StringUtils.isEmpty(authorizationHeader)) {
            request.headers().add("Authorization", authorizationHeader);
        }
        return request;
    }
}
