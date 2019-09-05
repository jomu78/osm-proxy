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
import de.muehlencord.osmproxy.business.proxy.control.ConnectionManager;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Iterator;
import java.util.List;
import javax.ejb.Stateless;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * REST Web Service
 *
 * @author joern.muehlencord
 */
@Stateless
@javax.ws.rs.Path("")
public class ProxyResource implements Serializable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyResource.class);

    @Inject
    private ConfigurationBean configurationBean;

    @Inject
    ConnectionManager connectionManager;

    @GET
    @Produces({"image/png", "text/plain"})
    @javax.ws.rs.Path("/{layer}/{z}/{x}/{y}.{ending}")
    public Response getTile(
            final @HeaderParam("user-agent") String userAgent,
            final @PathParam("layer") String layer,
            final @PathParam("z") Long z,
            final @PathParam("x") Long x,
            final @PathParam("y") Long y,
            final @PathParam("ending") String ending) {

        if ((layer == null) || (x == null) || (y == null) || (z == null) || (ending == null)) {
            return createErrorResponse("<layer>/<z>/<x>/<y>.<filetype> parameter is mandatory", HttpURLConnection.HTTP_BAD_REQUEST);
        }

        if (z < 0 || (z > 19)) {
            return createErrorResponse("parameter z must be between 0 and 19", HttpURLConnection.HTTP_BAD_REQUEST);
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
        
        // make sure a valid user header is set - this is a requirement of the OpenStreeMap fair use policy
        // see https://operations.osmfoundation.org/policies/tiles/
        String finalUserAgent;
        if ((userAgent == null) || ("".equals (userAgent.trim()))) {
            finalUserAgent = "OSMProxy "+configurationBean.getVersion();
        } else {
            finalUserAgent = userAgent;
        }

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
                response = respondTileFromUpstreamServer(finalUserAgent, tile, layer, z, x, y, ending);
            }
        } else {
            response = respondTileFromUpstreamServer(finalUserAgent, tile, layer, z, x, y, ending);
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
                connectionManager.executeDownload(currentServer, userAgent, urlString, tilePath);
                return true;
            } catch (URISyntaxException | IOException ex) {
                LOGGER.error("cannot construct URL for upstream server. ", urlString);
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Detailed stacktrace", new Object[]{ex});
                }
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

    private void deleteTile(Path tilePath, String deleteReason) {
        if (tilePath.toFile().exists()) {
            try {
                Files.delete(tilePath);
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(deleteReason, tilePath.toString());
                }
            } catch (IOException ex) {
                LOGGER.error("error during " + deleteReason, tilePath.toString());
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Detailed stacktrace", new Object[]{ex});
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
            // if an error occurs we cannot say whether the file is 
            // in retention time or not - so we asume yes to keep the file in the cache
            LOGGER.error(ex.getMessage());
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Detailed stacktrace", new Object[]{ex});
            }
            return true;
        }
    }

}
