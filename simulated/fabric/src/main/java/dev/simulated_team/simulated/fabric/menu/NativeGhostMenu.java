package dev.simulated_team.simulated.fabric.menu;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;
import java.util.function.Predicate;

/** Native sample slots retain the Create ghost-menu contract without owning real items. */
public class NativeGhostMenu<T> extends AbstractContainerMenu {
    public final Inventory playerInventory;
    public final Player player;
    public final T contentHolder;
    public final SimpleContainer ghostInventory;
    public boolean slotsActive;
    private final Predicate<Player> canUse;
    private final boolean allowRepeats;

    public NativeGhostMenu(MenuType<?> type, int id, Inventory inventory, T content,
                           Predicate<Player> canUse, int samples, boolean allowRepeats,
                           int inventoryX, int inventoryY, int sampleX, int sampleY) {
        super(type, id);
        this.playerInventory = Objects.requireNonNull(inventory);
        this.player = inventory.player;
        this.contentHolder = Objects.requireNonNull(content);
        this.canUse = Objects.requireNonNull(canUse);
        this.allowRepeats = allowRepeats;
        this.ghostInventory = new SimpleContainer(samples);
        // Preserve the typewriter's hotbar-first ordering used by synchronized slot indices.
        for (int hotbar = 0; hotbar < 9; hotbar++) {
            this.addSlot(playerSlot(hotbar, inventoryX + hotbar * 18, inventoryY + 58));
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(playerSlot(column + row * 9 + 9, inventoryX + column * 18, inventoryY + row * 18));
            }
        }
        for (int i = 0; i < samples; i++) {
            this.addSlot(new Slot(this.ghostInventory, i, sampleX + i * 18, sampleY) {
                @Override public boolean isFake() { return true; }
                @Override public boolean mayPickup(Player player) { return false; }
                @Override public boolean mayPlace(ItemStack stack) { return false; }
                @Override public boolean isActive() { return NativeGhostMenu.this.slotsActive; }
            });
        }
    }

    private Slot playerSlot(int index, int x, int y) {
        return new Slot(this.playerInventory, index, x, y) {
            @Override public boolean isActive() { return NativeGhostMenu.this.slotsActive; }
        };
    }

    @Override public boolean stillValid(Player player) {
        return player == this.player && this.canUse.test(player);
    }

    @Override public void clicked(int index, int button, ContainerInput input, Player player) {
        if (!this.stillValid(player)) return;
        if (index < 36) {
            super.clicked(index, button, input, player);
            return;
        }
        if (index >= this.slots.size() || input == ContainerInput.THROW) return;
        if (input == ContainerInput.CLONE) {
            if (player.isCreative() && this.getCarried().isEmpty()) {
                final ItemStack sample = this.ghostInventory.getItem(index - 36);
                this.setCarried(sample.copyWithCount(sample.getMaxStackSize()));
            }
            return;
        }
        this.ghostInventory.setItem(index - 36, sample(this.getCarried()));
        this.getSlot(index).setChanged();
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (!this.stillValid(player) || index < 0 || index >= this.slots.size()) return ItemStack.EMPTY;
        if (index >= 36) {
            this.ghostInventory.setItem(index - 36, ItemStack.EMPTY);
            this.getSlot(index).setChanged();
            return ItemStack.EMPTY;
        }
        final ItemStack selected = this.getSlot(index).getItem();
        if (selected.isEmpty()) return ItemStack.EMPTY;
        for (int i = 0; i < this.ghostInventory.getContainerSize(); i++) {
            final ItemStack existing = this.ghostInventory.getItem(i);
            if (!this.allowRepeats && ItemStack.isSameItemSameComponents(existing, selected)) break;
            if (existing.isEmpty()) {
                this.ghostInventory.setItem(i, sample(selected));
                this.getSlot(i + 36).setChanged();
                break;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override protected boolean moveItemStackTo(ItemStack stack, int start, int end, boolean reverse) { return false; }
    @Override public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) { return slot.container == this.playerInventory; }
    @Override public boolean canDragTo(Slot slot) { return this.allowRepeats || slot.container == this.playerInventory; }

    public void clearContents() { this.ghostInventory.clearContent(); }
    public void setGhostSample(int slot, ItemStack stack) { this.ghostInventory.setItem(slot, sample(stack)); }
    private static ItemStack sample(ItemStack stack) { return stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1); }
}
