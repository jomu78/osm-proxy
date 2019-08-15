/*
 * Copyright 2019 joern.muehlencord.
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
package de.muehlencord.osmproxy.business.proxy.control;

import de.muehlencord.osmproxy.business.config.entity.Server;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.AccessTimeout;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author joern.muehlencord
 */
@Singleton
@Startup
@AccessTimeout(value = 60000)
public class ConnectionManager implements Serializable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionManager.class);

    private PoolingHttpClientConnectionManager connectionManager;
    private final Map<String, RequestConfig> requestConfigMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(20);
        connectionManager.setDefaultMaxPerRoute(2);
    }

    @PreDestroy
    public void shutdown() {
        connectionManager.shutdown();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Connection manager shutdown");
        }

    }

    @Lock(LockType.READ)
    public HttpClientConnectionManager getConnectionManager() {
        return connectionManager;
    }

    @Lock(LockType.READ)
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

    @Lock(LockType.READ)
    public void executeDownload(Server currentServer, String userAgent, String urlString, Path tilePath) throws MalformedURLException, URISyntaxException, IOException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Trying to download tile from upstream server {}", urlString);
        }
        URL url = new URL(urlString);
        RequestConfig config = getRequestConfig(url);
        HttpGet httpget = new HttpGet(url.toURI());
        httpget.setConfig(config);

        // check if userAgent is set for server, if yes, use this one
        // if not, use userAgent from client (default)
        if (currentServer.getUserAgent() == null) {
            httpget.setHeader("User-Agent", userAgent);
        } else {
            httpget.setHeader("User-Agent", currentServer.getUserAgent());
        }
        httpget.setHeader("Accept-Encoding", "gzip,deflate");
        httpget.setHeader("Accept-Language", "en-US");

        CloseableHttpClient httpClient = HttpClients.custom().setConnectionManager(connectionManager).build();
        HttpResponse response = httpClient.execute(httpget);
        HttpEntity entity = response.getEntity();
        if (entity != null) {
            saveFile(entity, tilePath);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("stored {} as {}", urlString, tilePath.toString());
            }
        }
    }

    private void saveFile(HttpEntity entity, Path tilePath) throws IOException {
        InputStream is = entity.getContent();
        FileOutputStream fos = new FileOutputStream(tilePath.toFile());
        int inByte;
        while ((inByte = is.read()) != -1) {
            fos.write(inByte);
        }
    }

}
