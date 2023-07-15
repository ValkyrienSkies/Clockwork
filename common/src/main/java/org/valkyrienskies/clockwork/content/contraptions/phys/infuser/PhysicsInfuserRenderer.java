package org.valkyrienskies.clockwork.content.contraptions.phys.infuser;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix4f;
import com.mojang.math.Quaternion;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.SuperByteBuffer;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.valkyrienskies.clockwork.ClockWorkPartials;
import org.valkyrienskies.clockwork.content.contraptions.phys.infuser.PhysicsInfuserBlockEntity;

import javax.annotation.Nullable;

public class PhysicsInfuserRenderer extends SmartBlockEntityRenderer<PhysicsInfuserBlockEntity> {

    private PhysicsInfuserBlockEntity te;

    public PhysicsInfuserRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(PhysicsInfuserBlockEntity te, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        super.renderSafe(te, partialTicks, ms, buffer, light, overlay);
        if (!(te instanceof PhysicsInfuserBlockEntity))
            return;

        this.te = te;
        PhysicsInfuserBlockEntity infuser = te;
        BlockState blockState = te.getBlockState();

        VertexConsumer vb = buffer.getBuffer(RenderType.translucent());

        // Render Mysterious Liquid

        SuperByteBuffer mysteriousLiquid = CachedBufferer.partial(ClockWorkPartials.STRANGE_FLUID, blockState);

        mysteriousLiquid.light(light).renderInto(ms, buffer.getBuffer(RenderType.translucent()));
        // Zappin'

        SuperByteBuffer zap1 = null;
        SuperByteBuffer zap2 = null;
        SuperByteBuffer zap3 = null;

        if (infuser.animationType == PhysicsInfuserBlockEntity.Animation.ASSEMBLY) {
            zap1 = CachedBufferer.partial(ClockWorkPartials.ZAP, blockState);
            zap2 = CachedBufferer.partial(ClockWorkPartials.ZAP, blockState);
            zap3 = CachedBufferer.partial(ClockWorkPartials.ZAP, blockState);
        } else {
            zap1 = null;
            zap2 = null;
            zap3 = null;
        }

        // Core

        SuperByteBuffer core = CachedBufferer.partial(ClockWorkPartials.PHYSICS_CORE, blockState);
        float angle = 0;
        float offset = 0;

        if (infuser.animationType != null) {
            if (infuser.animationType == PhysicsInfuserBlockEntity.Animation.ASSEMBLY) {
                float value = infuser.assemblyProgress.getValue();
                float coreOffset = te.getCoreOffset(partialTicks - 1);
                animateAssembly(core, angle, coreOffset, value, infuser).light(light).renderInto(ms, vb);
                if (value >= 160 && value <= 200 || value >= 300 && value <= 340 || value >= 420 && value <= 440) {
                    animateZapping(zap1, 0, coreOffset, 1, infuser).light(light).renderInto(ms, buffer.getBuffer(RenderType.translucentNoCrumbling()));
                }
                if (value >= 220 && value <= 260 || value >= 360 && value <= 400 || value >= 400 && value <= 420) {
                    animateZapping(zap2, 120, coreOffset, 2, infuser).light(light).renderInto(ms, buffer.getBuffer(RenderType.translucentNoCrumbling()));
                }
                if (value >= 240 && value <= 280 || value >= 320 && value <= 360 || value >= 410 && value <= 430) {
                    animateZapping(zap3, 240, coreOffset, 3, infuser).light(light).renderInto(ms, buffer.getBuffer(RenderType.translucentNoCrumbling()));
                }
            }
            if (infuser.animationType == PhysicsInfuserBlockEntity.Animation.DISASSEMBLY) {
                float value = infuser.disassemblyProgress.getValue();
                //animateDisassembly(core, angle, offset, value).light(light).renderInto(ms, vb);
            }

            if (infuser.animationType == PhysicsInfuserBlockEntity.Animation.IDLE) {
                idleRotateCore(core, angle, offset, infuser).light(light).renderInto(ms, vb);
            }
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
        float pivotX = 8f / 16f;
        float pivotY = 0;
        float pivotZ = 8f / 16f;
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

        buffer.translateY((coreOffset * 2) - 0.15f);
        buffer.translate(pivotX, pivotY, pivotZ);
        buffer.rotate(Direction.UP, (float) (angle / 180 * Math.PI));
        buffer.translate(transX, 0, transZ);
        buffer.translate(-pivotX, -pivotY, -pivotZ);
        return buffer;
    }

    private SuperByteBuffer animateAssembly(SuperByteBuffer buffer, float angle, float coreOffset, float value, PhysicsInfuserBlockEntity infuser) {

        float interpolatedAngle = infuser.getInterpolatedCoreAngle(AnimationTickHolder.getPartialTicks() - 1);
        buffer.translateY(coreOffset * 2).rotateCentered(Direction.UP, (float) (interpolatedAngle / 180 * Math.PI));
        return buffer;
    }

    @Environment(EnvType.CLIENT)
    public final class ScanManager {
        // The number of ticks over which to compute scan results. Which is at the
        // same time the use time of the scanner item.
        public static final int SCAN_COMPUTE_DURATION = 40;
        // Initial radius of the scan wave.
        public static final int SCAN_INITIAL_RADIUS = 10;
        // Scan wave growth time offset to avoid super slow start speed.
        public static final int SCAN_TIME_OFFSET = 200;
        // How long the ping takes to reach the end of the visible area.
        public static final int SCAN_GROWTH_DURATION = 2000;
        // Reference render distance the above constants are relative to.
        private static final int REFERENCE_RENDER_DISTANCE = 12;

        // --------------------------------------------------------------------- //
        public static int scanningTicks = -1;

        // --------------------------------------------------------------------- //

        // List of providers currently used to scan.
        public static long currentStart = -1;
        @Nullable
        public static Vec3 lastScanCenter;
        public static PoseStack viewModelStack;
        public static Matrix4f projectionMatrix;

        // --------------------------------------------------------------------- //
        public static void cancelScan() {
            scanningTicks = 0;
        }

        private static void clear() {
            lastScanCenter = null;
            currentStart = -1;
        }

        // --------------------------------------------------------------------- //

        public int computeScanGrowthDuration() {
            return te.getScanGrowthDuration();
        }
    }


}
