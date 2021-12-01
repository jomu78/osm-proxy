package de.muehlencord.osmproxy.common;

import de.muehlencord.osmproxy.common.entity.ConfigurationException;
import de.muehlencord.osmproxy.common.entity.Server;
import java.nio.file.Path;
import java.util.List;

/**
 * @author Joern Muehlencord, 2020-10-18
 * @since 1.1.0
 */
public interface ConfigurationService {

  List<Server> getUpstreamServer(String layerName) throws ConfigurationException;

  Path getCacheDirectory(String layerName) throws ConfigurationException;

  int getRetentionTime() throws ConfigurationException;

}
