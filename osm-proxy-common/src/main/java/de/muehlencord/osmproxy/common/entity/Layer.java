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
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author joern.muehlencord
 */
@Data
@NoArgsConstructor
public class Layer {

  private String name;
  private String cacheFolder;
  private List<Server> upstream;

  public Layer(String name) {
    this.name = name;
  }

  public Layer addUpstreamServer(Server upstreamServer) {
    if (this.upstream == null) {
      this.upstream = new ArrayList<>();
    }
    this.upstream.add(upstreamServer);
    return this;
  }


  public Layer withName(String name) {
    this.name = name;
    return this;
  }

  public Layer withCacheFolder(String cacheFolder) {
    this.cacheFolder = cacheFolder;
    return this;
  }

  public Layer withUpstream(List<Server> upstream) {
    this.upstream = upstream;
    return this;
  }
}
