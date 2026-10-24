package dev.eriksonn.aeronautics.config.server;

import com.simibubi.create.infrastructure.config.NativeConfigBase;

public class AeroKinetics extends NativeConfigBase {

	public final AeroStress stressValues = this.nested(1, AeroStress::new, Comments.stress);

	@Override
	public String getName() {
		return "kinetics";
	}

	private static class Comments {
		static String stress = "Fine tune the kinetic stats of individual components";
	}
}
