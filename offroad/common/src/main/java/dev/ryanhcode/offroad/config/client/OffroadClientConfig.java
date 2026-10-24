package dev.ryanhcode.offroad.config.client;

import com.simibubi.create.infrastructure.config.NativeConfigBase;
import com.simibubi.create.infrastructure.config.ConfigScope;

public class OffroadClientConfig extends NativeConfigBase {
    @Override
    protected ConfigScope defaultScope() { return ConfigScope.CLIENT; }

	@Override
	public String getName() {
		return "client";
	}
}
