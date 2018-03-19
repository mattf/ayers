package com.redhat.summit2018;

import com.google.gson.JsonObject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class Main
{
   private static final Logger LOGGER = LogManager.getLogger(Main.class);

   public static void main(String[] args)
   {
      JsonObject jsonArgs = new JsonObject();
      for (String arg : args) {
         LOGGER.info("processing: " + arg);
         String[] parts = arg.split("=");
         if (parts.length == 2) {
            LOGGER.info("found: " + parts[0] + " : " + parts[1]);
            jsonArgs.addProperty(parts[0], parts[1]);
         }
      }
      
      Action action = new Action();
      JsonObject jsonResult = action.main(jsonArgs);

      LOGGER.info(jsonResult);
   }
}
