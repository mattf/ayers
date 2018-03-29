package com.redhat.summit2018;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import javax.ws.rs.core.Response;
import javax.ws.rs.client.Entity;

import javax.ws.rs.core.MediaType;

import com.google.gson.JsonObject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ModelService
{
   private static final Logger LOGGER = LogManager.getLogger(ModelService.class);

   private String endpoint;

   public ModelService(JsonObject args) {
      endpoint = findEndpoint(args);
   }

   public Label[] score(Image image) {
      // YOLOv2 service
      // Input: { "image": base64(img) } -- Image
      // Output: [ { "score": float, "voc": "[category]" }, ... ] -- Label[]

      ResteasyClient client = new ResteasyClientBuilder().build();
      Response response = client.target(endpoint)
         .request()
         .accept(MediaType.APPLICATION_JSON_TYPE)
         .post(Entity.entity(image, MediaType.APPLICATION_JSON_TYPE));
      int status = response.getStatus();
      LOGGER.info("model status: " + status);
      return response.readEntity(Label[].class);
   }

   private String findEndpoint(JsonObject args) {
      String uri = "http://localhost:8080/v2/yolo";
      if (args.has("modelEndpoint")) {
         uri = args.get("modelEndpoint").getAsString();
         LOGGER.info("found modelEndpoint: " + uri);
      } else {
         LOGGER.info("using default modelEndpoint: " + uri);
      }
      return uri;
   }
}
