package de.muehlencord.osmproxy.business.config.entity;

import com.google.gson.annotations.Expose;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author joern.muehlencord
 */
public class Configuration {
    
    @Expose
    private Cache cache;

    @Expose
    private Map<String, Layer> layerMap;

    /* *** getter / setter *** */


    public void addLayer(Layer layer) {
        if (this.layerMap == null) {
            layerMap = new ConcurrentHashMap<>();
        }
        layerMap.put(layer.getName(), layer);
    }

    public Layer getLayer(String layerName) {
        if (layerMap.containsKey(layerName)) {
            return layerMap.get(layerName);
        } else {
            return null;
        }
    }

    public Cache getCache() {
        return cache;
    }

    public void setCache(Cache cache) {
        this.cache = cache;
    }
    
    
    /* equals / hashCode */

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 23 * hash + Objects.hashCode(this.cache);
        hash = 23 * hash + Objects.hashCode(this.layerMap);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Configuration other = (Configuration) obj;
        if (!Objects.equals(this.cache, other.cache)) {
            return false;
        }
        if (!Objects.equals(this.layerMap, other.layerMap)) {
            return false;
        }
        return true;
    }


}
