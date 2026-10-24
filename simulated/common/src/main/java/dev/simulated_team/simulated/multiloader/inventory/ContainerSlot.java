package dev.simulated_team.simulated.multiloader.inventory;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class ContainerSlot implements NBTSerializable, SlotAccess {

    public static final ContainerSlot EMPTY = new ContainerSlot();

    private final int index;
    private final AbstractContainer parent;

    private Item type;
    private ItemStack stack;

    public ContainerSlot() {
        this.index = -1;
        this.parent = null;

        this.type = ItemStack.EMPTY.getItem();
        this.stack = ItemStack.EMPTY;
    }

    public ContainerSlot(final int slot, final ItemStack stack, final Item type, final AbstractContainer parent) {
        this.index = slot;
        this.stack = stack;
        this.type = type;
        this.parent = parent;

        //populate the parent's fields
        parent.populateFields(this);
    }

    public static ContainerSlot of(final int slot, final Item type, final AbstractContainer parent) {
        return of(slot, new ItemStack(type), parent);
    }

    public static ContainerSlot of(final int slot, final ItemStack stack, final AbstractContainer parent) {
        return new ContainerSlot(slot, stack, stack.getItem(), parent);
    }

    public int insertStack(final ItemInfoWrapper info, final int maxAmount, final boolean simulate) {
        if (maxAmount <= 0 || info.type() == Items.AIR || !this.canInsert(info)) {
            return 0;
        }
        if (this.getStack().isEmpty() || (this.getType() == info.type()
                && this.getStack().getComponentsPatch().equals(info.patchMap()))) {
            final ItemStack incoming = ItemInfoWrapper.generateFromInfo(info);
            final int insertedAmount = Math.max(0, Math.min(
                    this.parent.getMaxStackSize(incoming) - this.getStack().getCount(), maxAmount));
            if (!simulate && insertedAmount > 0) {
                final ItemStack replacement = this.isEmpty() ? incoming : this.getStack().copy();
                replacement.setCount(this.getStack().getCount() + insertedAmount);
                this.setStack(replacement);
            }
            return insertedAmount;
        }

        return 0;
    }

    public int extractStack(final ItemInfoWrapper info, final int maxAmount, final boolean simulate) {
        if (maxAmount > 0 && this.canExtract() && !this.getStack().isEmpty()) {
            if (info != null && (this.getType() != info.type()
                    || !this.getStack().getComponentsPatch().equals(info.patchMap()))) {
                return 0;
            }

            final int extracted = Math.min(this.getStack().getCount(), maxAmount);
            if (!simulate && extracted > 0) {
                final ItemStack newStack = this.getStack().copy();
                newStack.shrink(extracted);

                this.setStack(newStack);
            }

            return extracted;
        }

        return 0;
    }

    public void shrink(final long amountToShrink) {
        ItemStack copied = this.getStack().copy();

        if (copied.getCount() - amountToShrink <= 0) {
            copied = ItemStack.EMPTY;
        } else {
            copied.shrink((int) amountToShrink);
        }

        this.setStack(copied);
    }

    public boolean canInsert(final ItemInfoWrapper info) {
        return this.parent.canInsertItem(info, this);
    }

    public boolean canExtract() {
        return this.parent.canExtractFromSlot(this);
    }

    public boolean isEmpty() {
        return this == EMPTY || this.getStack().isEmpty();
    }

    public void setStack(final ItemStack stack) {
        final ItemStack replacement = stack.isEmpty() ? ItemStack.EMPTY : stack;
        this.parent.onStackItemChange(this, this.getStack(), replacement);

        this.stack = replacement;
        this.type = replacement.getItem();

        this.parent.notifyContentsChanged();
    }

    public int getIndex() {
        return this.index;
    }

    public ItemStack getStack() {
        return this.stack;
    }

    @Override
    public ItemStack get() {
        return this.getStack();
    }

    @Override
    public boolean set(final ItemStack stack) {
        this.setStack(stack);
        return true;
    }

    public Item getType() {
        return this.type;
    }

    public AbstractContainer getParent() {
        return this.parent;
    }

    public void clear() {
        if (this.parent != null) {
            this.setStack(ItemStack.EMPTY);
        }
    }

    public void write(final net.minecraft.world.level.storage.ValueOutput output) {
        output.putInt("index", this.getIndex());
        if (!this.getStack().isEmpty()) output.store("item", ItemStack.CODEC, this.getStack());
    }
    public void read(final net.minecraft.world.level.storage.ValueInput input) {
        try (var ignored = AbstractContainer.suppressChangeNotifications()) {
            this.setStack(input.read("item", ItemStack.CODEC).orElse(ItemStack.EMPTY));
        }
    }

    @Override
    public CompoundTag write(final HolderLookup.Provider provider) {
        final CompoundTag slotTag = new CompoundTag();

        slotTag.putInt("index", this.getIndex());
        if (!this.getStack().isEmpty()) {
            slotTag.put("item", ItemStack.CODEC.encodeStart(provider.createSerializationContext(NbtOps.INSTANCE), this.getStack()).getOrThrow());
        }

        return slotTag;
    }

    @Override
    public void read(final HolderLookup.Provider provider, final CompoundTag nbt) {
        final ItemStack loaded = nbt.getCompound("item")
                .flatMap(tag -> ItemStack.CODEC.parse(provider.createSerializationContext(NbtOps.INSTANCE), tag).result())
                .orElse(ItemStack.EMPTY);
        try (var ignored = AbstractContainer.suppressChangeNotifications()) {
            this.setStack(loaded);
        }
    }
}
