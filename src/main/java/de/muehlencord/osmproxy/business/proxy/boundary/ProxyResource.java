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
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.imageio.ImageIO;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Produces;
import javax.ws.rs.PathParam;
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

    @EJB
    private ConfigurationBean configurationBean;

    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyResource.class);

    @GET
    @Produces("text/plain")
    @javax.ws.rs.Path("/hello")
    public String hellowWorld() {
        // Return some cliched textual content
        return "Hello World";
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

        if (!ending.equals("png")) {
            return createErrorResponse("png is currently supported only", HttpURLConnection.HTTP_BAD_REQUEST);
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Requesting tile {}/{}/{}/{}.{}", layer, z, x, y, ending);
        }

        Path layerCacheFolder = null;
        try {
            layerCacheFolder = configurationBean.getCacheDirectory(layer);
        } catch (ConfigurationException ex) {
            LOGGER.error(ex.toString());
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(ex.toString(), ex);
            }
            return createErrorResponse("cannot get cache folder from configuration", HttpURLConnection.HTTP_INTERNAL_ERROR, ex);
        }

        // load image from disk
        Path tile = layerCacheFolder
                .resolve(z.toString())
                .resolve(x.toString())
                .resolve(y.toString() + "." + ending);

        Response response;
        if (tile.toFile().exists()) {
            response = respondTileFromDiskCache(tile, layer, z, x, y, ending);
        } else {
            response = respondTileFromUpstreamServer(userAgent, tile, layer, z, x, y, ending);
        }
        response.getHeaders().add ("Access-Control-Allow-Origin", "*"); 
        return response;
    }

    private Response respondTileFromDiskCache(Path tilePath, String layer, Long x, Long y, Long z, String ending) {
        byte[] imageData;
        try {
            BufferedImage image = ImageIO.read(tilePath.toFile());
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            ImageIO.write(image, "png", baos);
            imageData = baos.toByteArray();
        } catch (IOException ex) {
            LOGGER.error(ex.toString(), ex);
            return createErrorResponse("error while reading tile from cache", HttpURLConnection.HTTP_INTERNAL_ERROR);
        }

        // uncomment line below to send non-streamed
        LOGGER.debug("served tile " + layer + "/" + z + "/" + y + "/" + x + "." + ending + " from cache");
        return Response.ok(imageData).type(new MediaType("image", "png")).build();
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

            CloseableHttpClient httpClient = null;

            try {
                URL url = new URL(urlString);
                RequestConfig config = getRequestConfig(url);
                LOGGER.debug("Trying to download tile from upstream server {}", urlString);

                httpClient = HttpClients.createDefault();
                HttpGet httpget = new HttpGet(url.toURI());
                httpget.setConfig(config);

                httpget.setHeader("User-Agent", userAgent);
                httpget.setHeader("Accept-Encoding", "deflate"); // TODO add gzip support
                httpget.setHeader("Cache-Control", "max-age=0");
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
            } finally {
                if (httpClient != null) {
                    try {
                        httpClient.close();
                    } catch (IOException ex) {
                        LOGGER.debug("error while closing httpClient ", ex);
                    }
                }
            }
        }
        return false;
    }

    private Response createErrorResponse(String message, int status) {
        return createErrorResponse(message, status, null);
    }

    private Response createErrorResponse(String message, int status, Exception ex) {
        LOGGER.error("request error: " + message);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(ex.toString(), ex);
        }
        return Response.status(status)
                .entity(message)
                .encoding("UTF-8")
                .type(MediaType.TEXT_PLAIN)
                .build();
    }

    private RequestConfig getRequestConfig(URL url) {
        String httpProxyHost = null;
        String httpProxyPortString = null;
        Integer httpProxyPort = null;
        String urlHostString = url.getProtocol() + "://" + url.getHost();
        String nonProxyHosts = System.getProperty("http.nonProxyHosts");

        HttpHost httpProxy = null;
        if (matchesNonProxyHosts(nonProxyHosts, urlHostString)) {
            LOGGER.debug("Using no proxy, as {} matches nonProxyHosts {}", url.toString(), nonProxyHosts);
        } else {
            LOGGER.debug ("Checking proxy settings"); 
            String prefix;
            if (url.getProtocol().toLowerCase(Locale.US).contains("https")) {
                prefix = "https";
            } else {
                prefix = "http";
            }

            httpProxyHost = System.getProperty(prefix + ".proxyHost");
            LOGGER.debug(prefix + ".proxyHost = {}", httpProxyHost);
            httpProxyPortString = System.getProperty(prefix + ".proxyPort");
            LOGGER.debug(prefix + ".proxyPort = {}", httpProxyPortString);
            if ((httpProxyPortString == null) || (httpProxyPortString.equals(""))) {
                httpProxyPort = 0;
            } else {
                try {
                    httpProxyPort = Integer.parseInt(System.getProperty("https.proxyPort"));
                } catch (NumberFormatException ex) {
                    LOGGER.error("Cannot parse proxy port, {} is not a valid number", httpProxyPortString);
                }
            }

            if (httpProxyHost != null && httpProxyPort != null && !httpProxyPort.equals(0)) {
                httpProxy = new HttpHost(httpProxyHost, httpProxyPort, "http");
                LOGGER.debug("Using proxy {}:{} to connect to {}", httpProxyHost, httpProxyPort, url.toString());
            } else LOGGER.debug ("Using no proxy to connect to {}", url.toString());
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

}
