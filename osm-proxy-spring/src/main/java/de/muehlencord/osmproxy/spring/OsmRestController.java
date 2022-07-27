package de.muehlencord.osmproxy.spring;

import de.muehlencord.osmproxy.common.DownloadConfiguration;
import de.muehlencord.osmproxy.common.entity.ConfigurationException;
import de.muehlencord.osmproxy.common.entity.Server;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * @author Joern Muehlencord, 2020-09-10
 * @since 1.1.0
 */
@RestController
public class OsmRestController {

  private static final Logger logger = LoggerFactory.getLogger(OsmRestController.class);

  private final VersionService versionService;
  private final ConfigurationService configurationService;
  private final ConnectionManager connectionManager;

  public OsmRestController(VersionService versionService, ConnectionManager connectionManager, ConfigurationService configurationService) {
    this.versionService = versionService;
    this.connectionManager = connectionManager;
    this.configurationService = configurationService;
  }

  @GetMapping(value = "/rest/{layer}/{z}/{x}/{y}.{ending}", produces = { "image/png", "text/plain" })
  public ResponseEntity<Resource> getTile(
      final @RequestHeader("user-agent") String userAgent,
      final @RequestHeader("accept") String acceptHeader,
      final @PathVariable("layer") String layer,
      final @PathVariable("z") Long z,
      final @PathVariable("x") Long x,
      final @PathVariable("y") Long y,
      final @PathVariable("ending") String ending) {

    if ((layer == null) || (x == null) || (y == null) || (z == null) || (ending == null)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "<layer>/<z>/<x>/<y>.<filetype> parameter is mandatory");
    }

    if (z < 0 || (z > 19)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "parameter z must be between 0 and 19");
    }

    if (!ending.equals("png")) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "png is currently supported only");
    }
    logger.debug("Requesting tile {}/{}/{}/{}.{}", layer, z, x, y, ending);

    Path layerCacheFolder;
    try {
      layerCacheFolder = configurationService.getCacheDirectory(layer);
    } catch (ConfigurationException ex) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "cannot get cache folder from configuration", ex);
    }

    // calculate file to load from disk
    Path tile = layerCacheFolder
        .resolve(z.toString())
        .resolve(x.toString())
        .resolve(y + "." + ending);

    // make sure a valid user header is set - this is a requirement of the OpenStreeMap fair use policy
    // see https://operations.osmfoundation.org/policies/tiles/
    String finalUserAgent;
    if ((userAgent == null) || ("".equals(userAgent.trim()))) {
      finalUserAgent = "OSMProxy " + versionService.getVersion();
    } else {
      finalUserAgent = userAgent;
    }

    String finalAcceptHeader;
    if ((acceptHeader == null) || ("".equals(acceptHeader.trim()))) {
      finalAcceptHeader = "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9";
    } else {
      finalAcceptHeader = acceptHeader;
    }

    DownloadConfiguration downloadConfiguration = new DownloadConfiguration()
        .withUserAgent(finalUserAgent)
        .withAcceptHeader(finalAcceptHeader)
        .withLayer(layer)
        .withTilePath(tile)
        .withZ(z)
        .withX(x)
        .withY(y)
        .withEnding(ending);

    int retentionTime;
    try {
      retentionTime = configurationService.getRetentionTime();
    } catch (ConfigurationException ex) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
          "cannot get retention time from configuration", ex);
    }

    List<Server> upstreamServers;
    try {
      upstreamServers = configurationService.getUpstreamServer(layer);
    } catch (ConfigurationException ex) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
          "error reading upstream servers from configuration", ex);
    }

    ResponseEntity<Resource> response;
    // check if the tile exist
    // if yes and in retention time - serv it from disk
    // if yes and no longer in retention time - delete it from cache and serv it from upstream
    // it not, serv it from upstream
    if (tile.toFile().exists()) {
      if (connectionManager.isInRetentionTime(tile, retentionTime)) {
        response = respondTileFromDiskCache(downloadConfiguration);
      } else {
        connectionManager.deleteTile(tile, "delete outdated file {}");
        response = respondTileFromUpstreamServer(upstreamServers, downloadConfiguration);
      }
    } else {
      response = respondTileFromUpstreamServer(upstreamServers, downloadConfiguration);
    }
    return response;
  }

  private ResponseEntity<Resource> respondTileFromDiskCache(DownloadConfiguration downloadConfiguration) {
    byte[] imageData;
    BufferedImage image = null;
    Path tilePath = downloadConfiguration.getTilePath();
    // try to load image from disk
    // if fails, image is broken and file needs to be deleted from disk
    try {
      image = ImageIO.read(tilePath.toFile());
    } catch (IOException ex) {
      logger.error("Cannot read image from file {}", tilePath);
      logger.debug(ex.getMessage(), ex);
    }
    if (image == null) {
      logger.debug("Cannot construct image from file {}, going to delete file", tilePath);
      connectionManager.deleteTile(tilePath, "Deleted broken file {}");
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "error while reading tile from cache");
    } else {
      try {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        imageData = baos.toByteArray();
      } catch (IOException ex) {
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "error while reading tile from cache", ex);
      }
      logger.debug("served tile {}/{}/{}/{}.{} from cache", downloadConfiguration.getLayer(),
          downloadConfiguration.getZ(), downloadConfiguration.getX(), downloadConfiguration.getY(),
          downloadConfiguration.getEnding());
      Resource resource = new ByteArrayResource(imageData);
      return new ResponseEntity<>(resource, new HttpHeaders(), HttpStatus.OK);
    }
  }

  private ResponseEntity<Resource> respondTileFromUpstreamServer(List<Server> upstreamServers,
      DownloadConfiguration downloadConfiguration) {
    // file does not exist, try to get it from upstream server
    if (!downloadConfiguration.getTilePath().toFile().getParentFile().exists()) {
      downloadConfiguration.getTilePath().toFile().getParentFile().mkdirs();
    }

    boolean fileDownloaded;
    try {
      fileDownloaded = connectionManager.downloadFromUpStreamServer(upstreamServers, downloadConfiguration);
    } catch (ConfigurationException ex) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "error loading tile from upstream server", ex);
    }
    if (fileDownloaded) {
      if (downloadConfiguration.getTilePath().toFile().exists()) {
        return respondTileFromDiskCache(downloadConfiguration);
      } else {
        logger.error("tile {}/{}/{}/{}.{} downloaded, but not available in cache",
            downloadConfiguration.getLayer(), downloadConfiguration.getZ(), downloadConfiguration.getX(),
            downloadConfiguration.getY(), downloadConfiguration.getEnding());
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "file downloaded but not available in cache");
      }
    } else {
      logger.debug("tile {}/{}/{}/{}.{} not downloaded", downloadConfiguration.getLayer(),
          downloadConfiguration.getZ(), downloadConfiguration.getX(), downloadConfiguration.getY(),
          downloadConfiguration.getEnding());
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "tile not available");
    }
  }

}
