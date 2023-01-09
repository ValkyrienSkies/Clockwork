package org.valkyrienskies.clockwork.fabric.content.contraptions.components.infuser;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Quaternion;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.SuperByteBuffer;
import com.simibubi.create.foundation.tileEntity.renderer.SafeTileEntityRenderer;
import com.simibubi.create.foundation.tileEntity.renderer.SmartTileEntityRenderer;
import com.simibubi.create.foundation.utility.AngleHelper;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.valkyrienskies.clockwork.fabric.AllClockworkPartials;

public class PhysicsInfuserRenderer extends SmartTileEntityRenderer<PhysicsInfuserBlockEntity> {

    public PhysicsInfuserRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(PhysicsInfuserBlockEntity te, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        super.renderSafe(te, partialTicks, ms, buffer, light, overlay);
        if (!(te instanceof PhysicsInfuserBlockEntity))
            return;

        PhysicsInfuserBlockEntity infuser = (PhysicsInfuserBlockEntity) te;
        BlockState blockState = te.getBlockState();

        VertexConsumer vb = buffer.getBuffer(RenderType.translucent());

        // Render Mysterious Liquid

        SuperByteBuffer mysteriousLiquid = CachedBufferer.partial(AllClockworkPartials.STRANGE_FLUID, blockState);

        mysteriousLiquid.light(light).renderInto(ms, buffer.getBuffer(RenderType.translucent()));

        // Core

        SuperByteBuffer core = CachedBufferer.partial(AllClockworkPartials.PHYSICS_CORE, blockState);
        float angle = 0;
        float offset = 0;

        if (infuser.animationType != null) {
            if (infuser.animationType == PhysicsInfuserBlockEntity.Animation.ASSEMBLY) {
                float value = infuser.assemblyProgress.getValue();
                rotateCore(core, angle, offset, value).light(light).renderInto(ms, vb);
            }
            if (infuser.animationType == PhysicsInfuserBlockEntity.Animation.DISASSEMBLY) {
                float value = infuser.disassemblyProgress.getValue();
                rotateCore(core, angle, offset, value).light(light).renderInto(ms, vb);
            }

            idleRotateCore(core, angle, offset, infuser).light(light).renderInto(ms, vb);
        }


    }

    private SuperByteBuffer idleRotateCore(SuperByteBuffer buffer, float angle, float offset, PhysicsInfuserBlockEntity infuser) {
        float pivotX = 8f;
        float pivotY = 8f;
        float pivotZ = 8f;
        LerpedFloat speen = LerpedFloat.linear();
        float interpolatedAngle = infuser.getInterpolatedCoreAngle(AnimationTickHolder.getPartialTicks() - 1);
        Quaternion q = new Quaternion(0, 1, 0, angle);
        buffer.rotateCentered(Direction.UP, (float) (interpolatedAngle / 180 * Math.PI)).translate(0, offset, 0);
        return buffer;
    }
    private SuperByteBuffer rotateCore(SuperByteBuffer buffer, float angle, float offset, float value) {
        return null;
    }
}
