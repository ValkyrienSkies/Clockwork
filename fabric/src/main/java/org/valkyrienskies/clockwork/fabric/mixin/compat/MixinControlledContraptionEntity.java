package org.valkyrienskies.clockwork.fabric.mixin.compat;

import com.simibubi.create.content.contraptions.components.structureMovement.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.components.structureMovement.ControlledContraptionEntity;
import com.simibubi.create.content.contraptions.components.structureMovement.MovementContext;
import com.simibubi.create.content.contraptions.components.structureMovement.bearing.BearingContraption;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Math;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ControlledContraptionEntity.class)
public abstract class MixinControlledContraptionEntity extends AbstractContraptionEntity {
    public MixinControlledContraptionEntity(EntityType<?> entityTypeIn, Level worldIn) {
        super(entityTypeIn, worldIn);
    }

    //Region start - fix actors in the center of a bearing contraption not triggering correctly (vanilla create bug)
    @Shadow
    protected float angleDelta;

    @Redirect(method = "shouldActorTrigger", at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD, target = "Lcom/simibubi/create/content/contraptions/components/structureMovement/MovementContext;motion:Lnet/minecraft/world/phys/Vec3;"))
    private void redirectPutMotion(MovementContext instance, Vec3 value) {
        BearingContraption bc = (BearingContraption) contraption;
        Direction facing = bc.getFacing();
        Vec3i dir = facing.getNormal();

        double scalar = Math.abs(angleDelta / 360.0) * Math.signum(dir.getX() + dir.getY() + dir.getZ());

        instance.motion = new Vec3(Math.abs(dir.getX()), Math.abs(dir.getY()), Math.abs(dir.getZ())).scale(scalar);
    }
    //Region end
}
