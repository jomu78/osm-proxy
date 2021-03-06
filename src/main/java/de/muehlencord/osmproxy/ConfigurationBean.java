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
package de.muehlencord.osmproxy;

import de.muehlencord.osmproxy.business.config.boundary.ConfigurationBuilder;
import de.muehlencord.osmproxy.business.config.entity.Cache;
import de.muehlencord.osmproxy.business.config.entity.Configuration;
import de.muehlencord.osmproxy.business.config.entity.ConfigurationException;
import de.muehlencord.osmproxy.business.config.entity.Layer;
import de.muehlencord.osmproxy.business.config.entity.Server;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;
import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author joern.muehlencord
 */
@Singleton
public class ConfigurationBean {

    /**
     * the logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationBean.class);

    /**
     * the version of the application - updated by maven build system
     * automatically
     */
    private String version;
    /**
     * the build date of the application - updated by maven
     */
    private String buildDate;

    /**
     * the configuration to be used - loaded from $home/.osmproxy/osmproxy.conf
     */
    private Configuration configuration;

    @PostConstruct
    public void init() {
        InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("buildInfo.properties");
        if (in == null) {
            return;
        }

        Properties props = new Properties();
        try {
            props.load(in);

            version = props.getProperty("build.version");
            buildDate = props.getProperty("build.timestamp");

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("buildInfo.properties read successfully");
            }

        } catch (IOException ex) {
            LOGGER.error("Cannot find buildInfo.properties. ", ex);
            version = "??";
            buildDate = "??";
        }

    }

    /**
     * return the configuration object. If not yet loaded, it is loaded from
     * disk.
     *
     * @return the configuration object.
     * @throws ConfigurationException if the configuration cannot be loaded from
     * disk.
     */
    protected Configuration getConfiguration() throws ConfigurationException {
        if (configuration == null) {
            URL url = ConfigurationBean.class.getResource("osmproxy.cfg");

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

    /**
     * returns the cache directory for the given layer
     *
     * @param layerName the name of the layer to return the cache for - e.g.
     * tiles.
     * @return the directory where to store the tiles to.
     * @throws ConfigurationException if the configuration is not loaded or the
     * cache directory is not defined.
     */
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

    /**
     * returns a list of servers to use as upstream servers.
     *
     * @param layerName the name of the layer to get the upstream server for -
     * e.g. tiles.
     * @return a list of servers to use as upstream servers.
     * @throws ConfigurationException if the configuration is not loaded or does
     * not contain any upstream server definition.
     */
    public List<Server> getUpstreamServer(String layerName) throws ConfigurationException {
        Layer layer = getConfiguration().getLayer(layerName);
        if (layer == null) {
            throw new ConfigurationException("layer " + layerName + " is not defined");
        }
        return layer.getUpstream();
    }

    public int getRetentionTime() throws ConfigurationException {
        Cache cache = getConfiguration().getCache();
        if (cache == null) {
            throw new ConfigurationException("no cache defined");
        }
        Integer retentionTime = cache.getRetentionTime();
        if (retentionTime == null) {
            throw new ConfigurationException("retentionTime is not defined");
        } else {
            return retentionTime;
        }
    }

    public List<String> getAllLayers() throws ConfigurationException {
        return getConfiguration().getAllLayers();
    }

    public String getVersion() {
        return version;
    }

    public String getBuildDate() {
        return buildDate;
    }

}
