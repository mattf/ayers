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
         long modelEnter, modelExit;
         modelEnter = System.currentTimeMillis();
         Label[] labels = service.score(image);
         modelExit = System.currentTimeMillis();

         Store store = new Store(args);
         JsonElement object = swiftObj.get("object");
         if (object == null) {
            throw new RuntimeException("swiftObj missing object (transaction id) field");
         }
         String transactionId = object.getAsString().split("\\.", 2)[0];
         LOGGER.info("transaction id: " + transactionId);
         JsonObject transaction = args.has(transactionId) ?
            args.getAsJsonObject(transactionId) :
            store.getTransaction(transactionId)
            .orElseThrow(() -> new RuntimeException("failed to get transaction: " + transactionId));
         LOGGER.info("transaction: " + transaction);

         String taskId = transaction.get("taskId").getAsString();
         if (taskId == null) {
            throw new RuntimeException("transaction missing taskId field");
         }
         LOGGER.info("task id: " + taskId);
         JsonObject task = args.has(taskId) ?
            args.getAsJsonObject(taskId) :
            store.getTask(taskId)
            .orElseThrow(() -> new RuntimeException("failed to get task: " + taskId));
         LOGGER.info("task: " + task);
         JsonElement target = task.get("object");
         if (target == null) {
            throw new RuntimeException("task missing object field");
         }
         String targetStr = target.getAsString();
         int score = 0;
         float area = 1;
         float certainty = 0;
         LOGGER.info("looking for: " + targetStr);
         JsonArray objects = new JsonArray();
         HashSet<String> set = new HashSet<String>();
         for (Label label : labels) {
            LOGGER.info("model identified: {} - {} - {}", label.getVoc(), label.getScore(), label.getArea());
            if (!set.contains(label.getVoc())) {
               set.add(label.getVoc());
               objects.add(label.getVoc());
            }
            if (targetStr.equalsIgnoreCase(label.getVoc())) {
               LOGGER.info("found one; certainty: {}, area: {}", label.getScore(), label.getArea());
               score = task.get("point").getAsInt();

               // three ways to score -
               // 0. area of highest certainty
               // 1. largest area of any certainty
               // 2. aggregate area of any certainty

               // 0 -
               // select correct object with highest certainty
               if (certainty < label.getScore()) {
                  certainty = label.getScore();
                  area = label.getArea();
               }

               // 1 -
               // select correct object with largest area
               //area = (area > label.getArea() ? area : label.getArea());
            }
         }

         // TODO: use proportion of area to image as scaling factor
         // and maybe some additional factor to scale it down
         score *= area;

         LOGGER.info("awarding points: " + score);

         JsonObject result = new JsonObject();
         result.addProperty("score", score);

         // XXX: duplicated from transaction
         result.add("playerId", transaction.get("playerId"));
         result.addProperty("transactionId", transactionId);
         result.add("data-center", transaction.get("data-center"));
         result.addProperty("taskId", taskId);
         result.addProperty("url", image.getUrl().toString());
         result.add("objects", objects);
         result.add("taskName", task.get("description"));
         result.add("taskObject", target);

         exit = System.currentTimeMillis();

         result.addProperty("function-b-enter-time-ms", enter);
         result.addProperty("function-b-exit-time-ms", exit);
         result.addProperty("function-b-duration-time-ms", exit - enter);

         result.addProperty("model-enter-time-ms", modelEnter);
         result.addProperty("model-exit-time-ms", modelExit);
         result.addProperty("model-duration-time-ms", modelExit - modelEnter);

         LOGGER.info("store.putResult({}, {})", transactionId, result);
         if (!args.has("doNotStore")) {
            store.putResult(transactionId, result);
         }
      } catch (FileNotFoundException e) {
         e.printStackTrace();
      } catch (IOException e) {
         e.printStackTrace();
      }

      return args;
   }
}
