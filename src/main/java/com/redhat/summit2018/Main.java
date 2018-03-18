package com.redhat.summit2018;

import com.google.gson.JsonObject;

public class Main
{
   public static void main(String[] args)
   {
      JsonObject jsonArgs = new JsonObject();
      for (String arg : args) {
         System.out.println("processing: " + arg);
         String[] parts = arg.split("=");
         if (parts.length == 2) {
            System.out.println("found: " + parts[0] + " : " + parts[1]);
            jsonArgs.addProperty(parts[0], parts[1]);
         }
      }
      
      Action action = new Action();
      JsonObject jsonResult = action.main(jsonArgs);

      System.out.println(jsonResult);
   }
}
