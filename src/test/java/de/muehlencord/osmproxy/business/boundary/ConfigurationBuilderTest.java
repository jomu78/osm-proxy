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
package de.muehlencord.osmproxy.business.boundary;

import de.muehlencord.osmproxy.business.config.boundary.ConfigurationBuilder;
import de.muehlencord.osmproxy.business.config.entity.Cache;
import de.muehlencord.osmproxy.business.config.entity.Configuration;
import de.muehlencord.osmproxy.business.config.entity.Layer;
import de.muehlencord.osmproxy.business.config.entity.Server;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;

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
