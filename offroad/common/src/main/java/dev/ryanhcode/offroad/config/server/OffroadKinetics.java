package dev.ryanhcode.offroad.config.server;

import com.simibubi.create.infrastructure.config.NativeConfigBase;

public class OffroadKinetics extends NativeConfigBase {

	public final OffroadStress stressValues = this.nested(1, OffroadStress::new, Comments.stress);

	@Override
	public String getName() {
		return "kinetics";
	}

	private static class Comments {
		static String stress = "Fine tune the kinetic stats of individual components";
	}
}
