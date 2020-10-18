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
package de.muehlencord.osmproxy.common.entity;

import com.google.gson.annotations.Expose;
import java.util.ArrayList;
import java.util.List;
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
    
    public List<String> getAllLayers() {
        List<String> resultList = new ArrayList<>();
        resultList.addAll (layerMap.keySet());        
        return resultList;        
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
        return Objects.equals(this.layerMap, other.layerMap);
    }


}
