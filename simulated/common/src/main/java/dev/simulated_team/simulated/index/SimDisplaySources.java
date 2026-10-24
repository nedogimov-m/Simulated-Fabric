package dev.simulated_team.simulated.index;

import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.content.redstone.displayLink.source.ItemThroughputDisplaySource;
import com.simibubi.create.api.registry.CreateBuiltInRegistries;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import dev.simulated_team.simulated.content.display_sources.*;

import java.util.function.Supplier;

public class SimDisplaySources {
    public static final ItemThroughputDisplaySource AUGER_DISPLAY = simple("auger_throughput", ItemThroughputDisplaySource::new);
    public static final VelocitySensorDisplaySource VELO_DISPLAY = simple("velocity_display", VelocitySensorDisplaySource::new);
    public static final GimbalSensorDisplaySource GIMBAL_DISPLAY = simple("gimbal_display", GimbalSensorDisplaySource::new);
    public static final OpticalSensorDisplaySource OPTICAL_SENSOR_DISPLAY = simple("optical_sensor_display", OpticalSensorDisplaySource::new);
    public static final LaserSensorDisplaySource LASER_SENSOR_DISPLAY = simple("laser_sensor_display", LaserSensorDisplaySource::new);
    public static final AltitudeSensorDisplaySource ALTITUDE_SENSOR_DISPLAY = simple("altitude_sensor_display", AltitudeSensorDisplaySource::new);
    public static final PortableEngineDisplaySource PORTABLE_ENGINE_DISPLAY = simple("portable_engine_display", PortableEngineDisplaySource::new);
    public static final DockingConnectorDisplaySource DOCKING_CONNECTOR_DISPLAY = simple("docking_connector_display", DockingConnectorDisplaySource::new);
    public static final LinkedTypewriterDisplaySource LINKED_TYPEWRITER__DISPLAY = simple("linked_typewriter_display", LinkedTypewriterDisplaySource::new);
    public static final NavigationTableDisplaySource NAV_TABLE_DISPLAY = simple("nav_table_display", NavigationTableDisplaySource::new);

    private static <T extends DisplaySource> T simple(final String name, final Supplier<T> supplier) {
        return Registry.register(CreateBuiltInRegistries.DISPLAY_SOURCE, Identifier.fromNamespaceAndPath("simulated", name), supplier.get());
    }

}
