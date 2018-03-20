package com.redhat.summit2018;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class Main
{
   private static final Logger LOGGER = LogManager.getLogger(Main.class);

   public static void main(String[] args)
   {
      if (args.length < 1) {
         LOGGER.warn("no arguments, exiting");
         return;
      }

      LOGGER.info("proceeding with args: " + args[0]);
      JsonObject jsonResult =
         new Action().main(new JsonParser().parse(args[0]).getAsJsonObject());

      LOGGER.info(jsonResult);
   }
}
