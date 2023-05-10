package org.valkyrienskies.clockwork.mixin.content.universal_joint;

import com.simibubi.create.content.contraptions.RotationPropagator;
import com.simibubi.create.content.contraptions.base.KineticTileEntity;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.valkyrienskies.clockwork.content.contraptions.universal_joint.UniversalJointBlockEntity;

import java.util.LinkedList;
import java.util.List;

@Mixin(RotationPropagator.class)
public abstract class MixinRotationPropagator {

    @Shadow
    private static List<BlockPos> getPotentialNeighbourLocations(KineticTileEntity te) {return null;};

    @Shadow
    private static KineticTileEntity findConnectedNeighbour(KineticTileEntity currentTE, BlockPos neighbourPos) {return null;};

    @Inject(method = "getConnectedNeighbours", at = @At("HEAD"), cancellable = true, remap = false)
    private static void getConnectedNeighborsDistant(@NotNull KineticTileEntity te, CallbackInfoReturnable<List<KineticTileEntity>> cir) {
        cir.cancel();
        List<KineticTileEntity> neighbours = new LinkedList<>();
        if (te instanceof UniversalJointBlockEntity) {
            final BlockPos jointNeighbourTEPos = ((UniversalJointBlockEntity) te).getConnectedPos();
            if (te.getLevel() != null && jointNeighbourTEPos != null) {
                if (te.getLevel().getBlockEntity(jointNeighbourTEPos) instanceof UniversalJointBlockEntity) {
                    final KineticTileEntity neighbourTE = (KineticTileEntity) te.getLevel().getBlockEntity(jointNeighbourTEPos);
                    neighbours.add(neighbourTE);
                }
            }
        }


        for (BlockPos neighbourPos : getPotentialNeighbourLocations(te)) {
            final KineticTileEntity neighbourTE = findConnectedNeighbour(te, neighbourPos);

            if (neighbourTE == null)
                continue;

            neighbours.add(neighbourTE);
        }
        cir.setReturnValue(neighbours);
    }
}
