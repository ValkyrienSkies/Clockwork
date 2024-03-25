package org.valkyrienskies.clockwork.platform.block_entity;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.content.logistics.vault.ItemVaultBlock;
import com.simibubi.create.content.logistics.vault.ItemVaultBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.VersionedInventoryWrapper;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.CombinedInvWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.valkyrienskies.clockwork.content.physicalities.ballast.BallastBlockEntity;

public class ForgeBallastBlockEntity extends BallastBlockEntity {

    protected LazyOptional<IItemHandler> itemCapability;

    public ForgeBallastBlockEntity(@Nullable BlockEntityType<?> type, @Nullable BlockPos pos, @Nullable BlockState state) {
        super(type, pos, state);

        inventory = new ItemStackHandler(4) {
            @Override
            protected void onContentsChanged(int slot) {
                super.onContentsChanged(slot);
                recalculateWeightNextTick = true;
            }
        };
    }

    @Override
    protected void read(@NotNull CompoundTag compound, boolean clientPacket) {
        super.read(compound, clientPacket);

        if (!clientPacket && inventory instanceof ItemStackHandler ish) {
            ish.deserializeNBT(compound.getCompound("Inventory"));
        }
    }

    @Override
    protected void write(@NotNull CompoundTag compound, boolean clientPacket) {
        super.write(compound, clientPacket);
        if (!clientPacket) {
            if (inventory instanceof ItemStackHandler ish) {
                compound.put("Inventory", ish.serializeNBT());
            }
        }
    }

    @Override
    public void updateWeight() {
        this.oldWeight = this.newWeight;

        int maxCountItem = 4 * 64;

        var temp = 0;
        for (int i = 0; i < getInventoryOfBlock().getSlots(); i++) {
            temp += getInventoryOfBlock().getStackInSlot(i).getCount();
        }

        this.newWeight = (double) mapValue(temp, 0, maxCountItem, 0, 10000);
    }

    public ItemStackHandler getInventoryOfBlock(){
        return (ItemStackHandler)inventory;
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        if (isItemHandlerCap(cap)) {
            initCapability();
            return itemCapability.cast();
        }
        return super.getCapability(cap, side);
    }

    private void initCapability() {
        if (itemCapability.isPresent())
            return;

        IItemHandlerModifiable[] invs = new IItemHandlerModifiable[4];

        IItemHandler itemHandler = new VersionedInventoryWrapper(new CombinedInvWrapper(invs));
        itemCapability = LazyOptional.of(() -> itemHandler);
    }
}
