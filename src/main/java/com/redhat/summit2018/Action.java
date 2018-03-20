package com.redhat.summit2018;

import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.net.URL;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;

import javax.ws.rs.core.MediaType;

import com.google.gson.JsonObject;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import javax.ws.rs.core.Response;
import javax.ws.rs.client.Entity;

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

      ResteasyClient client = new ResteasyClientBuilder().build();
      Response response = client.target(getModelEndpoint(args))
         .request()
         .accept(MediaType.APPLICATION_JSON_TYPE)
         .post(Entity.entity(getImage(args), MediaType.APPLICATION_JSON_TYPE));
      int status = response.getStatus();
      LOGGER.info("status: " + status);
      Label[] labels = response.readEntity(Label[].class);
      for (Label label : labels) {
         LOGGER.info("identified: " + label.getVoc() + " - " + label.getScore());
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


   private static Image getImage(JsonObject args) {
      Image image = new Image();
      try {
         byte[] data = new byte[0];
         if (args.has("imageFile")) {
            data = getImageFromFile(args.get("imageFile").getAsString());
         } else if (args.has("swiftObj")) {
            data = getImageFromS3(args.getAsJsonObject("swiftObj"));
         } else {
            LOGGER.fatal("unable to get image");
         }

         image.setImage(Base64.getEncoder().encodeToString(data));
      } catch (FileNotFoundException e) {
         e.printStackTrace();
      } catch (IOException e) {
         e.printStackTrace();
      }

      return image;
   }


   private static byte[] getImageFromFile(String filename)
      throws FileNotFoundException, IOException {
      File file = new File(filename);
      FileInputStream in = new FileInputStream(file);
      byte data[] = new byte[(int) file.length()];
      in.read(data); // TODO: check result is -1

      return data;
   }


   private static byte[] getImageFromS3(JsonObject swiftObj)
      throws MalformedURLException, IOException {
      // Example args["swiftObj"] -
      // {
      //    "container": "ayers-images",
      //    "method": "PUT",
      //    "object": "zero.jpg",
      //    "token": "AUTH_xyz...",
      //    "url": "http://host:port/v1/AUTH_gv0"
      // }
      LOGGER.info("swiftObj: " + swiftObj);

      URL url = new URL(
         swiftObj.get("url").getAsString() + "/"
         + swiftObj.get("container").getAsString() + "/"
         + swiftObj.get("object").getAsString());
      LOGGER.info("S3 url: " + url);
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setRequestProperty("X-Auth-Token", swiftObj.get("token").getAsString());

      LOGGER.info("S3 status code: " + connection.getResponseCode());

      // XXX: will corrupt data for objects >2GB
      return IOUtils.toByteArray(connection);
   }
}
