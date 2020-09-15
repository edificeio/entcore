package org.entcore.cas.mapping;

import fr.wseduc.mongodb.MongoDb;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class MappingService {
    private static MappingService instance = new MappingService();
    public static final String COLLECTION = "casMapping";
    private final MongoDb mongoDb = MongoDb.getInstance();
    private Future<Mappings> cache;

    private MappingService(){ }

    public static MappingService getInstance(){
        return instance;
    }

    public void reset(){
        cache = null;
    }

    public Future<Void> create(JsonObject data){
        final Future<Void> future = Future.future();
        if(data.containsKey("type")){
            data.put("_id", data.getString("type"));
            data.remove("type");
        }
        mongoDb.insert(COLLECTION, data, r->{
            if ("ok".equals(r.body().getString("status"))) {
                future.complete();
            } else{
                future.fail(r.body().getString("message"));
            }
        });
        return future;
    }

    public Future<Mappings> getMappings() {
        if (cache == null) {
            cache = Future.future();
            final Map<String, Mapping> rows = new HashMap<>();
            //final String mappingJson = new String(Files.readAllBytes(Paths.get(RegisteredServices.class.getResource(MAPPING_FILE).toURI())));
            //final JsonObject mappingJsonObject = new JsonObject(mappingJson);
            mongoDb.find(COLLECTION, new JsonObject(),r->{
                final JsonArray results = r.body().getJsonArray("results");
                if ("ok".equals(r.body().getString("status")) && results != null) {
                    for (final Object row : results) {
                        final JsonObject info = (JsonObject)row;
                        final String key = info.getString("_id");
                        final Mapping mapping = new Mapping(key, info.getString("casType"), info.getString("pattern"));
                        rows.put(key, mapping);
                    }
                    cache.complete(new Mappings(rows));
                } else {
                    cache.fail(r.body().getString("message"));
                }
            });
        }
        return cache;
    }

    public static class Mappings{
        private final Map<String, Mapping> rowsByType;
        private Mappings(Map<String, Mapping> rows) {
            this.rowsByType = rows;
        }

        public JsonArray toJson(){
            final JsonArray all = new JsonArray();
            for(final Mapping mapping : rowsByType.values()){
                all.add(new JsonObject().put("casType", mapping.getCasType()).put("pattern", mapping.getPattern()).put("type", mapping.getType()));
            }
            return all;
        }

        public Optional<Mapping> find(String casType, String pattern){
            for(Mapping mapping: rowsByType.values()){
                if(mapping.getCasType().equals(casType) && mapping.getPattern().equals(pattern)){
                    return Optional.of(mapping);
                }
            }
            return Optional.empty();
        }

    }
}
