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

import lombok.Data;

/**
 * @author joern.muehlencord
 */
@Data
public class Cache {

  private String name;
  private String path;
  private Integer retentionTime;

  public Cache() {
    this.name = null;
    this.path = null;
    this.retentionTime = 0; // default retention time of 0 - disable cache expiry
  }

  public Cache(String name) {
    this.name = name;
    this.path = null;
    this.retentionTime = 0; // default retention time of 0 - disable cache expiry
  }

  public Cache(String name, String path) {
    this.name = name;
    this.path = path;
  }

  public Cache(String name, String path, int retentionTime) {
    this.name = name;
    this.path = path;
    this.retentionTime = retentionTime;
  }
}
