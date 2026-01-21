package org.valkyrienskies.clockwork.mixin.content.flap_bearing;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.redstone.link.LinkBehaviour;
import com.simibubi.create.content.redstone.link.controller.LinkedControllerBindPacket;
import com.simibubi.create.content.redstone.link.controller.LinkedControllerClientHandler;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import org.apache.commons.lang3.tuple.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.valkyrienskies.clockwork.ClockworkBlocks;
import org.valkyrienskies.clockwork.ClockworkPackets;
import org.valkyrienskies.clockwork.LinkedControllerClientHandlerMixinStorage;
import org.valkyrienskies.clockwork.content.contraptions.flap.dual_link.DualLinkBehaviour;
import org.valkyrienskies.clockwork.content.contraptions.flap.smart_flap.FlapLinkedControllerBindPacket;

import static org.valkyrienskies.clockwork.content.contraptions.flap.dual_link.DualLinkHandler.getFrontFacing;


@Mixin(LinkedControllerClientHandler.class)
public class MixinLinkedControllerClientHandler {

    @Shadow
    private static BlockPos selectedLocation;

    /*@Definition(id = "fallDistance", field = "Lnet/minecraft/entity/Entity;fallDistance:F")
    @Expression("this.fallDistance > 0.0")
    @ModifyExpressionValue(
            method = "tick",
            at = @At("MIXINEXTRAS:EXPRESSION"),
            remap = false
    )
    private static @Coerce Object wrapGetBehaviour(BlockGetter be, BlockPos e, BehaviourType reader, Operation original) {
        if (!ClockworkBlocks.SMART_FLAP_BEARING.has(be.getBlockState(e))) return original.call(be, e, reader);

        // Just needs to be a not-null be behaviour to pass a null check
        return new Object();
    }*/

    @WrapOperation(
            method = "tick",
            at = @At(value = "INVOKE", target = "Lcom/simibubi/create/foundation/blockEntity/behaviour/BlockEntityBehaviour;get(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lcom/simibubi/create/foundation/blockEntity/behaviour/BehaviourType;)Lcom/simibubi/create/foundation/blockEntity/behaviour/BlockEntityBehaviour;"),
            remap = false
    )
    private static <T extends BlockEntityBehaviour> T wrapGetBehaviour(BlockGetter be, BlockPos e, BehaviourType<T> reader, Operation<T> original) {
        // Clockwork flap bearing will never return the LinkBehaviour create is wanting, since it's using a custom behaviour.
        // However, we still have to pass a null check, hence the weird sus null LinkBehaviour.
        if (!ClockworkBlocks.SMART_FLAP_BEARING.has(be.getBlockState(e))) return original.call(be, e, reader);

        /*@NotNull BehaviourType<@NotNull DualLinkBehaviour> type;
        if (LinkedControllerClientHandlerMixinStorage.face == getFrontFacing(be.getBlockState(e))) {
            type =  DualLinkBehaviour.Companion.getFRONT_TYPE();
        } else {
            type = DualLinkBehaviour.Companion.getBACK_TYPE();
        }

        // If we aren't targeting a side with the link freq
        if (BlockEntityBehaviour.get(be, e, type) == null) return original.call(be, e, reader);*/

        // Just needs to be a not-null be behaviour to pass a null check
        return (T) LinkBehaviour.receiver(null, Pair.of(null, null), null);
    }


    @WrapOperation(
            method = "tick",
            at = @At(value = "INVOKE", target = "Lme/pepperbell/simplenetworking/SimpleChannel;sendToServer(Lme/pepperbell/simplenetworking/C2SPacket;)V", ordinal = 3),
            require = 0,
            remap = false
    )
    private static void wrapSendToServerFabric(@Coerce Object instance, @Coerce Object packet, Operation<Void> original, @Local LinkBehaviour l, @Local Integer button) {
        wrapSendToServerCommon(instance, packet, original, l, button);
    }

    @WrapOperation(
            method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraftforge/network/simple/SimpleChannel;sendToServer(Ljava/lang/Object;)V", ordinal = 3),
            require = 0,
            remap = false
    )
    private static void wrapSendToServerForge(@Coerce Object instance, @Coerce Object packet, Operation<Void> original, @Local LinkBehaviour l, @Local Integer button) {
        wrapSendToServerCommon(instance, packet, original, l, button);
    }

    @Unique
    private static void wrapSendToServerCommon(Object instance, Object packet, Operation<Void> original, LinkBehaviour l, Integer button) {
        // No one else should be stupid enough to be passing in a LinkBehaviour with a null be
        if (l.blockEntity != null) {
            original.call(instance, packet);
            return;
        }
        ClockworkPackets.sendToServer(new FlapLinkedControllerBindPacket(button, selectedLocation, LinkedControllerClientHandlerMixinStorage.face));
    }
}
