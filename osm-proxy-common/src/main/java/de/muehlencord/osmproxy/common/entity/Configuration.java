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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Data;

/**
 * @author joern.muehlencord
 */
@Data
public class Configuration {

  private Cache cache;
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
    resultList.addAll(layerMap.keySet());
    return resultList;
  }
}
