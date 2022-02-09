package org.entcore.common.elasticsearch;

import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ElasticBulkBuilder {
    final Future<Buffer> body;
    final List<String> actions = new ArrayList<>();
    private final HttpClientRequest request;

    public ElasticBulkBuilder(final HttpClientRequest aReq, final Future<Buffer> body) {
        this.request = aReq;
        this.body = body;
    }

    private void doWrite(final Optional<JsonObject> documentOpt, final JsonObject metadata, final String action) {
        if (documentOpt.isPresent()) {
            final JsonObject document = documentOpt.get();
            final String buffer = (new JsonObject()).put(action, metadata).encode() + "\n" + document.encode() + "\n";
            this.request.write(buffer);
            //System.out.print(buffer);
        } else {
            final String buffer = (new JsonObject()).put(action, metadata).encode() + "\n";
            this.request.write(buffer);
            //System.out.print(buffer);

        }
    }

    public void index(final JsonObject document) {
        index(document, Optional.empty(), Optional.empty(), Optional.empty());
    }

    public void index(final JsonObject document, final String id) {
        index(document, Optional.ofNullable(id), Optional.empty(), Optional.empty());
    }

    public void index(final JsonObject document, final Optional<String> id, final Optional<String> index, final Optional<String> routing) {
        final JsonObject metadata = new JsonObject();
        if (id.isPresent()) {
            metadata.put("_id", id.get());
        } else if (document.containsKey("_id")) {
            metadata.put("_id", document.getString("_id"));
        }
        if (index.isPresent()) {
            metadata.put("_index", index.get());
        }
        if (routing.isPresent()) {
            metadata.put("routing", routing.get());
        }
        document.remove("_id");
        doWrite(Optional.of(document), metadata, "index");
        actions.add("index");
    }

    public void create(final JsonObject document) {
        create(document, Optional.empty(), Optional.empty(), Optional.empty());
    }

    public void create(final JsonObject document, final String id) {
        create(document, Optional.ofNullable(id), Optional.empty(), Optional.empty());
    }

    public void create(final JsonObject document, final Optional<String> id, final Optional<String> index, final Optional<String> routing) {
        final JsonObject metadata = new JsonObject();
        if (id.isPresent()) {
            metadata.put("_id", id.get());
        } else if (document.containsKey("_id")) {
            metadata.put("_id", document.getString("_id"));
        }
        if (index.isPresent()) {
            metadata.put("_index", index.get());
        }
        if (routing.isPresent()) {
            metadata.put("routing", routing.get());
        }
        document.remove("_id");
        doWrite(Optional.of(document), metadata, "create");
        actions.add("create");
    }

    public void update(final JsonObject document) {
        update(document, Optional.empty(), Optional.empty(), Optional.empty());
    }

    public void update(final JsonObject document, final String id) {
        update(document, Optional.ofNullable(id), Optional.empty(), Optional.empty());
    }

    public void update(final JsonObject document, final Optional<String> id, final Optional<String> index, final Optional<String> routing) {
        final JsonObject metadata = new JsonObject();
        if (id.isPresent()) {
            metadata.put("_id", id.get());
        } else if (document.containsKey("_id")) {
            metadata.put("_id", document.getString("_id"));
        }
        if (index.isPresent()) {
            metadata.put("_index", index.get());
        }
        if (routing.isPresent()) {
            metadata.put("routing", routing.get());
        }
        document.remove("_id");
        doWrite(Optional.of(new JsonObject().put("doc", document)), metadata, "update");
        actions.add("update");
    }

    public void upsert(final JsonObject insert, final JsonObject update) {
        upsert(insert, update, Optional.empty(), Optional.empty(), Optional.empty());
    }

    public void upsert(final JsonObject insert, final JsonObject update, final String id) {
        upsert(insert, update, Optional.ofNullable(id), Optional.empty(), Optional.empty());
    }

    public void upsert(final JsonObject insert, final JsonObject update, final Optional<String> id, final Optional<String> index, final Optional<String> routing) {
        final JsonObject metadata = new JsonObject();
        if (id.isPresent()) {
            metadata.put("_id", id.get());
        } else if (insert.containsKey("_id")) {
            metadata.put("_id", insert.getString("_id"));
        }
        if (index.isPresent()) {
            metadata.put("_index", index.get());
        }
        if (routing.isPresent()) {
            metadata.put("routing", routing.get());
        }
        insert.remove("_id");
        doWrite(Optional.of(new JsonObject().put("doc", update).put("upsert", insert)), metadata, "update");
        actions.add("update");
    }

    public void script(final JsonObject document) {
        script(document, Optional.empty(), Optional.empty(), Optional.empty());
    }

    public void script(final JsonObject document, final String id) {
        script(document, Optional.ofNullable(id), Optional.empty(), Optional.empty());
    }

    public void script(final JsonObject document, final Optional<String> id, final Optional<String> index, final Optional<String> routing) {
        final JsonObject metadata = new JsonObject();
        if (id.isPresent()) {
            metadata.put("_id", id.get());
        } else if (document.containsKey("_id")) {
            metadata.put("_id", document.getString("_id"));
        }
        if (index.isPresent()) {
            metadata.put("_index", index.get());
        }
        if (routing.isPresent()) {
            metadata.put("routing", routing.get());
        }
        document.remove("_id");
        doWrite(Optional.of(document), metadata, "update");
        actions.add("update");
    }

    public void delete(final String id) {
        delete(id, Optional.empty(), Optional.empty());
    }

    public void delete(final String id, final Optional<String> index, final Optional<String> routing) {
        final JsonObject metadata = new JsonObject();
        metadata.put("_id", id);
        if (index.isPresent()) {
            metadata.put("_index", index.get());
        }
        if (routing.isPresent()) {
            metadata.put("routing", routing.get());
        }
        doWrite(Optional.empty(), metadata, "delete");
        actions.add("delete");
    }

    public Future<List<ElasticBulkRequestResult>> end() {
        this.request.end();
        return this.body.map(buffer -> {
            final JsonObject response = new JsonObject(buffer.toString());
            final JsonArray items = response.getJsonArray("items");
            final List<ElasticBulkRequestResult> results = new ArrayList<>();
            for (int i = 0; i < items.size(); i++) {
                final JsonObject item = items.getJsonObject(i);
                final JsonObject realItem = item.getJsonObject(actions.get(i));
                final String id = realItem.getString("_id");
                final int status = realItem.getInteger("status");
                final boolean ok = status >= 200 && status <= 299;
                final String result = realItem.getString("result");
                final String error = realItem.getJsonObject("error", new JsonObject()).getString("reason");
                results.add(new ElasticBulkRequestResult(id, ok, result, error));
            }
            return results;
        });
    }

    public static class ElasticBulkRequestResult {
        private final boolean ok;
        private final String id;
        private final String message;
        private final String details;

        ElasticBulkRequestResult(final String id, final boolean ok, final String message, final String details) {
            this.ok = ok;
            this.id = id;
            this.message = message;
            this.details = details;
        }

        public String getDetails() {
            return details;
        }

        public String getId() {
            return id;
        }

        public boolean isOk() {
            return ok;
        }

        public String getMessage() {
            return message;
        }
    }
}
