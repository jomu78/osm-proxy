package de.muehlencord.osmproxy.business.config.boundary;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.muehlencord.osmproxy.business.config.entity.Configuration;
import de.muehlencord.osmproxy.business.config.entity.ConfigurationException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 *
 * @author joern.muehlencord
 */
public class ConfigurationBuilder {

    private final static Gson GSON_INSTANCE = new GsonBuilder()
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
