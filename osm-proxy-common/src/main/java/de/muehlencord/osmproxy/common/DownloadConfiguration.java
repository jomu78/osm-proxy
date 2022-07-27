package de.muehlencord.osmproxy.common;

import java.nio.file.Path;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Joern Muehlencord, 2020-10-18
 * @since 1.1.0
 */
@Getter
@Setter
public class DownloadConfiguration {

  String userAgent;
  String acceptHeader;
  private Path tilePath;
  private String layer;
  private long z;
  private long x;
  private long y;
  private String ending;

  public DownloadConfiguration withUserAgent(final String userAgent) {
    this.userAgent = userAgent;
    return this;
  }

  public DownloadConfiguration withAcceptHeader(String acceptHeader) {
    this.acceptHeader = acceptHeader;
    return this;
  }


  public DownloadConfiguration withTilePath(final Path tilePath) {
    this.tilePath = tilePath;
    return this;
  }

  public DownloadConfiguration withLayer(final String layer) {
    this.layer = layer;
    return this;
  }

  public DownloadConfiguration withZ(final long z) {
    this.z = z;
    return this;
  }

  public DownloadConfiguration withX(final long x) {
    this.x = x;
    return this;
  }

  public DownloadConfiguration withY(final long y) {
    this.y = y;
    return this;
  }

  public DownloadConfiguration withEnding(final String ending) {
    this.ending = ending;
    return this;
  }


}
