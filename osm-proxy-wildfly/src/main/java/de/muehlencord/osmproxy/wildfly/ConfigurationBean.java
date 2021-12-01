/*
 * Copyright 2018 Joern Muehlencord <joern at muehlencord.de>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.muehlencord.osmproxy.wildfly;

import de.muehlencord.osmproxy.common.AbstractConfigurationService;
import de.muehlencord.osmproxy.common.ConfigurationBuilder;
import de.muehlencord.osmproxy.common.entity.Configuration;
import de.muehlencord.osmproxy.common.entity.ConfigurationException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author joern.muehlencord
 */
@Singleton
public class ConfigurationBean extends AbstractConfigurationService {

  private static final Logger logger = LoggerFactory.getLogger(ConfigurationBean.class);

  /**
   * the configuration to be used
   */
  protected Configuration configuration = null;

  /**
   * return the configuration object. If not yet loaded, it is loaded from disk.
   *
   * @return the configuration object.
   *
   * @throws ConfigurationException if the configuration cannot be loaded from disk.
   */
  protected synchronized Configuration getConfiguration() throws ConfigurationException {
    if (configuration == null) {
      URL url = AbstractConfigurationService.class.getResource("osmproxy.cfg");

      if (url == null) {
        // try to find file in user.home
        Path path = Paths.get(System.getProperty("user.home"), ".osmproxy", "osmproxy.cfg");
        if (path.toFile().exists()) {
          try {
            configuration = ConfigurationBuilder.fromFile(path);
            logger.info("Loaded configuration from {}", path);
          } catch (ConfigurationException ex) {
            logger.error("Cannot load configuration from file. Reason: {}", ex.getMessage());
            if (logger.isDebugEnabled()) {
              logger.debug(ex.getMessage(), ex);
            }
            configuration = null;
          }
        } else {
          logger.error("Configuration not found");
          configuration = null;
        }
      } else {
        logger.error("Search for configuration in classpath not yet supported, please store in {}"
            + "/.osmproxy/osmproxy.config", System.getProperty("user.home"));
        configuration = null;
      }
    }

    if (configuration == null) {
      throw new ConfigurationException("Invalid configuration, see log for details");
    }

    return configuration;
  }
}
