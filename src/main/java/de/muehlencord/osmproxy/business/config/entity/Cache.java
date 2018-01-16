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
package de.muehlencord.osmproxy.business.config.entity;

import com.google.gson.annotations.Expose;
import java.nio.file.Paths;
import java.util.Objects;

/**
 *
 * @author joern.muehlencord
 */
public class Cache {

    @Expose
    private String name;

    @Expose
    private String path;

    public Cache(String name) {
        this.name = name;
    }

    public Cache(String name, String path) {
        this.name = name;
        this.path = path;
    }

    /* *** getter / setter *** */
    public String getName() {
        return name;
    }

    public Cache setName(String name) {
        this.name = name;
        return this;
    }

    public String getPath() {
        if (path == null) {
            path = Paths.get(System.getProperty("user.home"), ".osmproxy", "cache").toString();
        }

        return path;
    }

    public Cache setPath(String path) {
        this.path = path;
        return this;
    }

    /* equals / hashCode */
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 79 * hash + Objects.hashCode(this.name);
        hash = 79 * hash + Objects.hashCode(this.path);
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
        final Cache other = (Cache) obj;
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        if (!Objects.equals(this.path, other.path)) {
            return false;
        }
        return true;
    }

}
