package com.redhat.summit2018;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.util.Base64;

import org.apache.commons.io.IOUtils;

import com.google.gson.JsonObject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Image
{
   private static final Logger LOGGER = LogManager.getLogger(Image.class);

   private String image;

   public Image(JsonObject args)
      throws FileNotFoundException, IOException {
      if (args.has("imageFile")) {
         image = encode(args.get("imageFile").getAsString());
      } else if (args.has("swiftObj")) {
         image = encode(args.getAsJsonObject("swiftObj"));
      } else {
         throw new IOException("do not know where to find the image, need imageFile or swiftObj");
      }
   }

   private String encode(JsonObject swiftObj)
      throws MalformedURLException, IOException {
      // expected schema:
      // swiftObj: {
      //   container: "[S3 BUCKET NAME]",
      //   object: "[OBJECT NAME]",
      //   token: "[AUTH TOKEN]",
      //   url: "[S3 BASE URL]"
      // }

      LOGGER.info("encoding image from swiftObj: " + swiftObj);

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

      return encode(IOUtils.toByteArray(connection));
   }

   private String encode(String filename)
      throws FileNotFoundException, IOException {
      LOGGER.info("encoding image from file:  " + filename);
      return encode(IOUtils.toByteArray(new FileInputStream(filename)));
   }

   private String encode(byte[] data) {
      return Base64.getEncoder().encodeToString(data);
   }

   public Image(String image) {
      this.image = image;
   }

   public void setImage(String image) {
      this.image = image;
   }

   public String getImage() {
      return image;
   }
}
