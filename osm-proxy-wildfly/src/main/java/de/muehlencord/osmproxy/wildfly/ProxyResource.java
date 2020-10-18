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
package de.muehlencord.osmproxy.wildfly;

import de.muehlencord.osmproxy.common.DownloadConfiguration;
import de.muehlencord.osmproxy.common.entity.ConfigurationException;
import de.muehlencord.osmproxy.common.entity.Server;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.file.Path;
import java.util.List;
import javax.ejb.Stateless;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * REST Web Service
 *
 * @author joern.muehlencord
 */
@Stateless
@javax.ws.rs.Path("")
public class ProxyResource {

  private static final Logger logger = LoggerFactory.getLogger(ProxyResource.class);

  @Inject
  ConfigurationBean configurationService;

  @Inject
  WildflyConnectionManager connectionManager;


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
      return createErrorResponse("<layer>/<z>/<x>/<y>.<filetype> parameter is mandatory",
          HttpURLConnection.HTTP_BAD_REQUEST);
    }

    if (z < 0 || (z > 19)) {
      return createErrorResponse("parameter z must be between 0 and 19", HttpURLConnection.HTTP_BAD_REQUEST);
    }

    if (!ending.equals("png")) {
      return createErrorResponse("png is currently supported only", HttpURLConnection.HTTP_BAD_REQUEST);
    }
    if (logger.isDebugEnabled()) {
      logger.debug("Requesting tile {}/{}/{}/{}.{}", layer, z, x, y, ending);
    }

    Path layerCacheFolder;
    try {
      layerCacheFolder = configurationService.getCacheDirectory(layer);
    } catch (ConfigurationException ex) {
      logger.error(ex.getMessage());
      if (logger.isDebugEnabled()) {
        logger.debug(ex.getMessage(), ex);
      }
      return createErrorResponse("cannot get cache folder from configuration", HttpURLConnection.HTTP_INTERNAL_ERROR,
          ex);
    }

    // calculate file to load from disk
    Path tile = layerCacheFolder
        .resolve(z.toString())
        .resolve(x.toString())
        .resolve(y.toString() + "." + ending);

    // make sure a valid user header is set - this is a requirement of the OpenStreeMap fair use policy
    // see https://operations.osmfoundation.org/policies/tiles/
    String finalUserAgent;
    if ((userAgent == null) || ("".equals(userAgent.trim()))) {
      finalUserAgent = "OSMProxy " + configurationService.getVersion();
    } else {
      finalUserAgent = userAgent;
    }

    DownloadConfiguration downloadConfiguration = new DownloadConfiguration()
        .withUserAgent(finalUserAgent)
        .withTilePath(tile)
        .withLayer(layer)
        .withZ(z)
        .withX(x)
        .withY(y)
        .withEnding(ending);

    int retentionTime;
    try {
      retentionTime = configurationService.getRetentionTime();
    } catch (ConfigurationException ex) {
      return createErrorResponse("error reading retentiontime", HttpStatus.SC_INTERNAL_SERVER_ERROR, ex);
    }

    Response response;
    // check if the tile exist
    // if yes and in retention time - serv it from disk
    // if yes and no longer in retention time - delete it from cache and serv it from upstream
    // it not, serv it from upstream
    if (tile.toFile().exists()) {
      if (connectionManager.isInRetentionTime(tile, retentionTime)) {
        response = respondTileFromDiskCache(downloadConfiguration);
      } else {
        connectionManager.deleteTile(tile, "delete outdated file {}");
        response = respondTileFromUpstreamServer(downloadConfiguration);
      }
    } else {
      response = respondTileFromUpstreamServer(downloadConfiguration);
    }
    response.getHeaders().add("Access-Control-Allow-Origin", "*");
    return response;
  }

  private Response respondTileFromDiskCache(DownloadConfiguration downloadConfiguration) {
    byte[] imageData;
    BufferedImage image = null;
    // try to load image from disk
    // if fails, image is broken and file needs to be deleted from disk
    Path tilePath = downloadConfiguration.getTilePath();

    try {
      image = ImageIO.read(tilePath.toFile());
    } catch (IOException ex) {

      if (logger.isDebugEnabled()) {
        logger.debug("Cannot read image from file {}", tilePath);
        if (logger.isDebugEnabled()) {
          logger.debug(ex.getMessage(), ex);
        }
      }
    }
    if (image == null) {
      if (logger.isDebugEnabled()) {
        logger.debug("Cannot construct image from file {}, going to delete file", tilePath);
      }
      connectionManager.deleteTile(tilePath, "Deleted broken file {}");
      return createErrorResponse("error while reading tile from cache", HttpURLConnection.HTTP_INTERNAL_ERROR);
    } else {
      try {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        imageData = baos.toByteArray();
      } catch (IOException ex) {
        if (logger.isDebugEnabled()) {
          logger.debug(ex.toString(), ex);
        }
        return createErrorResponse("error while reading tile from cache", HttpURLConnection.HTTP_INTERNAL_ERROR);
      }
      logger.debug("served tile {}/{}/{}/{}.{} from cache", downloadConfiguration.getLayer(),
          downloadConfiguration.getZ(), downloadConfiguration.getX(), downloadConfiguration.getY(),
          downloadConfiguration.getEnding());
      return Response.ok(imageData).type(new MediaType("image", "png")).build();
    }
  }

  private Response respondTileFromUpstreamServer(DownloadConfiguration downloadConfiguration) {
    // file does not exists, try to get it from upstream server
    Path tilePath = downloadConfiguration.getTilePath();
    if (!tilePath.toFile().getParentFile().exists()) {
      tilePath.toFile().getParentFile().mkdirs();
    }

    List<Server> upstreamServers;
    try {
      upstreamServers = configurationService.getUpstreamServer(downloadConfiguration.getLayer());
    } catch (ConfigurationException ex) {
      return createErrorResponse("error reading upstream servers from configuration",
          HttpURLConnection.HTTP_INTERNAL_ERROR, ex);
    }

    boolean fileDownloaded;
    try {
      fileDownloaded = connectionManager.downloadFromUpStreamServer(upstreamServers, downloadConfiguration);

    } catch (ConfigurationException ex) {
      if (logger.isDebugEnabled()) {
        logger.debug(ex.getMessage(), ex);
      }
      return createErrorResponse(ex.getMessage(), HttpURLConnection.HTTP_INTERNAL_ERROR, ex);
    }
    if (fileDownloaded) {
      if (tilePath.toFile().exists()) {
        return respondTileFromDiskCache(downloadConfiguration);
      } else {
        logger.error("tile {}/{}/{}/{}.{} downloaded, but not available in cache",
            downloadConfiguration.getLayer(), downloadConfiguration.getZ(), downloadConfiguration.getX(),
            downloadConfiguration.getY(), downloadConfiguration.getEnding());
        return createErrorResponse("file downloaded but not available in cache", HttpURLConnection.HTTP_INTERNAL_ERROR);
      }
    } else {
      logger.debug("tile {}/{}/{}/{}.{} not downloaded", downloadConfiguration.getLayer(), downloadConfiguration.getZ(),
          downloadConfiguration.getX(), downloadConfiguration.getY(), downloadConfiguration.getEnding());
      return createErrorResponse("tile not available", HttpURLConnection.HTTP_NOT_FOUND);
    }
  }

  private Response createErrorResponse(String message, int status) {
    return createErrorResponse(message, status, null);
  }

  private Response createErrorResponse(String message, int status, Exception ex) {
    logger.error("request error: {}", message);
    if (ex != null && logger.isDebugEnabled()) {
      logger.debug(ex.toString(), ex);
    }

    return Response.status(status)
        .entity(message)
        .encoding("UTF-8")
        .type(MediaType.TEXT_PLAIN)
        .build();
  }


}
