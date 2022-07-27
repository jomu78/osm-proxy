/*
 * Copyright 2019 joern.muehlencord.
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

import de.muehlencord.osmproxy.common.AbstractConnectionManager;
import java.io.Serializable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.AccessTimeout;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;

/**
 * @author joern.muehlencord
 */
@Singleton
@Startup
@AccessTimeout(value = 60000)
public class WildflyConnectionManager extends AbstractConnectionManager implements Serializable {


  @PostConstruct
  public void init() {
    connectionManager = PoolingHttpClientConnectionManagerBuilder
        .create()
        .setMaxConnTotal(10)
        .setMaxConnPerRoute(2)
        .build();
  }

  @PreDestroy
  @Override
  public void shutdown() {
    super.shutdown();
  }
}
