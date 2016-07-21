package de.muehlencord.osmproxy.business.boundary;

import de.muehlencord.osmproxy.business.config.entity.Layer;
import de.muehlencord.osmproxy.business.config.entity.Server;
import de.muehlencord.osmproxy.business.config.entity.Configuration;
import de.muehlencord.osmproxy.business.config.boundary.ConfigurationBuilder;
import de.muehlencord.osmproxy.business.config.entity.Cache;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author joern.muehlencord
 */
public class ConfigurationBuilderTest {

    @Test
    public void testConfiguration() {
        Configuration config = new Configuration();
        config.setCache(new Cache("disk"));
        
        Layer layer = new Layer ("tiles")
                .addUpstreamServer(new Server("openstreetmap", "http://a.tile.openstreetmap.org/{z}/{x}/{y}.png"))
                .setCacheFolder ("tiles");
                
        config.addLayer(layer);

        String jsonString = ConfigurationBuilder.toJson(config);
        assertNotNull(jsonString);
        System.out.println(jsonString);

        Configuration c = ConfigurationBuilder.fromJson(jsonString);
        assertEquals(config, c);
    }
}
