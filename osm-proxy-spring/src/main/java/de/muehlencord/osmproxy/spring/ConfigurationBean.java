/*
 * Copyright 2020 Joern Muehlencord <joern at muehlencord.de>.
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
package de.muehlencord.osmproxy.spring;

import de.muehlencord.osmproxy.common.AbstractConfigurationService;
import javax.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * @author joern.muehlencord
 * @since 1.1.0
 */
@Component
public class ConfigurationBean extends AbstractConfigurationService {


  @PostConstruct
  public void init() {
    readConfiguration();
  }

}
