package org.valkyrienskies.clockwork.fabric.mixin.content.vs_contraptions;

import com.simibubi.create.content.contraptions.components.structureMovement.AssemblyException;
import com.simibubi.create.content.contraptions.components.structureMovement.bearing.BearingBlock;
import com.simibubi.create.content.contraptions.components.structureMovement.bearing.MechanicalBearingTileEntity;
import com.simibubi.create.foundation.tileEntity.SyncedTileEntity;
import com.simibubi.create.foundation.utility.UniqueLinkedList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.clockwork.fabric.mixinducks.CWIControlContraption;
import org.valkyrienskies.clockwork.fabric.util.assemble.GlueAssembler;
import org.valkyrienskies.core.impl.datastructures.DenseBlockPosSet;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.assembly.ShipAssemblyKt;

import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

@Mixin(MechanicalBearingTileEntity.class)
public abstract class MixinMechanicalBearingTileEntity extends BlockEntity implements CWIControlContraption  {

    @Shadow protected AssemblyException lastException;

    public MixinMechanicalBearingTileEntity(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState) {
        super(blockEntityType, blockPos, blockState);
    }

    @Override
    public boolean isShipContraptionController() {
        return true;
    }


    @Inject(method = "assemble", at = @At("HEAD"), cancellable = true, remap = false)
    void assemble(CallbackInfo ci) {
        if (isShipContraptionController() && level != null && !level.isClientSide) {
            ci.cancel();

            if (!((level.getBlockState(worldPosition)).getBlock() instanceof BearingBlock))
                return;

            Direction direction = getBlockState().getValue(BearingBlock.FACING);
            BlockPos center = worldPosition.relative(direction);
            DenseBlockPosSet selection = null;

            try {
                selection = GlueAssembler.collectGlued(this.level, center);
                this.lastException = null;
            } catch (AssemblyException e) {
                lastException = e;
                ((SyncedTileEntity) (Object) this).sendData();
                return;
            }

            if (selection == null) return;

            ShipAssemblyKt.createNewShipWithBlocks(center, selection, (ServerLevel) level);
        }
    }
}
