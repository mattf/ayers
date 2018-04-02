package com.redhat.summit2018;

import java.lang.RuntimeException;

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

   public JsonObject getTransaction(String transactionId) {
      RemoteCache<String,String> cache = manager.getCache("txs");
      return new JsonParser()
         .parse(cache.get(transactionId))
         .getAsJsonObject();
   }

   public JsonObject getTask(String taskId) {
      RemoteCache<String,String> cache = manager.getCache("tasks");
      return new JsonParser()
         .parse(cache.get(taskId))
         .getAsJsonObject();
   }

   public void putScore(String transactionId, int score) {
      RemoteCache<String, String> cache = manager.getCache("objects");
      JsonObject value = new JsonObject();
      value.addProperty("score", score);
      cache.put(transactionId, value.toString());
   }
}
