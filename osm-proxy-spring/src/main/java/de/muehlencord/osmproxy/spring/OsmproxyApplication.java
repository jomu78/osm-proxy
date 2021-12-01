package de.muehlencord.osmproxy.spring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Add a short description of the class
 *
 * @author Joern Muehlencord, 2020-09-10
 * @since 1.1.0
 */
@SpringBootApplication
@EnableConfigurationProperties(OsmCacheProperties.class)
 public  class OsmproxyApplication {

  public static void main(String[] args) {
    SpringApplication.run(OsmproxyApplication.class, args);
  }



}
