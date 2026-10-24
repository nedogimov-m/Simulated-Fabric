package dev.simulated_team.simulated.content.blocks.behaviour;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import java.util.List;

/** The original tick/second/minute controls on actual native Create value settings. */
public class TimedScrollValueBehaviour extends ScrollValueBehaviour {
    public TimedScrollValueBehaviour(final Component label, final SmartBlockEntity owner, final ValueBoxTransform slot) { super(label, owner, slot); }
    @Override public ValueSettingsBoard createBoard(final Player player, final BlockHitResult hit) {
        return new ValueSettingsBoard(this.label, 60, 10, List.of(Component.translatable("create.generic.unit.ticks"),
                Component.translatable("create.generic.unit.seconds"), Component.translatable("create.generic.unit.minutes")), new ValueSettingsFormatter(this::formatSettings));
    }
    @Override public ValueSettings getValueSettings() {
        if (this.value >= 1200 && this.value % 1200 == 0) return new ValueSettings(2, this.value / 1200);
        if (this.value >= 20 && this.value % 20 == 0) return new ValueSettings(1, this.value / 20);
        return new ValueSettings(0, this.value);
    }
    @Override public void setValueSettings(final Player player, final ValueSettings setting, final boolean control) {
        if (setting.equals(this.getValueSettings())) return;
        final int multiplier = setting.row() == 0 ? 1 : setting.row() == 1 ? 20 : 1200;
        this.setValue(Math.max(1, setting.value()) * multiplier); this.playFeedbackSound(this);
    }
    public MutableComponent formatSettings(final ValueSettings setting) {
        return Component.literal(switch (setting.row()) {
            case 1 -> "0:" + (setting.value() < 10 ? "0" : "") + setting.value();
            case 2 -> setting.value() + ":00";
            default -> setting.value() + "t";
        });
    }
}
