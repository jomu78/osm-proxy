/*
 * Copyright 2018 Joern Muehlencord <joern at muehlencord.de>.
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
package de.muehlencord.osmproxy.common;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.muehlencord.osmproxy.common.entity.Configuration;
import de.muehlencord.osmproxy.common.entity.ConfigurationException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author joern.muehlencord
 */
public class ConfigurationBuilder {

  private static final ObjectMapper objectMapper;

  static {
    objectMapper = new ObjectMapper();
    objectMapper.setVisibility(objectMapper.getSerializationConfig().getDefaultVisibilityChecker()
                                           .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                                           .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                                           .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                                           .withIsGetterVisibility(JsonAutoDetect.Visibility.NONE)
                                           .withCreatorVisibility(JsonAutoDetect.Visibility.NONE));
  }


  private ConfigurationBuilder() {
    // hide constructor
  }

  public static String toJson(Configuration config) throws JsonProcessingException {
    return objectMapper.writeValueAsString(config);
  }

  public static Configuration fromJson(String jsonString) throws JsonProcessingException {
    return objectMapper.readValue(jsonString, Configuration.class);
  }

  public static Configuration fromFile(Path configFile) throws ConfigurationException {
    if (configFile.toFile().exists()) {
      String jsonString;

      try {
        jsonString = new String(Files.readAllBytes(configFile));
        return fromJson(jsonString);
      } catch (IOException ex) {
        throw new ConfigurationException("Cannot read configfile from " + configFile.toString(), ex);
      }
    } else {
      throw new ConfigurationException("Cannot find file " + configFile.toString());
    }
  }
}
