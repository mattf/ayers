package com.redhat.summit2018;

import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.HashSet;
import java.net.URL;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;

import javax.ws.rs.core.MediaType;

import com.google.gson.JsonParser;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import javax.ws.rs.core.Response;
import javax.ws.rs.client.Entity;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.impl.ConfigurationProperties;

import org.apache.commons.io.IOUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Hello OpenWhisk...
 */
public class Action
{
   private static final Logger LOGGER = LogManager.getLogger(Action.class);

   public static JsonObject main(JsonObject args) {
      // YOLOv2 service
      // Input: { "image": base64(img) } -- Image
      // Output: [ { "score": float, "voc": "[category]" }, ... ] -- Label[]

      LOGGER.info("args:" + args);

      if (args.has("echoMode") &&
          args.get("echoMode").getAsBoolean()) {
         return args;
      }

      try {
         ResteasyClient client = new ResteasyClientBuilder().build();
         Response response = client.target(getModelEndpoint(args))
            .request()
            .accept(MediaType.APPLICATION_JSON_TYPE)
            .post(Entity.entity(getImage(args), MediaType.APPLICATION_JSON_TYPE));
         int status = response.getStatus();
         LOGGER.info("model status: " + status);
         Label[] labels = response.readEntity(Label[].class);
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


   private static String getModelEndpoint(JsonObject args) {
      String uri;
      if (args.has("modelEndpoint")) {
         uri = args.get("modelEndpoint").getAsString();
         LOGGER.info("found modelEndpoint: " + uri);
      } else {
         uri = "http://localhost:8080/v2/yolo";
         LOGGER.info("using default modelEndpoint: " + uri);
      }
      return uri;
   }


   private static Image getImage(JsonObject args)
      throws FileNotFoundException, IOException {
      byte[] data = new byte[0];
      if (args.has("imageFile")) {
         data = getImageFromFile(args.get("imageFile").getAsString());
      } else if (args.has("swiftObj")) {
         data = getImageFromS3(args.getAsJsonObject("swiftObj"));
      } else {
         LOGGER.fatal("do not know where to find the image, need imageFile or swiftObj");
      }

      return new Image(Base64.getEncoder().encodeToString(data));
   }


   private static byte[] getImageFromFile(String filename)
      throws FileNotFoundException, IOException {
      LOGGER.info("filename: " + filename);

      // XXX: will corrupt data for objects >2GB
      return IOUtils.toByteArray(new FileInputStream(filename));
   }


   private static byte[] getImageFromS3(JsonObject swiftObj)
      throws MalformedURLException, IOException {
      // swiftObj -
      // {
      //    "container": "[S3 BUCKET]",
      //    "method": "PUT",
      //    "object": "[TXN ID]",
      //    "token": "[AUTH TOKEN]",
      //    "url": "[S3 BASE URL]"
      // }
      LOGGER.info("swiftObj: " + swiftObj);

      // TODO: validate swiftObj schema
      if (!(swiftObj.has("url") &&
            swiftObj.has("container") &&
            swiftObj.has("object"))) {
         throw new IOException("url, container and object are required, given: " + swiftObj);
      }

      URL url = new URL(
         swiftObj.get("url").getAsString() + "/"
         + swiftObj.get("container").getAsString() + "/"
         + swiftObj.get("object").getAsString());
      LOGGER.info("S3 url: " + url);
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      // XXX: the token has two components separated by a ",", the X-Auth-Token is the second component
      String token = swiftObj.get("token").getAsString();
      String[] bits = token.split(",", 2);
      token = bits[bits.length == 2 ? 1 : 0];
      LOGGER.info("S3 auth token: " + token);
      connection.setRequestProperty("X-Auth-Token", token);

      LOGGER.info("S3 status code: " + connection.getResponseCode());

      // XXX: will corrupt data for objects >2GB
      return IOUtils.toByteArray(connection);
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
               .host("jdg-app-hotrod.infinispan.svc")
               .port(ConfigurationProperties.DEFAULT_HOTROD_PORT)
               .build());
         RemoteCache<String, String> cache = manager.getCache("txs");
         JsonObject value =
            new JsonParser()
            .parse(cache.get(transactionId))
            .getAsJsonObject();
         LOGGER.info("value: " + value);
         value.get("metadata").getAsJsonObject().add("objects", args.get("objects"));
         cache.put(transactionId, value.toString());
      }
   }
}
