package org.entcore.cas.mapping;

import fr.wseduc.mongodb.MongoDb;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.AsyncResult;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;

import java.util.*;

public class MappingService {
    private boolean splitByStructure = true;
    private static final MappingService instance = new MappingService();
    public static final String COLLECTION = "casMapping";
    private final MongoDb mongoDb = MongoDb.getInstance();
    private final Neo4j neo = Neo4j.getInstance();
    private Date cacheMappingDate;
    private Date cacheStructuresDate;
    private Future<Mappings> cacheMapping;
    private final List<Promise<Mappings>> cacheMappingPending = new ArrayList<>();
    private Promise<JsonArray> cacheStructures;
    private final List<Promise<JsonArray>> cacheStructuresPending = new ArrayList<>();
    private MappingService(){ }

    public static MappingService getInstance(){
        return instance;
    }

    public Date getCacheMappingDate() {
        return cacheMappingDate;
    }

    public Date getCacheStructuresDate() {
        return cacheStructuresDate;
    }

    public boolean isSplitByStructure() {
        return splitByStructure;
    }

    public void configure(JsonObject config){
        if(config != null){
            this.splitByStructure = config.getBoolean("enableStructureSplit", true);
        }
    }

    public void reset(){
        cacheMapping = null;
        cacheStructures = null;
    }

    public Future<Void> create(JsonObject data){
        final Promise<Void> future = Promise.promise();
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
        return future.future();
    }

    public Future<Mappings> getMappings() {
        if (cacheMapping == null) {
            final Promise<Mappings> futureMapping = Promise.promise();
            //load mappings
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
                    futureMapping.complete(new Mappings(rows));
                } else {
                    futureMapping.fail(r.body().getString("message"));
                }
            });
            //load structures
            cacheMapping = futureMapping.future().compose(r->{
                final Mappings tmp = r;
                final Promise<Mappings> future = Promise.promise();
                getStructures().onComplete(resStruct -> {
                    if(resStruct.succeeded()){
                        for(final Object o : resStruct.result()){
                            final JsonObject json = (JsonObject)o;
                            final String id = json.getString("id");
                            final JsonArray parents = json.getJsonArray("parents", new JsonArray());
                            for(final Object parent : parents){
                                final JsonObject parentJ = (JsonObject)parent;
                                final String parentId = parentJ.getString("id");
                                tmp.addStructure(id, parentId);
                            }
                        }
                        tmp.computeHierarchy();
                        future.complete(tmp);
                    }else{
                        future.fail(resStruct.cause());
                    }
                });
                return future.future();
            });
            cacheMapping.onComplete(r -> {
                for(final Promise<Mappings> f : cacheMappingPending){
                    if(r.succeeded()) {
                        f.tryComplete(r.result());
                    } else {
                        f.tryFail(r.cause());
                    }
                }
                cacheMappingPending.clear();
                cacheMappingDate = new Date();
            });
            final Promise<Mappings> future = Promise.promise();
            cacheMappingPending.add(future);
            return future.future();
        } else if(cacheMapping.isComplete()){
            if(cacheMapping.succeeded()){
                return Future.succeededFuture(cacheMapping.result());
            } else {
                return Future.failedFuture(cacheMapping.cause());
            }
        }else {//pending
            final Promise<Mappings> future = Promise.promise();
            cacheMappingPending.add(future);
            return future.future();
        }
    }

    public Future<JsonArray> getStructures(){
        if(cacheStructures==null){//init
            cacheStructures = Promise.promise();
            String query =
                "MATCH (s:Structure) OPTIONAL MATCH (s)-[r:HAS_ATTACHMENT]->(ps:Structure) " +
                "WITH s, COLLECT({id: ps.id, name: ps.name}) as parents " +
                "RETURN s.id as id , CASE WHEN any(p in parents where p <> {id: null, name: null}) THEN parents END as parents ";
            neo.execute(query, new JsonObject(), Neo4jResult.validResultHandler(r->{
                if(r.isLeft()){
                    cacheStructures.fail(r.left().getValue());
                }else{
                    cacheStructures.complete(r.right().getValue());
                }
            }));
            cacheStructures.future().onComplete(r -> {
               for(final Promise<JsonArray> f : cacheStructuresPending){
                   if(r.succeeded()) {
                       f.tryComplete(r.result());
                   } else {
                       f.tryFail(r.cause());
                   }
               }
               cacheStructuresPending.clear();
               cacheStructuresDate = new Date();
            });
            final Promise<JsonArray> future = Promise.promise();
            cacheStructuresPending.add(future);
            return future.future();
        } else if(cacheStructures.future().isComplete()){
            if(cacheStructures.future().succeeded()){
                return Future.succeededFuture(cacheStructures.future().result());
            } else {
                return Future.failedFuture(cacheStructures.future().cause());
            }
        }else {//pending
            final Promise<JsonArray> future = Promise.promise();
            cacheStructuresPending.add(future);
            return future.future();
        }
    }

    public Future<JsonObject> getMappingUsage(String mappingId, Optional<String> structureId)
    {
        final Promise<JsonObject> future = Promise.promise();
        getMappings().onComplete(cacheRes -> {
            if(cacheRes.failed())
            {
                future.fail(cacheRes.cause().getMessage());
                return;
            }
            final Mappings mps = cacheRes.result();
            final Optional<Mapping> requested = mps.getById(mappingId);
            if(!requested.isPresent())
            {
                //if mapping changes -> all nodes cannot receive it and mapping could not be found
                future.complete(new JsonObject().put("usesInOtherStructs", 0).put("totalUses", 0));
                return;
            }

            Mapping found = requested.get();
            String usageQuery = "MATCH (a:Application:External)-[:PROVIDE]->(:Action)<-[:AUTHORIZE]-(r:Role) "+
                                "WHERE a.casType = {type} AND a.pattern = {pattern} " +
                                "RETURN r.structureId AS sID, COLLECT(a.name) AS connectors";
            neo.execute(usageQuery, new JsonObject().put("type", found.getCasType()).put("pattern", found.getPattern()), Neo4jResult.validResultHandler(r ->
            {
                if(r.isLeft())
                    future.fail(r.left().getValue());
                else
                {
                    JsonObject usageStats = new JsonObject();
                    long otherStructUsage = 0;
                    long totalUsages = 0;

                    JsonArray res = r.right().getValue();

                    for(int i = res.size(); i-- > 0;)
                    {
                        JsonObject stats = res.getJsonObject(i);
                        JsonArray cntrs = stats.getJsonArray("connectors");
                        totalUsages += cntrs.size();
                        if(stats.getString("sID").equals(structureId.orElse("")))
                            usageStats.put("connectorsInThisStruct", cntrs);
                        else
                            otherStructUsage += cntrs.size();

                    }
                    usageStats.put("usesInOtherStructs", otherStructUsage);
                    usageStats.put("totalUses", totalUsages);
                    future.complete(usageStats);
                }
            }));
        });
        return future.future();
    }

    public Future<Void> delete(String mappingId)
    {
        final Promise<Void> future = Promise.promise();
        final JsonObject deleteData = new JsonObject().put("_id", mappingId);

        if(mappingId == null)
        {
            return Future.failedFuture("cas.mappings.emptyId");
        }
        this.getMappingUsage(mappingId, Optional.empty()).onComplete(new Handler<AsyncResult<JsonObject>>()
        {
            @Override
            public void handle(AsyncResult<JsonObject> res)
            {
                if(res.failed())
                {
                    future.fail(res.cause().getMessage());
                    return;
                }
                else
                {
                    long nbUses = res.result().getLong("totalUses");
                    if(nbUses == 0)
                    {
                        mongoDb.delete(COLLECTION, deleteData, r->
                        {
                            if ("ok".equals(r.body().getString("status"))) {
                                future.complete();
                            } else{
                                future.fail(r.body().getString("message"));
                            }
                        });
                    }
                    else
                        future.fail("cas.mapping.inuse");
                }
            }
        });
        return future.future();
    }

    public static class Mappings{
        static final Logger logger = LoggerFactory.getLogger(Mappings.class);
        private final Map<String, Mapping> rowsByType;
        private final Map<String, Set<String>> structuresWithChildren = new HashMap<>();
        private final Map<String, Set<String>> structuresWithDescendants = new HashMap<>();
        private Mappings(Map<String, Mapping> rows) {
            this.rowsByType = rows;
        }

        public void addStructure(final String structureId, String parentId){
            this.structuresWithChildren.putIfAbsent(structureId, new HashSet<>());
            if(parentId != null){
                this.structuresWithChildren.putIfAbsent(parentId, new HashSet<>());
                this.structuresWithChildren.get(parentId).add(structureId);
            }
        }

        private void getDescendants(final String structureId, final Set<String> all){
            final Set<String> children = this.structuresWithChildren.getOrDefault(structureId, new HashSet<>());
            for(final String child : children){
                if(all.contains(child)){
                    logger.debug("Loop while getting descendant of "+structureId+ " child already added: "+child);
                }else{
                    all.add(child);
                    getDescendants(child, all);
                }
            }
        }

        public void computeHierarchy(){
            for(final String structureId : structuresWithChildren.keySet()){
                final Set<String> all = new HashSet<>();
                getDescendants(structureId, all);
                this.structuresWithDescendants.put(structureId, all);
            }
        }

        public JsonArray toJson(){
            final JsonArray all = new JsonArray();
            for(final Mapping mapping : rowsByType.values()){
                all.add(new JsonObject().put("casType", mapping.getCasType()).put("pattern", mapping.getPattern()).put("type", mapping.getType()));
            }
            return all;
        }

        public Optional<Mapping> find(Optional<String> structureId, String casType, String pattern, boolean canInherits, Optional<String> statCasType){
            //find by statType
            if(statCasType.isPresent()){
                if(rowsByType.containsKey(statCasType.get())){
                    final Mapping mapping = rowsByType.get(statCasType.get());
                    if(structureId.isPresent()){
                        final Set<String> descendantAndSelf = new HashSet<>();
                        if(canInherits){
                            final Set<String> found = structuresWithDescendants.getOrDefault(structureId.get(), new HashSet<>());
                            descendantAndSelf.addAll(found);
                        }
                        descendantAndSelf.add(structureId.get());
                        return Optional.of(mapping.copyWith(descendantAndSelf, false));
                    } else {
                        return Optional.of(mapping.copyWith(new HashSet<>(), true));
                    }
                } else {
                    logger.warn("Could not found forced statCasType: "+statCasType.get());
                    return Optional.empty();
                }
            }
            //find by pattern and casType
            for(Mapping mapping: rowsByType.values()){
                if(mapping.getCasType().equals(casType) && mapping.getPattern().equals(pattern)){
                    if(structureId.isPresent()){
                        final Set<String> descendantAndSelf = new HashSet<>();
                        if(canInherits){
                            final Set<String> found = structuresWithDescendants.getOrDefault(structureId.get(), new HashSet<>());
                            descendantAndSelf.addAll(found);
                        }
                        descendantAndSelf.add(structureId.get());
                        return Optional.of(mapping.copyWith(descendantAndSelf, false));
                    } else {
                        return Optional.of(mapping.copyWith(new HashSet<>(), true));
                    }
                }
            }
            return Optional.empty();
        }

        public Optional<Mapping> getById(String id)
        {
            Mapping m = rowsByType.get(id);
            return Optional.ofNullable(m);
        }
    }
}
