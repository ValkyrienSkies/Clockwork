package org.valkyrienskies.clockwork.fabric.mixin.create.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.contraptions.components.structureMovement.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.components.structureMovement.Contraption;
import com.simibubi.create.content.contraptions.components.structureMovement.render.ContraptionMatrices;
import com.simibubi.create.content.contraptions.components.structureMovement.render.ContraptionRenderInfo;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.core.api.ships.ClientShip;
import org.valkyrienskies.mod.common.VSClientGameUtils;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

@Mixin(ContraptionRenderInfo.class)
public class MixinContraptionRenderInfo {

    @Redirect(
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/simibubi/create/content/contraptions/components/structureMovement/AbstractContraptionEntity;getBoundingBoxForCulling()Lnet/minecraft/world/phys/AABB;"
            ),
            method = "beginFrame"
    )
    private AABB redirectGetAABBForCulling(final AbstractContraptionEntity receiver) {
        return VSGameUtilsKt.transformRenderAABBToWorld(((ClientLevel) receiver.level), receiver.position(),
                receiver.getBoundingBoxForCulling());
    }
}