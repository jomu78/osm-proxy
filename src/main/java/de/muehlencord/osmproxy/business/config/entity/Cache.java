package de.muehlencord.osmproxy.business.config.entity;

import com.google.gson.annotations.Expose;
import java.nio.file.Path;
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
