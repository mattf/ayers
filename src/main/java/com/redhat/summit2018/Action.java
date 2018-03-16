package com.redhat.summit2018;

import com.google.gson.JsonObject;

/**
 * Hello OpenWhisk...
 */
public class Action {
   public static JsonObject main(JsonObject args) {
      // Example args["swiftObj"] -
      // "swiftObj": {
      //    "container": "ayers-images",
      //    "method": "PUT",
      //    "object": "zero.jpg",
      //    "token": "AUTH_xyz...",
      //    "url": "http://host:port/v1/AUTH_gv0"
      // }
      return args;
   }
}
