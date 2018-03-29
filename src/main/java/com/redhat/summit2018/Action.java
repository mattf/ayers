package com.redhat.summit2018;

import java.io.File;
import java.io.InputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.HashSet;

import com.google.gson.JsonParser;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.impl.ConfigurationProperties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Hello OpenWhisk...
 */
public class Action
{
   private static final Logger LOGGER = LogManager.getLogger(Action.class);

   public static JsonObject main(JsonObject args) {
      LOGGER.info("args:" + args);

      if (args.has("echoMode") &&
          args.get("echoMode").getAsBoolean()) {
         return args;
      }

      try {
         ModelService service = new ModelService(args);
         Label[] labels = service.score(new Image(args));

         JsonArray objects = new JsonArray();
         HashSet<String> set = new HashSet<String>();
         for (Label label : labels) {
            LOGGER.info("model identified: " + label.getVoc() + " - " + label.getScore());
            if (!set.contains(label.getVoc())) {
               objects.add(label.getVoc());
               set.add(label.getVoc());
            }
         }
         args.add("objects", objects);

         updateTransaction(args);
      } catch (FileNotFoundException e) {
         e.printStackTrace();
      } catch (IOException e) {
         e.printStackTrace();
      }

      return args;
   }


   private static String getInfinispanHost(JsonObject args) {
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


   private static void updateTransaction(JsonObject args) {
      // txs cache schema -
      // <String, String>, the key is the transaction id, the value is a JSON string
      // The JSON contains 'playerId', 'taskId', metadata (JSON object).
      // https://github.com/rhdemo/scavenger-hunt-microservice/blob/master/src/main/java/me/escoffier/keynote/MetadataRepository.java

      if (args.has("swiftObj")) {
         String transactionId = args.getAsJsonObject("swiftObj").get("object").getAsString();
         LOGGER.info("transaction id: " + transactionId);

         RemoteCacheManager manager =
            new RemoteCacheManager(
               new ConfigurationBuilder()
               .addServer()
               .host(getInfinispanHost(args))
               .port(ConfigurationProperties.DEFAULT_HOTROD_PORT)
               .build());
         RemoteCache<String, String> cache = manager.getCache("txs");
         JsonObject value =
            new JsonParser()
            .parse(cache.get(transactionId))
            .getAsJsonObject();
         LOGGER.info("value: " + value);
         JsonObject metadata;
         if (value.has("metadata")) {
            metadata = value.getAsJsonObject("metadata");
         } else {
            metadata = new JsonObject();
            value.add("metadata", metadata);
         }
         metadata.add("objects", args.get("objects"));
         cache.put(transactionId, value.toString());
      }
   }
}
