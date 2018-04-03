package com.redhat.summit2018;

import java.lang.RuntimeException;

import java.util.Optional;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.impl.ConfigurationProperties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Store
{
   private static final Logger LOGGER = LogManager.getLogger(Store.class);

   private RemoteCacheManager manager;

   public Store(JsonObject args) {
      manager = new RemoteCacheManager(
         new ConfigurationBuilder()
         .addServer()
         .host(findInfinispanHost(args))
         .port(ConfigurationProperties.DEFAULT_HOTROD_PORT)
         .build());
   }

   private static String findInfinispanHost(JsonObject args) {
      String host;
      if (args.has("infinispanHost")) {
         host = args.get("infinispanHost").getAsString();
         LOGGER.info("found infinispanHost: " + host);
      } else {
         host = "jdg-app-hotrod.infinispan.svc";
         LOGGER.info("using default infinispanHost: " + host);
      }
      return host;
   }

   private Optional<JsonObject> get(String name, String id) {
      Optional<JsonObject> result = Optional.empty();
      RemoteCache<String,String> cache = manager.getCache(name);
      if (cache == null) {
         LOGGER.fatal("failed to get cache: " + name);
      } else {
         LOGGER.info("got cache: " + name);
         String object = cache.get(id);
         if (object == null) {
            LOGGER.fatal("failed to get: " + id);
         } else {
            LOGGER.info("got: " + object);
            result = Optional.of(new JsonParser().parse(object).getAsJsonObject());
         }
      }
      return result;
   }

   public Optional<JsonObject> getTransaction(String transactionId) {
      return get("txs", transactionId);
   }

   public Optional<JsonObject> getTask(String taskId) {
      return get("tasks", taskId);
   }

   public void putResult(String transactionId, JsonObject result) {
      RemoteCache<String, String> cache = manager.getCache("objects");
      if (cache == null) {
         LOGGER.fatal("failed to get cache: objects");
      } else {
         cache.put(transactionId, result.toString());
         LOGGER.info("put: " + result);
      }
   }
}
