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
import java.util.Objects;

/**
 * @author joern.muehlencord
 */
public class Layer {

  @Expose
  private String name;

  @Expose
  private String cacheFolder;

  @Expose
  private List<Server> upstream;

  public Layer(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public Layer setName(String name) {
    this.name = name;
    return this;
  }

  public List<Server> getUpstream() {
    return upstream;
  }

  public Layer setUpstreamServers(List<Server> upstream) {
    this.upstream = upstream;
    return this;
  }

  public Layer addUpstreamServer(Server upstreamServer) {
    if (this.upstream == null) {
      this.upstream = new ArrayList<>();
    }
    this.upstream.add(upstreamServer);
    return this;
  }

  public String getCacheFolder() {
    return cacheFolder;
  }

  public Layer setCacheFolder(String cacheFolder) {
    this.cacheFolder = cacheFolder;
    return this;
  }

  @Override
  public int hashCode() {
    int hash = 3;
    hash = 67 * hash + Objects.hashCode(this.name);
    hash = 67 * hash + Objects.hashCode(this.cacheFolder);
    hash = 67 * hash + Objects.hashCode(this.upstream);
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
    final Layer other = (Layer) obj;
    if (!Objects.equals(this.name, other.name)) {
      return false;
    }
    if (!Objects.equals(this.cacheFolder, other.cacheFolder)) {
      return false;
    }
    return Objects.equals(this.upstream, other.upstream);
  }

}
