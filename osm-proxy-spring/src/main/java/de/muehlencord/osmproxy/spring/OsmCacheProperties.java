package de.muehlencord.osmproxy.spring;

import de.muehlencord.osmproxy.common.entity.Cache;
import de.muehlencord.osmproxy.common.entity.Layer;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.validation.annotation.Validated;

/**
 * spring boot configuration properties to configure the osm proxy application via application.properties
 *
 * @author Joern Muehlencord, 2021-12-01
 * @since 1.1
 */
@ConfigurationProperties(prefix = "osmproxy", ignoreUnknownFields = false)
@Validated
@Getter
@Setter
public class OsmCacheProperties {

  @NestedConfigurationProperty
  private Cache cache;
  @NestedConfigurationProperty
  private Map<String, Layer> layerMap;


}
