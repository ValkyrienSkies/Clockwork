package org.valkyrienskies.clockwork.fabric.mixin.content.vs_contraptions.render;

import com.jozufozu.flywheel.api.Instancer;
import com.jozufozu.flywheel.api.Material;
import com.jozufozu.flywheel.core.PartialModel;
import com.jozufozu.flywheel.core.materials.oriented.OrientedData;
import com.simibubi.create.content.contraptions.base.KineticTileEntity;
import com.simibubi.create.content.contraptions.components.structureMovement.bearing.BearingInstance;
import com.simibubi.create.content.contraptions.components.structureMovement.bearing.IBearingTileEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.valkyrienskies.clockwork.fabric.AllClockworkPartials;
import org.valkyrienskies.clockwork.fabric.mixinducks.CWIControlContraption;

@Mixin(BearingInstance.class)
public class MixinBearingInstance {


    @Shadow @Final
    KineticTileEntity bearing;

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/jozufozu/flywheel/api/Material;getModel(Lcom/jozufozu/flywheel/core/PartialModel;Lnet/minecraft/world/level/block/state/BlockState;)Lcom/jozufozu/flywheel/api/Instancer;"))
    Instancer getVSModel(Material instance, PartialModel partial, BlockState referenceState)
    {
        if (!(bearing instanceof CWIControlContraption && ((CWIControlContraption) bearing).containsShip()))
            return instance.getModel(partial, referenceState);

        return instance.getModel(AllClockworkPartials.BEARING_TOP_VSIFIED);
    }


}
