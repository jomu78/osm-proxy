package de.muehlencord.osmproxy.common;

import java.nio.file.Path;

/**
 * @author Joern Muehlencord, 2020-10-18
 * @since 1.1.0
 */
public class DownloadConfiguration {

  String userAgent;
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

  /* *** fluent api *** */


  /* *** getter / setter *** */

  public String getUserAgent() {
    return userAgent;
  }

  public void setUserAgent(String userAgent) {
    this.userAgent = userAgent;
  }

  public Path getTilePath() {
    return tilePath;
  }

  public void setTilePath(Path tilePath) {
    this.tilePath = tilePath;
  }

  public String getLayer() {
    return layer;
  }

  public void setLayer(String layer) {
    this.layer = layer;
  }

  public long getZ() {
    return z;
  }

  public void setZ(long z) {
    this.z = z;
  }

  public long getX() {
    return x;
  }

  public void setX(long x) {
    this.x = x;
  }

  public long getY() {
    return y;
  }

  public void setY(long y) {
    this.y = y;
  }

  public String getEnding() {
    return ending;
  }

  public void setEnding(String ending) {
    this.ending = ending;
  }
}
