package de.muehlencord.osmproxy;

import de.muehlencord.osmproxy.business.config.entity.ConfigurationException;
import de.muehlencord.osmproxy.business.config.entity.Cache;
import de.muehlencord.osmproxy.business.config.boundary.ConfigurationBuilder;
import de.muehlencord.osmproxy.business.config.entity.Configuration;
import de.muehlencord.osmproxy.business.config.entity.Layer;
import de.muehlencord.osmproxy.business.config.entity.Server;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import javax.ejb.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author joern.muehlencord
 */
@Singleton
public class ConfigurationBean {

    private Configuration configuration;

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationBean.class);

    protected Configuration getConfiguration() throws ConfigurationException {
        if (configuration == null) {
            URL url = ConfigurationBean.class.getResource("osmproxy.cfg"); // FIXME needs to be tested, will be null at the moment

            if (url == null) {
                // try to find file in user.home
                Path path = Paths.get(System.getProperty("user.home"), ".osmproxy", "osmproxy.cfg");
                if (path.toFile().exists()) {
                    try {
                        configuration = ConfigurationBuilder.fromFile(path);
                        LOGGER.info("Loaded configuration from {}", path.toString());
                    } catch (ConfigurationException ex) {
                        LOGGER.error("Cannot load configuration from file. Reason: " + ex.toString(), ex);
                        configuration = null;
                    }
                } else {
                    LOGGER.error("Configuration not found");
                    configuration = null;
                }
            } else {
                LOGGER.error("Search for configuration in classpath not yet supported, please store in " + System.getProperty("user.home") + "/.osmproxy/osmproxy.config");
                configuration = null;
            }
        }

        if (configuration == null) {
            throw new ConfigurationException("Invalid configuration, see log for details");
        }

        return configuration;
    }

    public Path getCacheDirectory(String layerName) throws ConfigurationException {
        Cache cache = getConfiguration().getCache();
        if (cache == null) {
            throw new ConfigurationException("no cache defined");
        }

        Layer layer = getConfiguration().getLayer(layerName);
        if (layer == null) {
            throw new ConfigurationException("layer " + layerName + " is not defined");
        }

        Path path = Paths.get(cache.getPath(), layer.getCacheFolder());
        if (path == null) {
            throw new ConfigurationException("unable to resolve path from cache and layer");
        }
        return path;
    }

    public List<Server> getUpstreamServer(String layerName) throws ConfigurationException {
        Layer layer = getConfiguration().getLayer(layerName);
        if (layer == null) {
            throw new ConfigurationException("layer " + layerName + " is not defined");
        }
        return layer.getUpstream();
    }

}
