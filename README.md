# OSMProxy

OSMProxy is a simple caching [OpenStreetMap](https://www.openstreetmap.org) proxy. Instead of directly requesting tiles from the OpenStreetMap servers, you can configure any application which requires
to connect to OpenStreetMap servers to this application.
When a tile is requsted, the application checks whether the tile is available in the local cache. If the tile is not available, it is requested from OpenStreetMap server first.

[![][Build Status img]][Build Status]
[![][Dependency Status img]][Dependency Status]
[![][license img]][license]
[![][Maven Central img]][Maven Central]
[![][Javadocs img]][Javadocs]


## Prerequisites
The application requires a Java-Web-Application service running on Java 11 at minimum. It has been tested with the following servers

* WildFly 16
* WildFly 17

## Installation
There is no configuration on the Application Server required. The configuration is done in a simple JSON-based configuration file which is located in the home directory of the user
running the application server in $HOME/.osmproxy/osmproxy.cfg. 

```json
{
  "cache": {
    "name": "disk",
    "retentionTime": 180
  },
  "layerMap": {
    "tiles": {
      "name": "tiles",
      "cacheFolder": "tiles",
      "upstream": [
        {
          "name": "openstreetmap",
          "url": "http://tile.openstreetmap.org/{z}/{x}/{y}.png"		  
        }
      ]
    }
  }
}
```

The above configuration defines a disk cache located at $HOME/.osmproxy/cache/. Files older than 180 days will be refreshed from OpenStreetMap server when requested again.
The tiles received from main OpenStreetMap server are stored in $HOME/.osmproxy/cache/tiles. 

Once the configuration file is in place the osmproxy.war file can be deployed to the application server. A map for testing will be served at http(s)://yoursrever:port/osmproxy/. 

## Configuring your application

In order to make use of OSMProxy you need to configure your application to request tiles from OSMProxy rather than OpenStreetMap servers directly. 
Asumption for the following example

* your application is configured to communicate to [https://tile.openstreetmap.org/](https://tile.openstreetmap.org/) 
* configuration as shown above
* OSMProxy is running on [https://localhost:8443/osmproxy/](https://localhost:8443/osmproxy/). 

You need to configure your application to communicate to 
[https://localhost:8443/osmproxy/rest/tiles/](https://localhost:8443/osmproxy/rest/tiles/)

For some configurations you need to specify the zoom level (z) and the x/y coordinates of the tiles to request. These are apended to the URL the sameway as for OpenStreetMap. 
The full configuration URL is 
```
  <scheme>://<yourserver>:<port>/osmproxy/rest/<layername>/<z>/<x>/<y>.png
```


## Built With

* [Apache HttpComponents](https://hc.apache.org/) - For upstream requests to OpenStreetMap
* [OpenLayers](https://github.com/openlayers/openlayers/) - Provision of example map
* [Maven](https://maven.apache.org/) - Dependency Management


## Author

* **Joern Muehlencord** - [jomu78](https://github.com/jomu78)


## License

This project is licensed under the Apache 2.0 License - see the [LICENSE.txt](LICENSE.txt) file for details

## OpenStreetMap Acceptable Use Policy

**Important:** Please ensure you have read and understood the [OpenStreetMap Fair Use Policy](https://wiki.openstreetmap.org/wiki/Acceptable_Use_Policy), 
especially the [Tile Usage Policy](https://operations.osmfoundation.org/policies/tiles/). 

OSMProxy helps to fullfill the requirements where possible:

* it forwards the original user agent if available. If no header is set, OSMProxy will use its own User-Agent header. 
* does not send no-cache-headers
* caches tiles as per configuration
* only uses 2 download threads



[Build Status]:https://travis-ci.org/jomu78/osm-proxy
[Build Status img]:https://travis-ci.org/jomu78/osm-proxy.svg?branch=master

[Dependency Status]:https://snyk.io/test/github/jomu78/osm-proxy
[Dependency Status img]:https://snyk.io/test/github/jomu78/osm-proxy/badge.svg?style=flat

[license]:LICENSE
[license img]:https://img.shields.io/badge/license-Apache%202-blue.svg

[Maven Central]:https://maven-badges.herokuapp.com/maven-central/de.muehlencord/osm-proxy
[Maven Central img]:https://maven-badges.herokuapp.com/maven-central/de/muehlencord/osm-proxy/badge.svg

[Javadocs]:http://www.javadoc.io/doc/de.muehlencord/osm-proxy
[Javadocs img]:http://javadoc.io/badge/de.muehlencord/osm-proxy.svg 
