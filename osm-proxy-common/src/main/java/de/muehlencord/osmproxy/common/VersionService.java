package de.muehlencord.osmproxy.common;

/**
 * Add a short description of the class
 *
 * @author Joern Muehlencord, 2021-12-01
 * @since TODO - add versiom
 */
public interface VersionService {

  void readVersionInformation();

  String getVersion();

  String getBuildDate();

}
