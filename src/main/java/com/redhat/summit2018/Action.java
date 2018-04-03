package com.redhat.summit2018;

import java.io.File;
import java.io.InputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.HashSet;

import com.google.gson.JsonElement;
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
      long enter, exit;

      enter = System.currentTimeMillis();

      LOGGER.info("args:" + args);

      if (args.has("echoMode") &&
          args.get("echoMode").getAsBoolean()) {
         return args;
      }

      try {
         JsonObject swiftObj = args.getAsJsonObject("swiftObj");
         if (swiftObj == null) {
            throw new RuntimeException("missing swiftObj: " + args);
         }

         Image image = new Image(args);
         ModelService service = new ModelService(args);
         Label[] labels = service.score(image);

         Store store = new Store(args);
         JsonElement object = swiftObj.get("object");
         if (object == null) {
            throw new RuntimeException("swiftObj missing object (transaction id) field");
         }
         String transactionId = object.getAsString().split("\\.", 2)[0];
         LOGGER.info("transaction id: " + transactionId);
         JsonObject transaction = store.getTransaction(transactionId)
            .orElseThrow(() -> new RuntimeException("failed to get transaction: " + transactionId));
         LOGGER.info("transaction: " + transaction);

         JsonElement taskId = transaction.get("taskId");
         if (taskId == null) {
            throw new RuntimeException("transaction missing taskId field");
         }
         LOGGER.info("task id: " + taskId);
         JsonObject task = store.getTask(taskId.getAsString())
            .orElseThrow(() -> new RuntimeException("failed to get task: " + taskId));
         LOGGER.info("task: " + task);
         JsonElement target = task.get("object");
         if (target == null) {
            throw new RuntimeException("task missing object field");
         }
         String targetStr = target.getAsString();
         int score = 0;
         LOGGER.info("looking for: " + targetStr);
         JsonArray objects = new JsonArray();
         HashSet<String> set = new HashSet<String>();
         for (Label label : labels) {
            LOGGER.info("model identified: " + label.getVoc() + " - " + label.getScore());
            if (!set.contains(label.getVoc())) {
               set.add(label.getVoc());
               objects.add(label.getVoc());
               if (targetStr.equalsIgnoreCase(label.getVoc())) {
                  LOGGER.info("found one");
                  score = task.get("point").getAsInt();
               }
            }
         }
         LOGGER.info("awarding points: " + score);

         JsonObject result = new JsonObject();
         result.addProperty("score", score);

         // XXX: duplicated from transaction
         result.add("playerId", transaction.get("playerId"));
         result.addProperty("transactionId", transactionId);
         result.add("data-center", transaction.get("data-center"));
         result.add("taskId", taskId);
         result.addProperty("url", image.getUrl().toString());
         result.add("objects", objects);
         result.add("taskName", task.get("description"));
         result.add("taskObject", target);

         exit = System.currentTimeMillis();

         result.addProperty("function-b-enter-time-ms", enter);
         result.addProperty("function-b-exit-time-ms", exit);
         result.addProperty("function-b-duration-time-ms", exit - enter);

         store.putResult(transactionId, result);
      } catch (FileNotFoundException e) {
         e.printStackTrace();
      } catch (IOException e) {
         e.printStackTrace();
      }

      return args;
   }
}
