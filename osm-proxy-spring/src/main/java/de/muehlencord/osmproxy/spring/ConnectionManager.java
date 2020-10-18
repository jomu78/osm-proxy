package de.muehlencord.osmproxy.spring;

import de.muehlencord.osmproxy.common.AbstractConnectionManager;
import javax.annotation.PreDestroy;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.stereotype.Component;

/**
 * @author Joern Muehlencord, 2020-09-10
 * @since 1.1.0
 */
@Component
public class ConnectionManager extends AbstractConnectionManager {

  public ConnectionManager() {
    connectionManager = new PoolingHttpClientConnectionManager();
    connectionManager.setMaxTotal(20);
    connectionManager.setDefaultMaxPerRoute(2);
  }

  @PreDestroy
  @Override
  public void shutdown() {
    super.shutdown();
  }


}
