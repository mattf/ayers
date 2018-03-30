package com.redhat.summit2018;

import java.lang.RuntimeException;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.impl.ConfigurationProperties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Store
{
   private static final Logger LOGGER = LogManager.getLogger(Store.class);

   private String infinispanHost;

   public Store(JsonObject args) {
      infinispanHost = findInfinispanHost(args);
   }

   private static String findInfinispanHost(JsonObject args) {
      String host;
      if (args.has("infinispanHost")) {
         host = args.get("infinispanHost").getAsString();
         LOGGER.info("found infinispanHost: " + host);
      } else {
         host = "jdg-app-hotrod.infinispan.svc";
         LOGGER.info("using default infinispanHost: " + host);
      }
      return host;
   }

   public void store(String key, String value) {
      LOGGER.info(key + " = " + value);
      RemoteCacheManager manager =
         new RemoteCacheManager(
            new ConfigurationBuilder()
            .addServer()
            .host(infinispanHost)
            .port(ConfigurationProperties.DEFAULT_HOTROD_PORT)
            .build());
      RemoteCache<String, String> cache = manager.getCache();

      cache.put(key, value);
   }
}
