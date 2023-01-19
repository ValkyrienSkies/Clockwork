package org.valkyrienskies.clockwork.mixin.content.vs_contraptions.render;

import com.jozufozu.flywheel.api.Instancer;
import com.jozufozu.flywheel.api.Material;
import com.jozufozu.flywheel.core.PartialModel;
import com.simibubi.create.content.contraptions.base.KineticTileEntity;
import com.simibubi.create.content.contraptions.components.structureMovement.bearing.BearingInstance;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.valkyrienskies.clockwork.ClockWorkPartials;
import org.valkyrienskies.clockwork.platform.api.ContraptionController;

import static org.valkyrienskies.clockwork.ClockWorkMod.MIXIN_LOGGER;

@Mixin(BearingInstance.class)
public class MixinBearingInstance {

    @Shadow @Final
    KineticTileEntity bearing;

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/jozufozu/flywheel/api/Material;getModel(Lcom/jozufozu/flywheel/core/PartialModel;Lnet/minecraft/world/level/block/state/BlockState;)Lcom/jozufozu/flywheel/api/Instancer;"))
    Instancer getVSModel(Material instance, PartialModel partial, BlockState referenceState)
    {
        if (!(bearing instanceof ContraptionController)) {
            MIXIN_LOGGER.warn("We found contraption controller that does not has CW-compat implemented, this shouldn't happen.\n" +
                    "controller: " + bearing.getClass().getName());
            return instance.getModel(partial, referenceState);
        }

        if (!((ContraptionController) bearing).isShipContraptionController())
            return instance.getModel(partial, referenceState);

        return instance.getModel(ClockWorkPartials.BEARING_TOP_VSIFIED);
    }
}
