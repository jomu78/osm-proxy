package de.muehlencord.osmproxy.common;

import de.muehlencord.osmproxy.common.entity.Cache;
import de.muehlencord.osmproxy.common.entity.Configuration;
import de.muehlencord.osmproxy.common.entity.ConfigurationException;
import de.muehlencord.osmproxy.common.entity.Layer;
import de.muehlencord.osmproxy.common.entity.Server;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Joern Muehlencord, 2020-09-10
 * @since 1.1.0
 */
public class AbstractConfigurationService implements ConfigurationService {

  private static final Logger logger = LoggerFactory.getLogger(AbstractConfigurationService.class);


  /**
   * the version of the application - updated by maven build system automatically
   */
  private String version;
  /**
   * the build date of the application - updated by maven
   */
  private String buildDate;

  /**
   * the configuration to be used - loaded from $home/.osmproxy/osmproxy.conf
   */
  private Configuration configuration;


  @Override
  public void readConfiguration() {
    InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("buildInfo.properties");
    if (in == null) {
      return;
    }

    Properties props = new Properties();
    try {
      props.load(in);

      version = props.getProperty("build.version");
      buildDate = props.getProperty("build.timestamp");

      if (logger.isDebugEnabled()) {
        logger.debug("buildInfo.properties read successfully");
      }

    } catch (IOException ex) {
      logger.error("Cannot find buildInfo.properties. ", ex);
      version = "??";
      buildDate = "??";
    }
  }

  /**
   * return the configuration object. If not yet loaded, it is loaded from disk.
   *
   * @return the configuration object.
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
              logger.debug (ex.getMessage(), ex);
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

  /**
   * returns the cache directory for the given layer
   *
   * @param layerName the name of the layer to return the cache for - e.g. tiles.
   * @return the directory where to store the tiles to.
   * @throws ConfigurationException if the configuration is not loaded or the cache directory is not defined.
   */
  @Override
  public Path getCacheDirectory(String layerName) throws ConfigurationException {
    Cache cache = getConfiguration().getCache();
    if (cache == null) {
      throw new ConfigurationException("no cache defined");
    }

    Layer layer = getConfiguration().getLayer(layerName);
    if (layer == null) {
      throw new ConfigurationException("layer " + layerName + " is not defined");
    }

    Path path = Paths.get(cache.getPath(), layer.getCacheFolder());
    if (path == null) {
      throw new ConfigurationException("unable to resolve path from cache and layer");
    }
    return path;
  }

  /**
   * returns a list of servers to use as upstream servers.
   *
   * @param layerName the name of the layer to get the upstream server for - e.g. tiles.
   * @return a list of servers to use as upstream servers.
   * @throws ConfigurationException if the configuration is not loaded or does not contain any upstream server
   *                                definition.
   */
  @Override
  public List<Server> getUpstreamServer(String layerName) throws ConfigurationException {
    Layer layer = getConfiguration().getLayer(layerName);
    if (layer == null) {
      throw new ConfigurationException("layer " + layerName + " is not defined");
    }
    return layer.getUpstream();
  }

  @Override
  public int getRetentionTime() throws ConfigurationException {
    Cache cache = getConfiguration().getCache();
    if (cache == null) {
      throw new ConfigurationException("no cache defined");
    }
    Integer retentionTime = cache.getRetentionTime();
    if (retentionTime == null) {
      throw new ConfigurationException("retentionTime is not defined");
    } else {
      return retentionTime;
    }
  }

  public List<String> getAllLayers() throws ConfigurationException {
    return getConfiguration().getAllLayers();
  }

  @Override
  public String getVersion() {
    return version;
  }

  @Override
  public String getBuildDate() {
    return buildDate;
  }

}
