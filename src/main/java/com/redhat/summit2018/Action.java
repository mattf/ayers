package com.redhat.summit2018;

import java.io.File;
import java.io.InputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.HashSet;

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

         String transactionId =
            args.getAsJsonObject("swiftObj").get("object").getAsString().split("\\.", 2)[0];
         LOGGER.info("transaction id: " + transactionId);

         new Store(args).store(transactionId, args.toString());
      } catch (FileNotFoundException e) {
         e.printStackTrace();
      } catch (IOException e) {
         e.printStackTrace();
      }

      return args;
   }
}
