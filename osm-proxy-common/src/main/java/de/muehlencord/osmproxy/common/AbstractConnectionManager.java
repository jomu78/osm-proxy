package de.muehlencord.osmproxy.common;

import de.muehlencord.osmproxy.common.entity.ConfigurationException;
import de.muehlencord.osmproxy.common.entity.Server;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.io.CloseMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Joern Muehlencord, 2020-09-10
 * @since 1.1.0
 */
public abstract class AbstractConnectionManager implements ConnectionManager {

  private static final Logger logger = LoggerFactory.getLogger(AbstractConnectionManager.class);

  protected PoolingHttpClientConnectionManager connectionManager;
  protected final Map<String, RequestConfig> requestConfigMap = new ConcurrentHashMap<>();


  public RequestConfig getRequestConfig(URL url) {
    String urlHostString = url.getProtocol() + "://" + url.getHost();
    if (requestConfigMap.containsKey(urlHostString)) {
      return requestConfigMap.get(urlHostString);
    } else {
      RequestConfig requestConfig = createRequestConfig(url);
      requestConfigMap.put(urlHostString, requestConfig);
      return requestConfig;
    }
  }

  private RequestConfig createRequestConfig(URL url) {
    String httpProxyHost;
    String httpProxyPortString;
    Integer httpProxyPort = null;
    String urlHostString = url.getProtocol() + "://" + url.getHost();
    String nonProxyHosts = System.getProperty("http.nonProxyHosts");

    HttpHost httpProxy = null;
    if (matchesNonProxyHosts(nonProxyHosts, urlHostString)) {
      if (logger.isDebugEnabled()) {
        logger.debug("Using no proxy, as {} matches nonProxyHosts {}", url, nonProxyHosts);
      }

    } else {
      if (logger.isDebugEnabled()) {
        logger.debug("Checking proxy settings");
      }
      String prefix;
      if (url.getProtocol().toLowerCase(Locale.US).contains("https")) {
        prefix = "https";
      } else {
        prefix = "http";
      }

      httpProxyHost = System.getProperty(prefix + ".proxyHost");
      if (logger.isDebugEnabled()) {
        logger.debug("{}.proxyHost = {}", prefix, httpProxyHost);
      }
      httpProxyPortString = System.getProperty(prefix + ".proxyPort");
      if (logger.isDebugEnabled()) {
        logger.debug("{}.proxyPort = {}", prefix, httpProxyPortString);
      }
      if ((httpProxyPortString == null) || (httpProxyPortString.equals(""))) {
        httpProxyPort = 0;
      } else {
        try {
          httpProxyPort = Integer.parseInt(httpProxyPortString.trim());
        } catch (NumberFormatException ex) {
          logger.error("Cannot parse proxy port, {} is not a valid number", httpProxyPortString);
        }
      }

      if (httpProxyHost != null && httpProxyPort != null && !httpProxyPort.equals(0)) {
        httpProxy = new HttpHost("http", httpProxyHost, httpProxyPort);
        if (logger.isDebugEnabled()) {
          logger.debug("Using proxy {}:{} to connect to {}", httpProxyHost, httpProxyPort, url);
        }
      } else {
        if (logger.isDebugEnabled()) {
          logger.debug("Using no proxy to connect to {}", url);
        }
      }
    }

    return RequestConfig.custom().setProxy(httpProxy).build();
  }

  private boolean matchesNonProxyHosts(String nonProxyHostString, String hostString) {
    if (nonProxyHostString == null || nonProxyHostString.equals("")) {
      return false;
    }
    String[] nonProxyHosts = nonProxyHostString.split(",");
    for (String currentNonProxyHost : nonProxyHosts) {

      // "*.fedora-commons.org" -> ".*?\.fedora-commons\.org"
      currentNonProxyHost = currentNonProxyHost.replace("\\.", "\\\\.").replace("\\*", ".*?");
      // a|b|*.c -> (a)|(b)|(.*?\.c)
      currentNonProxyHost = "(" + currentNonProxyHost.replace("\\|", ")|(") + ")";

      try {
        if (Pattern.compile(currentNonProxyHost).matcher(hostString).matches()) {
          return true;
        }
      } catch (Exception e) {
        logger
            .error("Creating the nonProxyHosts pattern failed for http.nonProxyHosts={} with the follwing expceiton: ",
                nonProxyHosts, e);
      }
    }
    return false;
  }

  @Override
  public void executeDownload(Server currentServer, String userAgent, String acceptHeader, String urlString, Path tilePath)
      throws URISyntaxException, IOException {
    if (logger.isDebugEnabled()) {
      logger.debug("Trying to download tile from upstream server {}", urlString);
    }
    URL url = new URL(urlString);
    RequestConfig config = getRequestConfig(url);
    HttpGet httpget = new HttpGet(url.toURI());
    httpget.setConfig(config);

    // check if userAgent is set for server (in config file)
    // if not, use the given userAgent. this is either the original user request (e.g. from the browser) or the default application one
    if (currentServer.getUserAgent() != null) {
      httpget.setHeader("User-Agent", currentServer.getUserAgent());
    } else {
      httpget.setHeader("User-Agent", userAgent);
    }
    httpget.setHeader("Accept-Encoding", "gzip,deflate");
    httpget.setHeader("Accept-Language", "en-US");
    httpget.setHeader("accept", acceptHeader);

    CloseableHttpClient httpClient = HttpClients.custom().setConnectionManager(connectionManager).build();
    CloseableHttpResponse response = httpClient.execute(httpget);
    int status = response.getCode();
    if ((status >= 100) && (status <= 226)) {
      HttpEntity entity = response.getEntity();
      if (entity != null) {
        saveFile(entity, tilePath);
        if (logger.isDebugEnabled()) {
          logger.debug("stored {} as {}", urlString, tilePath);
        }
      }
    }
  }

  private void saveFile(HttpEntity entity, Path tilePath) throws IOException {
    InputStream is = entity.getContent();
    try (FileOutputStream fos = new FileOutputStream(tilePath.toFile())) {
      int inByte;
      while ((inByte = is.read()) != -1) {
        fos.write(inByte);
      }
    }
  }

  public void deleteTile(Path tilePath, String deleteReason) {
    if (tilePath.toFile().exists()) {
      try {
        Files.delete(tilePath);
        if (logger.isDebugEnabled()) {
          logger.debug(deleteReason, tilePath);
        }
      } catch (IOException ex) {
        logger.error("error during deleting {} caused by {}", tilePath, deleteReason);
        if (logger.isDebugEnabled()) {
          logger.debug(ex.getMessage(), ex);
        }
      }
    } else if (logger.isTraceEnabled()) {
      logger.trace("Tile {} does not exist, skipping deletion", tilePath.toFile());
    }
  }

  public boolean isInRetentionTime(Path tile, int retentionTime) {
    try {

      // ir retentionTime is set to 0, disable cache retention and assume file is still in retention time
      if (retentionTime == 0) {
        return true;
      }
      LocalDateTime minFileDate = LocalDateTime.now().minusDays(retentionTime);
      BasicFileAttributes attr = Files.readAttributes(tile, BasicFileAttributes.class);
      LocalDateTime lastModifiedDate = LocalDateTime
          .ofInstant(attr.lastModifiedTime().toInstant(), ZoneId.systemDefault());
      return lastModifiedDate.isAfter(minFileDate);
    } catch (IOException ex) {
      logger.error(ex.getMessage());
      if (logger.isDebugEnabled()) {
        logger.debug(ex.getMessage(), ex);
      }
      return true;
    }
  }

  protected void shutdown() {
    connectionManager.close(CloseMode.GRACEFUL);
    if (logger.isDebugEnabled()) {
      logger.debug("Connection manager shutdown");
    }
  }

  public boolean downloadFromUpStreamServer(List<Server> upstreamServer, DownloadConfiguration downloadConfig)
      throws ConfigurationException {
    for (Server currentServer : upstreamServer) {
      String urlString = currentServer.getUrl();
      urlString = urlString.replace("{layer}", downloadConfig.getLayer());
      urlString = urlString.replace("{z}", Long.toString(downloadConfig.getZ()));
      urlString = urlString.replace("{x}", Long.toString(downloadConfig.getX()));
      urlString = urlString.replace("{y}", Long.toString(downloadConfig.getY()));
      urlString = urlString.replace("{ending}", downloadConfig.getEnding());
      try {
        executeDownload(currentServer, downloadConfig.getUserAgent(), downloadConfig.getAcceptHeader(), urlString, downloadConfig.getTilePath());
        return true;
      } catch (URISyntaxException | IOException ex) {
        throw new ConfigurationException("cannot construct URL for upstream server.", ex);
      }
    }

    return false;
  }

}
