package dev.eriksonn.aeronautics.config.client;

import com.simibubi.create.infrastructure.config.NativeConfigBase;
import com.simibubi.create.infrastructure.config.ConfigScope;

public class AeroClient extends NativeConfigBase {
    @Override
    protected ConfigScope defaultScope() { return ConfigScope.CLIENT; }


	public final ConfigBool enableHotAirRendering = this.b(true, "hotAirRendering", Comments.hotAirRendering);

	@Override
	public String getName() {
		return "client";
	}

	private static class Comments {
		static String hotAirRendering = "If the wiggly hot air effect inside of balloons should be rendered";
	}
}
