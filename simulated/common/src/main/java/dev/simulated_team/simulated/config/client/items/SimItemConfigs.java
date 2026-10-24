package dev.simulated_team.simulated.config.client.items;

import com.simibubi.create.infrastructure.config.NativeConfigBase;

public class SimItemConfigs extends NativeConfigBase {
    public final ConfigEnum<PropertyDisplay> displayProperties =
            this.e(PropertyDisplay.GOGGLES, "displayProperties", Comments.displayProperties);

    public final ConfigFloat physicsStaffScrollSensitivity = this.f(0.6f, 0, Float.MAX_VALUE, "physics_staff_scroll_sensitivity", SimItemConfigs.Comments.physicsStaffScrollSensitivity);
    public final ConfigFloat physicsStaffRotateSensitivity = this.f(0.35f, 0, Float.MAX_VALUE, "physics_staff_rotate_sensitivity", SimItemConfigs.Comments.physicsStaffRotateSensitivity);
    public final ConfigBool showNewPonderTag = this.b(true, "show_new_ponders", Comments.showNewPonderTag);

    @Override
    public String getName() {
        return "items";
    }

    /** Kept with the common config so dedicated servers never load tooltip/rendering classes. */
    public enum PropertyDisplay {
        ALWAYS(true, false, false),
        SHIFT(true, true, false),
        GOGGLES(true, false, true),
        SHIFT_GOGGLES(true, true, true),
        NEVER(false, false, false);

        private final boolean allow, requireShift, requireGoggles;
        PropertyDisplay(boolean allow, boolean requireShift, boolean requireGoggles) {
            this.allow = allow;
            this.requireShift = requireShift;
            this.requireGoggles = requireGoggles;
        }
        public boolean test(boolean shift, boolean goggles) {
            return this.allow && (!this.requireShift || shift) && (!this.requireGoggles || goggles);
        }
    }

    public static class Comments {
        static String displayProperties = "When to display physics properties in block tooltips";
        static String physicsStaffScrollSensitivity = "The sensitivity of scrolling when holding a sub-level with the Creative Physics Staff";
        static String physicsStaffRotateSensitivity = "The sensitivity of rotation when holding a sub-level with the Creative Physics Staff";
        static String showNewPonderTag = "If ponders that have new additions should display the \"(New!)\" text";
    }
}
