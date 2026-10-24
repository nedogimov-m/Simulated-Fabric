package dev.ryanhcode.offroad.config.server;

import com.simibubi.create.infrastructure.config.NativeConfigBase;

public class OffroadServer extends NativeConfigBase {

	public final OffroadBlockConfigs blocks = this.nested(0, OffroadBlockConfigs::new, Comments.blockConfig);
	public final OffroadKinetics kinetics = this.nested(0, OffroadKinetics::new, Comments.kinetics);

	@Override
	public String getName() {
		return "server";
	}

	private static class Comments {
		static String kinetics = "Parameters and abilities of Offroad's kinetic mechanisms";
		static String blockConfig = "Parameters and abilities of Offroad Blocks";
	}
}
