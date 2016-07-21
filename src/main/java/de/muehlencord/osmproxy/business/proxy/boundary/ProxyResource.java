package de.muehlencord.osmproxy.business.proxy.boundary;

import de.muehlencord.osmproxy.ConfigurationBean;
import de.muehlencord.osmproxy.business.config.entity.ConfigurationException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.imageio.ImageIO;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
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
    @Produces("image/png")
    @javax.ws.rs.Path("/{layer}/{z}/{x}/{y}.{ending}")
    public Response getTile(
            @PathParam("layer") String layer,
            @PathParam("z") Long z,
            @PathParam("x") Long x,
            @PathParam("y") Long y,
            @PathParam("ending") String ending) {

        if ((layer == null) || (x == null) || (y == null) || (z == null) || (ending == null)) {
            throw new WebApplicationException(
                    Response.status(HttpURLConnection.HTTP_BAD_REQUEST)
                            .entity("z/x/y parameter is mandatory")
                            .build()
            );
        }

        if (!ending.equals("png")) {
            throw new WebApplicationException(
                    Response.status(HttpURLConnection.HTTP_BAD_REQUEST)
                            .entity("only png is currently supported")
                            .build()
            );
        }

        LOGGER.debug("Requesting tile {}/{}/{}/{}.{}", layer, z, x, y, ending);

        Path layerCacheFolder;
        try {
            layerCacheFolder = configurationBean.getCacheDirectory(layer);
        } catch (ConfigurationException ex) {
            LOGGER.error(ex.toString());
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(ex.toString(), ex);
            }
            throw new WebApplicationException(
                    Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR)
                            .entity(ex.getMessage())
                            .build()
            );
        }

        // load image from disk
        Path tile = layerCacheFolder
                .resolve(z.toString())
                .resolve(x.toString())
                .resolve(y.toString() + "." + ending);

        if (tile.toFile().exists()) {
            return respondTileFromDiskCache(tile, layer, z, x, y, ending);
        } else {
            return respondTileFromUpstreamServer(tile, layer, z, x, y, ending);

        }
    }

    private Response respondTileFromDiskCache(Path tilePath, String layer, Long x, Long y, Long z, String ending) {
        byte[] imageData = null;
        try {
            BufferedImage image = ImageIO.read(tilePath.toFile());
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            ImageIO.write(image, "png", baos);
            imageData = baos.toByteArray();
        } catch (IOException ex) {
            LOGGER.error(ex.toString(), ex);
            throw new WebApplicationException(
                    Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR)
                            .entity("error while reading tile from cache")
                            .build()
            );
        }

        // uncomment line below to send non-streamed
        LOGGER.debug("served tile " + layer + "/" + z + "/" + y + "/" + x + "." + ending + " from cache");
        return Response.ok(imageData).build();
        // uncomment line below to send streamed
        // return Response.ok(new ByteArrayInputStream(imageData)).build();
    }

    private Response respondTileFromUpstreamServer(Path tilePath, String layer, Long z, Long x, Long y, String ending) {
        // file does not exis, try to get it from upstream server
        if (!tilePath.toFile().getParentFile().exists()) {
            tilePath.toFile().getParentFile().mkdirs();
        }

        boolean fileDownloaded = downloadFromUpStreamServer(tilePath, layer, z, x, y, ending);
        if (fileDownloaded) {
            if (tilePath.toFile().exists()) {
                return respondTileFromDiskCache(tilePath, layer, z, x, y, ending);
            } else {
                LOGGER.error("tile {}/{}/{}/{}.{} downloaded, but not available in cache", layer, z, x, y, ending);
                throw new WebApplicationException(
                        Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR)
                                .entity("file downloaded by not available in cache")
                                .build()
                );
            }
        } else {
            LOGGER.error("tile {}/{}/{}/{}.{} not downloaded", layer, z, x, y, ending);
            throw new WebApplicationException(
                    Response.status(HttpURLConnection.HTTP_NOT_FOUND)
                            .entity("file downloaded by not available in cache")
                            .build()
            );
        }
    }

    private boolean downloadFromUpStreamServer(Path tilePath, String layer, long z, long x, long y, String ending) {
        String urlString = "http://a.tile.openstreetmap.org/" + z + "/" + x + "/" + y + "." + ending;
        CloseableHttpClient httpClient = null;

        HttpHost proxy = new HttpHost("proxy.wincor-nixdorf.com", 81, "http");
        RequestConfig config = RequestConfig.custom()
                .setProxy(proxy)
                .build();

        try {
            URL url = new URL(urlString);
            LOGGER.debug("Trying to download tile from upstream server {}", urlString);

            httpClient = HttpClients.createDefault();
            HttpGet httpget = new HttpGet(url.toURI());
            httpget.setConfig(config);
            httpget.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:47.0) Gecko/20100101 Firefox/47.0");
            httpget.setHeader("Accept-Encoding", "deflate");
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

}
