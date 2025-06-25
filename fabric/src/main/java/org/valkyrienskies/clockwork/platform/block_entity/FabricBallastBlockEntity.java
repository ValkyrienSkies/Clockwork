package org.valkyrienskies.clockwork.platform.block_entity;

import com.simibubi.create.content.logistics.vault.ItemVaultBlockEntity;
import io.github.fabricators_of_create.porting_lib.transfer.item.ItemStackHandler;
import io.github.fabricators_of_create.porting_lib.transfer.item.SlottedStackStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SidedStorageBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.valkyrienskies.clockwork.content.physicalities.ballast.BallastBlockEntity;

public class FabricBallastBlockEntity extends BallastBlockEntity implements SidedStorageBlockEntity {
    public FabricBallastBlockEntity(@Nullable BlockEntityType<?> type, @Nullable BlockPos pos, @Nullable BlockState state) {

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
    public void updateWeight(){
        this.oldWeight = this.newWeight;

        int maxCountItem = 4 * 64;

        var temp = 0;
        for (int i = 0; i < getInventoryOfBlock().getSlotCount(); i++) {
            temp += getInventoryOfBlock().getStackInSlot(i).getCount();
        }

        this.newWeight = (double) mapValue(temp, 0, maxCountItem, 0, 10000);
    }

    public ItemStackHandler getInventoryOfBlock(){
        return (ItemStackHandler)inventory;
    }

    @Override
    public @Nullable Storage<ItemVariant> getItemStorage(@Nullable Direction face) {
        return (Storage<ItemVariant>) inventory;
    }
}
