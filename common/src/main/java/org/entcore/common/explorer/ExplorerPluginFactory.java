package org.entcore.common.explorer;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import org.entcore.common.explorer.impl.ExplorerPluginCommunicationPostgres;
import org.entcore.common.explorer.impl.ExplorerPluginCommunicationRedis;
import org.entcore.common.mongodb.MongoClientFactory;
import org.entcore.common.postgres.IPostgresClient;
import org.entcore.common.redis.RedisClient;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static io.vertx.core.Future.failedFuture;
import static com.google.common.collect.Lists.newArrayList;
import static io.vertx.core.Future.succeededFuture;

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

    public static JsonObject getPostgresConfig() throws Exception{
        final JsonObject explorerConfig = getExplorerConfig();
        if(explorerConfig.containsKey("postgresConfig")){
            return explorerConfig;
        }
        return globalConfig;
    }

    public static Future<IExplorerPluginCommunication> getCommunication() {
        if(explorerConfig == null){
            return failedFuture("Explorer config not initialized");
        }
        try {
          if (explorerConfig.getBoolean("postgres", false)) {
            final IExplorerPluginMetricsRecorder metricsRecorder = ExplorerPluginMetricsFactory.getExplorerPluginMetricsRecorder("postgres");
            return IPostgresClient.create(vertxInstance, getPostgresConfig(), false, true)
              .flatMap(postgresClient -> {
                try {
                  final IExplorerPluginCommunication communication = new ExplorerPluginCommunicationPostgres(vertxInstance, postgresClient, metricsRecorder).setEnabled(isEnabled());
                  return succeededFuture(communication);
                } catch (Exception e) {
                  return failedFuture(e);
                }
              });
          } else {
            final IExplorerPluginMetricsRecorder metricsRecorder = ExplorerPluginMetricsFactory.getExplorerPluginMetricsRecorder("redis");
            return RedisClient.create(vertxInstance, getRedisConfig())
              .map(redisClient -> {
                try {
                  return new ExplorerPluginCommunicationRedis(vertxInstance, redisClient, metricsRecorder).setEnabled(isEnabled());
                } catch (Exception e) {
                  throw new RuntimeException(e);
                }
              });
          }
        } catch (Exception e) {
          return failedFuture(e);
        }
    }

    public static Future<IExplorerPlugin> createMongoPlugin(final Function<ExplorerFactoryParams<MongoClient>, IExplorerPlugin> instance) throws Exception {
      final List<Future<?>> futures = newArrayList(
        getCommunication(),
        MongoClientFactory.create(vertxInstance, globalConfig)
      );
        return Future.all(futures).map(res -> {
          final IExplorerPluginCommunication communication = res.resultAt(0);
          final MongoClient mongoClient = res.resultAt(1);
          try {
            final ExplorerFactoryParams<MongoClient> params = new ExplorerFactoryParams<MongoClient>(mongoClient, communication);
            return succeededFuture(instance.apply(params).setConfig(getExplorerConfig()));
          } catch (Exception e) {
            return failedFuture(new RuntimeException("Error while initializing explorer mongo plugin", e));
          }
        });
    }

    public static Future<IExplorerPlugin> createPostgresPlugin(final Function<ExplorerFactoryParams<IPostgresClient>, IExplorerPlugin> instance) {
      try {
        return getCommunication().flatMap(communication -> {
          try {
            return IPostgresClient.create(vertxInstance, globalConfig, false, true)
              .flatMap(postgresClient -> {
                final ExplorerFactoryParams<IPostgresClient> params = new ExplorerFactoryParams<IPostgresClient>(postgresClient, communication);
                Future<IExplorerPlugin> future;
                try {
                  future = succeededFuture(instance.apply(params).setConfig(getExplorerConfig()));
                } catch (Exception e) {
                  future = failedFuture(e);
                }
                return future;
              });
          } catch (Exception e) {
            throw new RuntimeException("Error while initializing explorer postgres plugin", e);
          }
        });
      } catch (Exception e) {
        throw new RuntimeException("Error while creating postgres plugin", e);
      }
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
