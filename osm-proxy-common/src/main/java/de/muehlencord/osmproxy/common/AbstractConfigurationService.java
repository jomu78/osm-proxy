package de.muehlencord.osmproxy.common;

import de.muehlencord.osmproxy.common.entity.Cache;
import de.muehlencord.osmproxy.common.entity.Configuration;
import de.muehlencord.osmproxy.common.entity.ConfigurationException;
import de.muehlencord.osmproxy.common.entity.Layer;
import de.muehlencord.osmproxy.common.entity.Server;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Joern Muehlencord, 2020-09-10
 * @since 1.1.0
 */
public abstract class AbstractConfigurationService implements ConfigurationService {

  private static final Logger logger = LoggerFactory.getLogger(AbstractConfigurationService.class);

  /**
   * return the configuration object. If not yet loaded, it is loaded from disk.
   *
   * @return the configuration object.
   *
   * @throws ConfigurationException if the configuration cannot be loaded from disk.
   */
  protected abstract Configuration getConfiguration() throws ConfigurationException;

  /**
   * returns the cache directory for the given layer
   *
   * @param layerName the name of the layer to return the cache for - e.g. tiles.
   *
   * @return the directory where to store the tiles to.
   *
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
   *
   * @return a list of servers to use as upstream servers.
   *
   * @throws ConfigurationException if the configuration is not loaded or does not contain any upstream server definition.
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

}
