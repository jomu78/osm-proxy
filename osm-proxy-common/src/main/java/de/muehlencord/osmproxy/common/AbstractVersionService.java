package de.muehlencord.osmproxy.common;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * read version information from build information properties.
 *
 * @author Joern Muehlencord, 2021-12-01
 * @since 1.1
 */
public abstract class AbstractVersionService implements VersionService {

  private static final Logger logger = LoggerFactory.getLogger(AbstractVersionService.class);

  /**
   * the version of the application - updated by maven build system automatically
   */
  private String version;
  /**
   * the build date of the application - updated by maven
   */
  private String buildDate;

  @Override
  public void readVersionInformation() {
    InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("buildInfo.properties");
    if (in == null) {
      return;
    }

    Properties props = new Properties();
    try {
      props.load(in);

      version = props.getProperty("build.version");
      buildDate = props.getProperty("build.timestamp");

      logger.debug("buildInfo.properties read successfully");

    } catch (IOException ex) {
      logger.error("Cannot find buildInfo.properties. ", ex);
      version = "??";
      buildDate = "??";
    }
  }

  @Override
  public String getVersion() {
    return version;
  }

  @Override
  public String getBuildDate() {
    return buildDate;
  }

}
