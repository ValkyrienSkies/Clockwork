package org.valkyrienskies.clockwork.fabric.content.contraptions.components.infuser;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Quaternion;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.RenderTypes;
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
import org.valkyrienskies.clockwork.fabric.ClockWorkModFabric;

public class PhysicsInfuserRenderer extends SmartTileEntityRenderer<PhysicsInfuserBlockEntity> {

    private boolean doneAnimating = false;
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
        // Zappin'

        SuperByteBuffer zap1 = null;
        SuperByteBuffer zap2 = null;
        SuperByteBuffer zap3 = null;

        if (infuser.animationType == PhysicsInfuserBlockEntity.Animation.ASSEMBLY) {
            zap1 = CachedBufferer.partial(AllClockworkPartials.ZAP, blockState);
            zap2 = CachedBufferer.partial(AllClockworkPartials.ZAP, blockState);
            zap3 = CachedBufferer.partial(AllClockworkPartials.ZAP, blockState);
        } else {
            zap1 = null;
            zap2 = null;
            zap3 = null;
        }

        // Core

        SuperByteBuffer core = CachedBufferer.partial(AllClockworkPartials.PHYSICS_CORE, blockState);
        float angle = 0;
        float offset = 0;

        if (infuser.animationType != null) {
            if (infuser.animationType == PhysicsInfuserBlockEntity.Animation.ASSEMBLY) {
                float value = infuser.assemblyProgress.getValue();
                float coreOffset = te.getCoreOffset(partialTicks - 1);
                if (value == 500) {
                    doneAnimating = true;
                }
                animateAssembly(core, angle, coreOffset, value, infuser).light(light).renderInto(ms, vb);
                if (value >= 160 && value <= 200 || value >= 300 && value <= 340 || value >= 420 && value <= 440) {
                    animateZapping(zap1,0,coreOffset,1,infuser).light(light).renderInto(ms,buffer.getBuffer(RenderType.translucentMovingBlock()));
                }
                if (value >= 220 && value <= 260 || value >= 360 && value <= 400 || value >= 400 && value <= 420) {
                    animateZapping(zap2,120,coreOffset,2,infuser).light(light).renderInto(ms,buffer.getBuffer(RenderType.translucentMovingBlock()));
                }
                if (value >= 240 && value <= 280 || value >= 320 && value <= 360 || value >= 410 && value <= 430) {
                    animateZapping(zap3,240,coreOffset,3,infuser).light(light).renderInto(ms,buffer.getBuffer(RenderType.translucentMovingBlock()));
                }
                if (doneAnimating == true) {
                    infuser.animationType = PhysicsInfuserBlockEntity.Animation.IDLE;
                    infuser.assembling = false;
                    infuser.isAssembled = true;
                    infuser.assemblyProgress.setValue(0);
                    infuser.initPlayed=false;
                }
            }
            if (infuser.animationType == PhysicsInfuserBlockEntity.Animation.DISASSEMBLY) {
                float value = infuser.disassemblyProgress.getValue();
                //animateDisassembly(core, angle, offset, value).light(light).renderInto(ms, vb);
                if (doneAnimating == true) {
                    infuser.animationType = PhysicsInfuserBlockEntity.Animation.IDLE;
                }
            }

            if (infuser.animationType == PhysicsInfuserBlockEntity.Animation.IDLE) {idleRotateCore(core, angle, offset, infuser).light(light).renderInto(ms, vb);}
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

    private SuperByteBuffer animateZapping(SuperByteBuffer buffer, float angle, float coreOffset, float value, PhysicsInfuserBlockEntity infuser) {
        float pivotX = 8f/16f;
        float pivotY = 0;
        float pivotZ = 8f/16f;
        float transX;
        float transZ;
        if (value == 1) {
            transX = 0f;
            transZ = -0.25f;
        } else if (value == 2) {
            transX = 0.25f;
            transZ = 0.25f;
        } else {
            transX = -0.25f;
            transZ = 0.25f;
        }

        buffer.translateY((coreOffset*2)-0.15f);
        buffer.translate(pivotX, pivotY, pivotZ);
        buffer.rotate(Direction.UP, (float) (angle/180  * Math.PI));
        buffer.translate(transX, 0, transZ);
        buffer.translate(-pivotX, -pivotY, -pivotZ);
        return buffer;
    }
    private SuperByteBuffer animateAssembly(SuperByteBuffer buffer, float angle, float coreOffset, float value, PhysicsInfuserBlockEntity infuser) {

        float interpolatedAngle = infuser.getInterpolatedCoreAngle(AnimationTickHolder.getPartialTicks() - 1);
        buffer.translateY(coreOffset*2).rotateCentered(Direction.UP, (float) (interpolatedAngle / 180 * Math.PI));
        return buffer;
    }


}
