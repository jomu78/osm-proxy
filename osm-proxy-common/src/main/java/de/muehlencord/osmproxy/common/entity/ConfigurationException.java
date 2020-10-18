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

/**
 *
 * @author joern.muehlencord
 */
public class ConfigurationException extends Exception {

    private static final long serialVersionUID = 8670920953038338033L;

    /**
     * Creates a new instance of <code>ConfigurationException</code> without detail message.
     */
    public ConfigurationException() {
    }


    /**
     * Constructs an instance of <code>ConfigurationException</code> with the specified detail message.
     * @param msg the detail message.
     */
    public ConfigurationException(String msg) {
        super(msg);
    }

    /**
     * Constructs an instance of <code>ConfigurationException</code> with the specified detail message and root cause.
     * @param msg the detail message.
     * @param th the root cause
     */
    public ConfigurationException(String msg, Throwable th) {
        super(msg,th);
    }
}
