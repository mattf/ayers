package com.redhat.summit2018;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Base64;
import java.util.List;

import javax.ws.rs.core.MediaType;

import com.google.gson.JsonObject;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import javax.ws.rs.core.Response;
import javax.ws.rs.client.Entity;

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
      } else {
         uri = "http://localhost:8080/v2/yolo";
      }
      return uri;
   }


   private static Image getImage(JsonObject args) {
      // Example args["swiftObj"] -
      // "swiftObj": {
      //    "container": "ayers-images",
      //    "method": "PUT",
      //    "object": "zero.jpg",
      //    "token": "AUTH_xyz...",
      //    "url": "http://host:port/v1/AUTH_gv0"
      // }

      Image image = new Image();
      if (args.has("imageFile")) {
         String filename = args.get("imageFile").getAsString();

         File file = new File(filename);
         try (FileInputStream in = new FileInputStream(file)) {
            byte data[] = new byte[(int) file.length()];
            in.read(data); // TODO: check result is -1
            image.setImage(Base64.getEncoder().encodeToString(data));
         } catch (FileNotFoundException e) {
            e.printStackTrace();
         } catch (IOException e) {
            e.printStackTrace();
         }
      }

      return image;
   }
}
