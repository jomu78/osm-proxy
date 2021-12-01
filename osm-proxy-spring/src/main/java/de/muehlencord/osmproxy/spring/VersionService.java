package de.muehlencord.osmproxy.spring;

import de.muehlencord.osmproxy.common.AbstractVersionService;
import javax.annotation.PostConstruct;
import org.springframework.stereotype.Service;

/**
 * get version information from buildInformation.properties
 *
 * @author Joern Muehlencord, 2021-12-01
 * @since 1.1
 */
@Service
public class VersionService extends AbstractVersionService {

  @PostConstruct
  void init() {
    readVersionInformation();
  }

}
