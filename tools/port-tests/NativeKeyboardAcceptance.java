import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.serialization.DynamicOps;
import dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.LinkedTypewriterEntries;
import dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.LinkedTypewriterInteractionHandler;
import dev.simulated_team.simulated.index.SimKeys;
import dev.simulated_team.simulated.util.KeyboardScanCodes;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import net.minecraft.SharedConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.*;

/** Genuine native bootstrap, production key defaults and production NBT/network codecs; no game/GPU launch. */
public final class NativeKeyboardAcceptance {
    public static void main(String[] args) throws Exception {
        SharedConstants.tryDetectVersion(); Bootstrap.bootStrap();
        BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(net.minecraft.data.registries.VanillaRegistries.createWorldLookup())
                .forEach(net.minecraft.core.component.DataComponentInitializers.PendingComponents::apply);
        var registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        DynamicOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
        int[][] critical = {{81, 20}, {32, 44}, {265, 82}, {48, 39}, {340, 225}};
        for (var pair : critical) {
            check(KeyboardScanCodes.fromLegacy(pair[0]) == pair[1], "Legacy physical conversion " + Arrays.toString(pair));
            check(KeyboardScanCodes.toLegacy(pair[1]) == pair[0], "Reverse legacy physical conversion " + Arrays.toString(pair));
            var entry = LinkedTypewriterEntries.KeyboardEntry.createFromCodec(new ItemStack(Items.DIAMOND), new ItemStack(Items.REDSTONE), pair[1]);
            var encoded = (CompoundTag) LinkedTypewriterEntries.KeyboardEntry.CODEC.encodeStart(ops, entry).getOrThrow();
            check(encoded.getIntOr("ScanCode", -99) == pair[1] && !encoded.contains("GLFWKey"), "Native save must contain ScanCode only");
            var legacy = encoded.copy(); legacy.remove("ScanCode"); legacy.putInt("GLFWKey", pair[0]);
            var decoded = LinkedTypewriterEntries.KeyboardEntry.CODEC.parse(ops, legacy).getOrThrow();
            same(entry, decoded);
            var savedAgain = (CompoundTag) LinkedTypewriterEntries.KeyboardEntry.CODEC.encodeStart(ops, decoded).getOrThrow();
            check(savedAgain.contains("ScanCode") && !savedAgain.contains("GLFWKey"), "Legacy decode must migrate on native save");
            var mixed = encoded.copy(); mixed.putInt("GLFWKey", 32);
            check(LinkedTypewriterEntries.KeyboardEntry.CODEC.parse(ops, mixed).getOrThrow().getScanCode() == pair[1], "Native ScanCode must win over legacy alternative");
            var wire = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
            try {
                LinkedTypewriterEntries.KeyboardEntry.STREAM_CODEC.encode(wire, decoded);
                same(decoded, LinkedTypewriterEntries.KeyboardEntry.STREAM_CODEC.decode(wire));
                check(wire.readableBytes() == 0, "Native keyboard stream codec left unread bytes");
            } finally { wire.release(); }
            var list = new ListTag(); list.add(legacy);
            var loaded = LinkedTypewriterEntries.readKeys(registries, list, new BlockPos(2, 3, 4));
            check(loaded.getEntry(pair[1]) != null && loaded.getEntry(pair[1]).getLocation().equals(new BlockPos(2, 3, 4)), "Actual entry map did not migrate native key/location");
            check(((CompoundTag) loaded.saveKeys(registries).getFirst()).contains("ScanCode"), "Actual entry-list save did not use native field");
        }
        check(InputConstants.UNKNOWN.getValue() == 0 && KeyboardScanCodes.UNBOUND == 0, "Actual SDL unbound key must be zero");
        check(KeyboardScanCodes.fromLegacy(-1) == 0 && KeyboardScanCodes.fromLegacy(99999) == 0 && KeyboardScanCodes.toLegacy(0) == -1,
                "Legacy unknown must migrate to native unbound");
        var unbound = LinkedTypewriterEntries.KeyboardEntry.createFromCodec(ItemStack.EMPTY, ItemStack.EMPTY, 0);
        var unknownTag = (CompoundTag) LinkedTypewriterEntries.KeyboardEntry.CODEC.encodeStart(ops, unbound).getOrThrow();
        unknownTag.remove("ScanCode"); unknownTag.putInt("GLFWKey", -1);
        check(LinkedTypewriterEntries.KeyboardEntry.CODEC.parse(ops, unknownTag).getOrThrow().getScanCode() == 0, "Legacy unknown codec is bound to a nonexistent negative scan code");
        var wire = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
        try { LinkedTypewriterEntries.KeyboardEntry.STREAM_CODEC.encode(wire, unbound); same(unbound, LinkedTypewriterEntries.KeyboardEntry.STREAM_CODEC.decode(wire)); }
        finally { wire.release(); }

        var defaults = SimKeys.class.getDeclaredField("key"); defaults.setAccessible(true);
        check(defaults.getInt(SimKeys.ROTATE_MODE) == InputConstants.KEY_TAB, "Actual staff rotate default must be SDL Tab");
        check(defaults.getInt(SimKeys.SCROLL_UP) == 0 && defaults.getInt(SimKeys.SCROLL_DOWN) == 0, "Actual scroll defaults must be native unbound");
        var mappings = new ArrayList<KeyMapping>(); SimKeys.registerTo(mappings::add);
        check(!SimKeys.ROTATE_MODE.getKeybind().isUnbound() && SimKeys.SCROLL_UP.getKeybind().isUnbound() && SimKeys.SCROLL_DOWN.getKeybind().isUnbound(), "Real key mappings disagree with native default values");
        var presets = LinkedTypewriterInteractionHandler.class.getDeclaredField("presetKeys"); presets.setAccessible(true);
        var keys = (Int2IntMap) presets.get(null);
        check(keys.get(20) == 0 && keys.get(44) == 13 && keys.get(82) == 4 && keys.get(39) == 12, "Actual typewriter physical defaults changed slots");
        String text = KeyboardScanCodes.editLegacyText("", 20);
        text = KeyboardScanCodes.editLegacyText(text, 44);
        check(text.equals("Q ") && KeyboardScanCodes.editLegacyText(text, InputConstants.KEY_BACKSPACE).equals("Q")
                && KeyboardScanCodes.editLegacyText(text, 0).equals(text), "Native physical typing/backspace broke original display text");
        check(InputConstants.PRESS == 1 && InputConstants.RELEASE == 0 && InputConstants.REPEAT == -1, "Actual native key action ABI changed");
        System.out.println("PASS genuine defaults, five critical physical migrations, production legacy/native NBT and registry-stream codecs, native unbound and physical display/backspace; SDL callback repeat dispatch separately bytecode-verified");
    }
    private static void same(LinkedTypewriterEntries.KeyboardEntry expected, LinkedTypewriterEntries.KeyboardEntry actual) {
        check(expected.getScanCode() == actual.getScanCode()
                && ItemStack.isSameItemSameComponents(expected.getFirstAsItemStack(), actual.getFirstAsItemStack())
                && ItemStack.isSameItemSameComponents(expected.getSecondAsItemStack(), actual.getSecondAsItemStack()), "Actual keyboard codec changed key/frequencies");
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
