package org.valkyrienskies.clockwork.platform.block_entity;

import com.simibubi.create.content.logistics.vault.ItemVaultBlockEntity;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.valkyrienskies.clockwork.content.physicalities.ballast.BallastBlockEntity;

public class ForgeBallastBlockEntity extends BallastBlockEntity {

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
}
