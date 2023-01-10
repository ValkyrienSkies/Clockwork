package org.valkyrienskies.clockwork.fabric.content.contraptions.components.infuser;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix4f;
import com.mojang.math.Quaternion;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.RenderTypes;
import com.simibubi.create.foundation.render.SuperByteBuffer;
import com.simibubi.create.foundation.tileEntity.renderer.SafeTileEntityRenderer;
import com.simibubi.create.foundation.tileEntity.renderer.SmartTileEntityRenderer;
import com.simibubi.create.foundation.utility.AngleHelper;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.valkyrienskies.clockwork.fabric.AllClockworkPartials;
import org.valkyrienskies.clockwork.fabric.ClockWorkModFabric;
import org.valkyrienskies.clockwork.fabric.render.assemblyscan.ScannerRenderer;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;
import javax.annotation.Nullable;
import java.util.*;

import static org.valkyrienskies.clockwork.fabric.content.contraptions.components.infuser.PhysicsInfuserRenderer.ScanManager.*;

public class PhysicsInfuserRenderer extends SmartTileEntityRenderer<PhysicsInfuserBlockEntity> {

    private boolean doneAnimating = false;

    private static PhysicsInfuserBlockEntity te;
    public PhysicsInfuserRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(PhysicsInfuserBlockEntity te, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        super.renderSafe(te, partialTicks, ms, buffer, light, overlay);
        if (!(te instanceof PhysicsInfuserBlockEntity))
            return;

        this.te = te;
        PhysicsInfuserBlockEntity infuser = (PhysicsInfuserBlockEntity) te;
        BlockState blockState = te.getBlockState();

        ScannerRenderer.currentCenterSetter = VectorConversionsMCKt.toMinecraft(VectorConversionsMCKt.toJOMLD(te.getBlockPos()));

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
        private static final int SCAN_GROWTH_DURATION = 2000;
        // Reference render distance the above constants are relative to.
        private static final int REFERENCE_RENDER_DISTANCE = 12;

        // --------------------------------------------------------------------- //

        public static float computeTargetRadius() {
            return Minecraft.getInstance().gameRenderer.getRenderDistance();
        }

        public static int computeScanGrowthDuration() {
            return SCAN_GROWTH_DURATION * Minecraft.getInstance().options.renderDistance / REFERENCE_RENDER_DISTANCE;
        }

        public static float computeRadius(final long start, final float duration) {
            // Scan wave speeds up exponentially. To avoid the initial speed being
            // near zero due to that we offset the time and adjust the remaining
            // parameters accordingly. Base equation is:
            //   r = a + (t + b)^2 * c
            // with r := 0 and target radius and t := 0 and target time this yields:
            //   c = r1/((t1 + b)^2 - b*b)
            //   a = -r1*b*b/((t1 + b)^2 - b*b)

            final float r1 = computeTargetRadius();
            final float t1 = duration;
            final float b = SCAN_TIME_OFFSET;
            final float n = 1f / ((t1 + b) * (t1 + b) - b * b);
            final float a = -r1 * b * b * n;
            final float c = r1 * n;

            final float t = (float) (System.currentTimeMillis() - start);

            return SCAN_INITIAL_RADIUS + a + (t + b) * (t + b) * c;
        }

        // --------------------------------------------------------------------- //

        // List of providers currently used to scan.



        public static int scanningTicks = -1;
        public static long currentStart = -1;
        @Nullable
        public static Vec3 lastScanCenter;

        public static PoseStack viewModelStack;
        public static Matrix4f projectionMatrix;

        // --------------------------------------------------------------------- //

        public static void beginScan(final PhysicsInfuserBlockEntity te) {
            cancelScan();

            float scanRadius = 1000;

            final Vec3 center = Vec3.atCenterOf(te.getBlockPos());

            te.initialize(center, scanRadius, SCAN_COMPUTE_DURATION);

        }

        public static void updateScan(final Entity entity, final boolean finish) {
            final int remaining = SCAN_COMPUTE_DURATION - scanningTicks;

            if (!finish) {
                if (remaining <= 0) {
                    return;
                }

                ++scanningTicks;

                return;
            }

            clear();

            lastScanCenter = entity.position();
            currentStart = System.currentTimeMillis();

            ScannerRenderer.INSTANCE.ping(lastScanCenter);

            cancelScan();
        }

        public static void cancelScan() {
            scanningTicks = 0;
        }

        // --------------------------------------------------------------------- //

        private static void clear() {
            lastScanCenter = null;
            currentStart = -1;
        }
    }


}
