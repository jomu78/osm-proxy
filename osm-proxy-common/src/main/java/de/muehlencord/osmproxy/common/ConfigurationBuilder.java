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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.muehlencord.osmproxy.common.entity.Configuration;
import de.muehlencord.osmproxy.common.entity.ConfigurationException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 *
 * @author joern.muehlencord
 */
public class ConfigurationBuilder {

    private static final Gson GSON_INSTANCE = new GsonBuilder()
            .setPrettyPrinting()
            .excludeFieldsWithoutExposeAnnotation()
            .create();

    private ConfigurationBuilder() {
        // hide constructor        
    }

    public static String toJson(Configuration config) {
        return GSON_INSTANCE.toJson(config);
    }

    public static Configuration fromJson(String jsonString) {
        return GSON_INSTANCE.fromJson(jsonString, Configuration.class);
    }

    public static Configuration fromFile(Path configFile) throws ConfigurationException {
        if (configFile.toFile().exists()) {            
            String jsonString;
            
            try {
                jsonString = new String(Files.readAllBytes(configFile));
            } catch (IOException ex) {
                throw new ConfigurationException ("Cannot read configfile from "+configFile.toString(), ex);  
            }
            
            return fromJson (jsonString); 
        } else {
            throw new ConfigurationException("Cannot find file " + configFile.toString());
        }
    }
}
