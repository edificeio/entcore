package org.entcore.common.explorer;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import org.entcore.common.explorer.impl.ExplorerPluginCommunicationPostgres;
import org.entcore.common.explorer.impl.ExplorerPluginCommunicationRedis;
import org.entcore.common.mongodb.MongoClientFactory;
import org.entcore.common.postgres.IPostgresClient;
import org.entcore.common.redis.RedisClient;

import java.util.function.Function;

public class ExplorerPluginFactory {
    private static  Vertx vertxInstance;
    private static JsonObject explorerConfig;
    private static JsonObject globalConfig;
    public static void init(final Vertx vertx, final JsonObject config){
        globalConfig = config;
        vertxInstance = vertx;
        ExplorerPluginMetricsFactory.init(vertxInstance, config);
        if (config.getJsonObject("explorerConfig") != null) {
            explorerConfig = config.getJsonObject("explorerConfig");
        } else {
            final String explorer = (String) vertx.sharedData().getLocalMap("server").get("explorerConfig");
            if(explorer != null){
                explorerConfig = new JsonObject(explorer);
            }else {
                explorerConfig = new JsonObject();
            }
        }
    }

    public static boolean isEnabled() throws Exception {
        return getExplorerConfig().getBoolean("enabled", true);
    }

    public static JsonObject getExplorerConfig() throws Exception {
        if(explorerConfig == null){
            throw new Exception("Explorer config not initialized");
        }
        return explorerConfig;
    }

    public static JsonObject getRedisConfig() throws Exception {
        final JsonObject explorerConfig = getExplorerConfig();
        if(explorerConfig.containsKey("redisConfig")){
            return explorerConfig;
        }
        return globalConfig;
    }

    public static JsonObject getPostgresConfig() throws Exception {
        final JsonObject explorerConfig = getExplorerConfig();
        if(explorerConfig.containsKey("postgresConfig")){
            return explorerConfig;
        }
        return globalConfig;
    }

    public static IExplorerPluginCommunication getCommunication() throws Exception {
        if(explorerConfig == null){
            throw new Exception("Explorer config not initialized");
        }
        if(explorerConfig.getBoolean("postgres", false)){
            final IExplorerPluginMetricsRecorder metricsRecorder = ExplorerPluginMetricsFactory.getExplorerPluginMetricsRecorder("postgres");
            final IPostgresClient postgresClient = IPostgresClient.create(vertxInstance, getPostgresConfig(), false, true);
            final IExplorerPluginCommunication communication = new ExplorerPluginCommunicationPostgres(vertxInstance, postgresClient, metricsRecorder).setEnabled(isEnabled());
            return communication;
        }else {
            final IExplorerPluginMetricsRecorder metricsRecorder = ExplorerPluginMetricsFactory.getExplorerPluginMetricsRecorder("redis");
            final RedisClient redisClient = RedisClient.create(vertxInstance, getRedisConfig());
            final IExplorerPluginCommunication communication = new ExplorerPluginCommunicationRedis(vertxInstance, redisClient, metricsRecorder).setEnabled(isEnabled());
            return communication;
        }
    }

    public static IExplorerPlugin createMongoPlugin(final Function<ExplorerFactoryParams<MongoClient>, IExplorerPlugin> instance) throws Exception {
        final IExplorerPluginCommunication communication = getCommunication();
        final MongoClient mongoClient = MongoClientFactory.create(vertxInstance, globalConfig);
        final ExplorerFactoryParams<MongoClient> params = new ExplorerFactoryParams<MongoClient>(mongoClient,communication);
        return instance.apply(params).setConfig(getExplorerConfig());
    }

    public static IExplorerPlugin createPostgresPlugin(final Function<ExplorerFactoryParams<IPostgresClient>, IExplorerPlugin> instance) throws Exception {
        final IExplorerPluginCommunication communication = getCommunication();
        final IPostgresClient postgresClient = IPostgresClient.create(vertxInstance, globalConfig, false, true);
        final ExplorerFactoryParams<IPostgresClient> params = new ExplorerFactoryParams<IPostgresClient>(postgresClient,communication);
        return instance.apply(params).setConfig(getExplorerConfig());
    }

    public static class ExplorerFactoryParams<DB>{
        private final DB db;
        private final IExplorerPluginCommunication communication;

        public ExplorerFactoryParams(DB db, IExplorerPluginCommunication communication) {
            this.db = db;
            this.communication = communication;
        }

        public DB getDb() {
            return db;
        }

        public IExplorerPluginCommunication getCommunication() {
            return communication;
        }
    }
}
