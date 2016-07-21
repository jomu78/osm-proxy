package de.muehlencord.osmproxy.business.config.entity;

/**
 *
 * @author joern.muehlencord
 */
public class ConfigurationException extends Exception {

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
