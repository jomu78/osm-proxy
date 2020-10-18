package de.muehlencord.osmproxy.common;

import de.muehlencord.osmproxy.common.entity.ConfigurationException;
import de.muehlencord.osmproxy.common.entity.Server;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;

/**
 * @author Joern Muehlencord, 2020-10-18
 * @since 1.1.0
 */
public interface ConnectionManager {

  void executeDownload(Server currentServer, String userAgent, String urlString, Path tilePath)
      throws URISyntaxException, IOException;

  void deleteTile(Path tilePath, String deleteReason);

  boolean isInRetentionTime(Path tile, int retentionTime);

  boolean downloadFromUpStreamServer(List<Server> upstreamServer, DownloadConfiguration downloadConfiguration) throws ConfigurationException;


}
