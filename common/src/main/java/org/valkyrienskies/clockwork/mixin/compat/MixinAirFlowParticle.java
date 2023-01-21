package org.valkyrienskies.clockwork.mixin.compat;

import com.simibubi.create.content.contraptions.components.fan.AirCurrent;
import com.simibubi.create.content.contraptions.components.fan.IAirCurrentSource;
import com.simibubi.create.content.contraptions.particle.AirFlowParticle;
import com.simibubi.create.foundation.utility.VecHelper;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.valkyrienskies.clockwork.mixinduck.IExtendedAirCurrentSource;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

@Mixin(AirFlowParticle.class)
public abstract class MixinAirFlowParticle {

    @Shadow
    @Final
    private IAirCurrentSource source;

    @Unique
    private Ship getShip() {
        if (source instanceof IExtendedAirCurrentSource se)
            return se.getShip();
        else if (source.getAirCurrentWorld() != null)
            return VSGameUtilsKt.getShipManagingPos(source.getAirCurrentWorld(), source.getAirCurrentPos());
        else
            return null;
    }

    @Redirect(method = "tick", at = @At(value = "FIELD", target = "Lcom/simibubi/create/content/contraptions/components/fan/AirCurrent;bounds:Lnet/minecraft/world/phys/AABB;", opcode = Opcodes.GETFIELD), remap = false)
    private AABB redirectBounds(AirCurrent instance) {
        Level level = instance.source.getAirCurrentWorld();
        if (level != null) {
            return VSGameUtilsKt.transformAabbToWorld(level, instance.bounds);
        }
        return instance.bounds;
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/foundation/utility/VecHelper;getCenterOf(Lnet/minecraft/core/Vec3i;)Lnet/minecraft/world/phys/Vec3;"), allow = 1)
    private Vec3 redirectGetCenterOf(Vec3i pos) {
        Ship ship = getShip();
        Vec3 result = VecHelper.getCenterOf(pos);
        if (ship != null) {
            Vector3d tempVec = new Vector3d();
            ship.getTransform().getShipToWorld().transformPosition(result.x, result.y, result.z, tempVec);
            result = VectorConversionsMCKt.toMinecraft(tempVec);
        }
        return result;
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;atLowerCornerOf(Lnet/minecraft/core/Vec3i;)Lnet/minecraft/world/phys/Vec3;"), allow = 1)
    private Vec3 redirectToLowerCorner(Vec3i pos) {
        Vec3 result = Vec3.atLowerCornerOf(pos);
        Ship ship = getShip();
        if (ship != null) {
            Vector3d tempVec = new Vector3d();
            ship.getTransform().getShipToWorld().transformDirection(result.x, result.y, result.z, tempVec);
            result = VectorConversionsMCKt.toMinecraft(tempVec);
        }
        return result;
    }
}