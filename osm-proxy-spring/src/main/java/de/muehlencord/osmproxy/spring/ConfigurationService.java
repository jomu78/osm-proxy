package de.muehlencord.osmproxy.spring;

import de.muehlencord.osmproxy.common.AbstractConfigurationService;
import de.muehlencord.osmproxy.common.entity.Configuration;
import de.muehlencord.osmproxy.common.entity.ConfigurationException;
import org.springframework.stereotype.Service;

/**
 * get configuration from application.yml
 *
 * @author Joern Muehlencord, 2021-12-01
 * @since 1.1
 */
@Service
public class ConfigurationService extends AbstractConfigurationService {

  private final Configuration configuration;

  public ConfigurationService(OsmCacheProperties osmCacheProperties) {
    this.configuration = new Configuration();
    this.configuration.setCache(osmCacheProperties.getCache());
    this.configuration.setLayerMap(osmCacheProperties.getLayerMap());

  }

  /**
   * return the configuration object. If not yet loaded, it is loaded from disk.
   *
   * @return the configuration object.
   *
   * @throws ConfigurationException if the configuration cannot be loaded from disk.
   */
  @Override
  protected Configuration getConfiguration() throws ConfigurationException {
    return configuration;
  }
}
