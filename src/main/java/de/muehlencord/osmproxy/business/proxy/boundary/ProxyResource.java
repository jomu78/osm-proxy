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
package de.muehlencord.osmproxy.business.proxy.boundary;

import de.muehlencord.osmproxy.ConfigurationBean;
import de.muehlencord.osmproxy.business.config.entity.ConfigurationException;
import de.muehlencord.osmproxy.business.config.entity.Server;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import javax.annotation.PreDestroy;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.imageio.ImageIO;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * REST Web Service
 *
 * @author joern.muehlencord
 */
@Stateless
@javax.ws.rs.Path("rest")
public class ProxyResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyResource.class);

    @EJB
    private ConfigurationBean configurationBean;

    private final CloseableHttpClient httpClient;

    public ProxyResource() {
        httpClient = HttpClients.createDefault();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Created new httpClient");
        }

    }

    @PreDestroy
    void preDestroy() {
        if (httpClient != null) {
            try {
                httpClient.close();
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("httpClient shutdown");
                }

            } catch (IOException ex) {
                LOGGER.error(ex.getMessage());
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Detailed stacktrace", new Object[]{ex});
                }
            }
        }
    }

    @GET
    @Produces({"image/png", "text/plain"})
    @javax.ws.rs.Path("/{layer}/{z}/{x}/{y}.{ending}")
    public Response getTile(
            @HeaderParam("user-agent") String userAgent,
            @PathParam("layer") String layer,
            @PathParam("z") Long z,
            @PathParam("x") Long x,
            @PathParam("y") Long y,
            @PathParam("ending") String ending) {

        if ((layer == null) || (x == null) || (y == null) || (z == null) || (ending == null)) {
            return createErrorResponse("<layer>/<z>/<x>/<y>.<filetype> parameter is mandatory", HttpURLConnection.HTTP_BAD_REQUEST);
        }

        if (z < 0 || (z > 16)) {
            return createErrorResponse("parameter z must be between 0 and 16", HttpURLConnection.HTTP_BAD_REQUEST);
        }

        if (!ending.equals("png")) {
            return createErrorResponse("png is currently supported only", HttpURLConnection.HTTP_BAD_REQUEST);
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Requesting tile {}/{}/{}/{}.{}", layer, z, x, y, ending);
        }

        Path layerCacheFolder;
        try {
            layerCacheFolder = configurationBean.getCacheDirectory(layer);
        } catch (ConfigurationException ex) {
            LOGGER.error(ex.getMessage());
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Detailed stacktrace", new Object[]{ex});
            }
            return createErrorResponse("cannot get cache folder from configuration", HttpURLConnection.HTTP_INTERNAL_ERROR, ex);
        }

        // calculate file to load from disk
        Path tile = layerCacheFolder
                .resolve(z.toString())
                .resolve(x.toString())
                .resolve(y.toString() + "." + ending);

        Response response;
        // check if the tile exist
        // if yes and in retention time - serv it from disk
        // if yes and no longer in retention time - delete it from cache and serv it from upstream
        // it not, serv it from upstream
        if (tile.toFile().exists()) {
            if (isInRetentionTime(tile)) {
                response = respondTileFromDiskCache(tile, layer, z, x, y, ending);
            } else {
                deleteTile(tile, "delete outdated file {}");
                response = respondTileFromUpstreamServer(userAgent, tile, layer, z, x, y, ending);
            }
        } else {
            response = respondTileFromUpstreamServer(userAgent, tile, layer, z, x, y, ending);
        }
        response.getHeaders().add("Access-Control-Allow-Origin", "*");
        return response;
    }

    private Response respondTileFromDiskCache(Path tilePath, String layer, Long z, Long x, Long y, String ending) {
        byte[] imageData;
        BufferedImage image = null;
        // try to load image from disk 
        // if fails, image is broken and file needs to be deleted from disk
        try {
            image = ImageIO.read(tilePath.toFile());
        } catch (IOException ex) {

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Cannot read image from file {}", tilePath.toString());
                LOGGER.debug("Detailed stacktrace", new Object[]{ex});
            }
        }
        if (image == null) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Cannot construct image from file {}, going to delete file", tilePath.toString());
            }
            deleteTile(tilePath, "Deleted broken file {}");
            return createErrorResponse("error while reading tile from cache", HttpURLConnection.HTTP_INTERNAL_ERROR);
        } else {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(image, "png", baos);
                imageData = baos.toByteArray();
            } catch (IOException ex) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(ex.toString(), ex);
                }
                return createErrorResponse("error while reading tile from cache", HttpURLConnection.HTTP_INTERNAL_ERROR);
            }
            LOGGER.debug("served tile {}/{}/{}/{}.{} from cache", layer, z, x, y, ending);
            return Response.ok(imageData).type(new MediaType("image", "png")).build();
        }
    }

    private Response respondTileFromUpstreamServer(String userAgent, Path tilePath, String layer, Long z, Long x, Long y, String ending) {
        // file does not exis, try to get it from upstream server
        if (!tilePath.toFile().getParentFile().exists()) {
            tilePath.toFile().getParentFile().mkdirs();
        }

        boolean fileDownloaded;
        try {
            fileDownloaded = downloadFromUpStreamServer(userAgent, tilePath, layer, z, x, y, ending);
        } catch (ConfigurationException ex) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Detailed stacktrace", new Object[]{ex});
            }
            return createErrorResponse(ex.getMessage(), HttpURLConnection.HTTP_INTERNAL_ERROR, ex);
        }
        if (fileDownloaded) {
            if (tilePath.toFile().exists()) {
                return respondTileFromDiskCache(tilePath, layer, z, x, y, ending);
            } else {
                LOGGER.error("tile {}/{}/{}/{}.{} downloaded, but not available in cache", layer, z, x, y, ending);
                return createErrorResponse("file downloaded but not available in cache", HttpURLConnection.HTTP_INTERNAL_ERROR);
            }
        } else {
            LOGGER.debug("tile {}/{}/{}/{}.{} not downloaded", layer, z, x, y, ending);
            return createErrorResponse("tile not available", HttpURLConnection.HTTP_NOT_FOUND);
        }
    }

    private boolean downloadFromUpStreamServer(String userAgent, Path tilePath, String layer, long z, long x, long y, String ending) throws ConfigurationException {
        List<Server> upstreamServer = configurationBean.getUpstreamServer(layer);
        Iterator<Server> it = upstreamServer.iterator();
        while (it.hasNext()) {
            Server currentServer = it.next();
            String urlString = currentServer.getUrl();
            urlString = urlString.replace("{layer}", layer);
            urlString = urlString.replace("{z}", Long.toString(z));
            urlString = urlString.replace("{x}", Long.toString(x));
            urlString = urlString.replace("{y}", Long.toString(y));
            urlString = urlString.replace("{ending}", ending);

            try {
                URL url = new URL(urlString);
                RequestConfig config = getRequestConfig(url);
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Trying to download tile from upstream server {}", urlString);
                }

                HttpGet httpget = new HttpGet(url.toURI());
                httpget.setConfig(config);

                // check if userAgent is set for server, if yes, use this one
                // if not, use userAgent from client (default)
                if (currentServer.getUserAgent() == null) {
                    httpget.setHeader("User-Agent", userAgent);
                } else {
                    httpget.setHeader("User-Agent", currentServer.getUserAgent());
                }
                httpget.setHeader("Accept-Encoding", "deflate"); // TODO add gzip support
                httpget.setHeader("Accept-Language", "en-US");

                HttpResponse response = httpClient.execute(httpget);
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    FileOutputStream fos;
                    try (InputStream is = entity.getContent()) {
                        fos = new FileOutputStream(tilePath.toFile());
                        int inByte;
                        while ((inByte = is.read()) != -1) {
                            fos.write(inByte);
                        }
                    }
                    fos.close();
                }
                LOGGER.info("stored {} as {}", urlString, tilePath.toString());

                return true;
            } catch (URISyntaxException | IOException ex) {
                LOGGER.error("cannot construct URL for upstream server. ", urlString);
                LOGGER.error(ex.toString(), ex);
                throw new WebApplicationException(
                        Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR)
                                .entity("upstream server url is not valid")
                                .build()
                );
            }
        }

        return false;
    }

    private Response createErrorResponse(String message, int status) {
        return createErrorResponse(message, status, null);
    }

    private Response createErrorResponse(String message, int status, Exception ex) {
        LOGGER.error("request error: " + message);
        if (ex != null) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(ex.toString(), ex);
            }
        }
        return Response.status(status)
                .entity(message)
                .encoding("UTF-8")
                .type(MediaType.TEXT_PLAIN)
                .build();
    }

    private RequestConfig getRequestConfig(URL url) {
        String httpProxyHost;
        String httpProxyPortString;
        Integer httpProxyPort = null;
        String urlHostString = url.getProtocol() + "://" + url.getHost();
        String nonProxyHosts = System.getProperty("http.nonProxyHosts");

        HttpHost httpProxy = null;
        if (matchesNonProxyHosts(nonProxyHosts, urlHostString)) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Using no proxy, as {} matches nonProxyHosts {}", url.toString(), nonProxyHosts);
            }

        } else {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Checking proxy settings");
            }
            String prefix;
            if (url.getProtocol().toLowerCase(Locale.US).contains("https")) {
                prefix = "https";
            } else {
                prefix = "http";
            }

            httpProxyHost = System.getProperty(prefix + ".proxyHost");
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(prefix + ".proxyHost = {}", httpProxyHost);
            }
            httpProxyPortString = System.getProperty(prefix + ".proxyPort");            
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(prefix + ".proxyPort = {}", httpProxyPortString);
            }
            if ((httpProxyPortString == null) || (httpProxyPortString.equals(""))) {
                httpProxyPort = 0;
            } else {
                try {
                    httpProxyPort = Integer.parseInt(httpProxyPortString.trim());
                } catch (NumberFormatException ex) {                    
                    LOGGER.error("Cannot parse proxy port, {} is not a valid number", httpProxyPortString);
                }
            }

            if (httpProxyHost != null && httpProxyPort != null && !httpProxyPort.equals(0)) {
                httpProxy = new HttpHost(httpProxyHost, httpProxyPort, "http");
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Using proxy {}:{} to connect to {}", httpProxyHost, httpProxyPort, url.toString());
                }
            } else {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Using no proxy to connect to {}", url.toString());
                }
            }
        }

        RequestConfig config = RequestConfig.custom()
                .setProxy(httpProxy)
                .build();

        return config;
    }

    private boolean matchesNonProxyHosts(String nonProxyHostString, String hostString) {
        if (nonProxyHostString == null || nonProxyHostString.equals("")) {
            return false;
        }
        String[] nonProxyHosts = nonProxyHostString.split(",");
        for (String currentNonProxyHost : nonProxyHosts) {

            // "*.fedora-commons.org" -> ".*?\.fedora-commons\.org" 
            currentNonProxyHost = currentNonProxyHost.replaceAll("\\.", "\\\\.").replaceAll("\\*", ".*?");
            // a|b|*.c -> (a)|(b)|(.*?\.c)
            currentNonProxyHost = "(" + currentNonProxyHost.replaceAll("\\|", ")|(") + ")";

            try {
                if (Pattern.compile(currentNonProxyHost).matcher(hostString).matches()) {
                    return true;
                }
            } catch (Exception e) {
                LOGGER.error("Creating the nonProxyHosts pattern failed for http.nonProxyHosts={} with the follwing expceiton: ", nonProxyHosts, e);
            }
        }
        return false;
    }

    private void deleteTile(Path tilePath, String deleteReason) {
        if (tilePath.toFile().exists()) {
            try {
                Files.delete(tilePath);
                LOGGER.error(deleteReason, tilePath.toString());
            } catch (IOException ex) {
                LOGGER.error("error during " + deleteReason, tilePath.toString());
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(ex.toString(), ex);
                }
            }
        } else if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Tile {} does not exist, skipping deletion", tilePath.toFile().toString());
        }
    }

    private boolean isInRetentionTime(Path tile) {
        try {
            int retentionTime = configurationBean.getRetentionTime();

            // ir retentionTime is set to 0, disable cache retention and asume file is still in retention time
            if (retentionTime == 0) {
                return true;
            }
            LocalDateTime minFileDate = LocalDateTime.now().minusDays(retentionTime);
            BasicFileAttributes attr = Files.readAttributes(tile, BasicFileAttributes.class
            );
            LocalDateTime lastModifiedDate = LocalDateTime.ofInstant(attr.lastModifiedTime().toInstant(), ZoneId.systemDefault());
            return lastModifiedDate.isAfter(minFileDate);
        } catch (ConfigurationException | IOException ex) {
            // if an erro occurs we cannot say whether the file is 
            // in retention time or not - so we asume yes to keep the file in the cache
            LOGGER.error(ex.getMessage());
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Detailed stacktrace", new Object[]{ex});
            }
            return true;
        }
    }

}
