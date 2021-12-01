package de.muehlencord.osmproxy.spring;

import de.muehlencord.osmproxy.common.AbstractConnectionManager;
import javax.annotation.PreDestroy;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author Joern Muehlencord, 2020-09-10
 * @since 1.1.0
 */
@Component
public class ConnectionManager extends AbstractConnectionManager {

  private static final Logger logger = LoggerFactory.getLogger(ConnectionManager.class);

  public ConnectionManager() {
    connectionManager = new PoolingHttpClientConnectionManager();
    connectionManager.setMaxTotal(20);
    connectionManager.setDefaultMaxPerRoute(2);
    logger.debug("Connection manager setup complete");
  }

  @PreDestroy
  @Override
  public void shutdown() {
    super.shutdown();
  }


}
