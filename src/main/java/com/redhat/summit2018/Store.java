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

   private String infinispanHost;

   public Store(JsonObject args) {
      infinispanHost = findInfinispanHost(args);
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

   public void updateTransaction(JsonObject args) {
      // txs cache schema -
      // <String, String>, the key is the transaction id, the value is a JSON string
      // The JSON contains 'playerId', 'taskId', metadata (JSON object).
      // https://github.com/rhdemo/scavenger-hunt-microservice/blob/master/src/main/java/me/escoffier/keynote/MetadataRepository.java

      if (args.has("swiftObj")) {
         String transactionId =
            args.getAsJsonObject("swiftObj").get("object").getAsString().split("\\.", 2)[0];
         LOGGER.info("transaction id: " + transactionId);

         RemoteCacheManager manager =
            new RemoteCacheManager(
               new ConfigurationBuilder()
               .addServer()
               .host(infinispanHost)
               .port(ConfigurationProperties.DEFAULT_HOTROD_PORT)
               .build());
         RemoteCache<String, String> cache = manager.getCache();
         LOGGER.info("cache:", cache);

         JsonObject value = new JsonObject();
         value.add("objects", args.get("objects"));
         LOGGER.info("value: " + value);
         cache.put(transactionId, value.toString());
      }
   }
}
