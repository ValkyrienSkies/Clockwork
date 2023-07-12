package org.valkyrienskies.clockwork.content.kinetics.combustion_engine;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.valkyrienskies.clockwork.ClockWorkPartials;

public class CombustionEngineRenderer extends KineticBlockEntityRenderer<CombustionEngineBlockEntity> {
    public CombustionEngineRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(CombustionEngineBlockEntity te, float partialTicks, PoseStack ms, MultiBufferSource buffer,
                              int light, int overlay) {
        super.renderSafe(te, partialTicks, ms, buffer, light, overlay);

//        if (Backend.canUseInstancing(te.getLevel()))
//            return;

        BlockState blockState = te.getBlockState();
        float speed = 0;
        if (te.getSpeed() != 0) {
            speed = te.getSpeed();
        }
        final SuperByteBuffer engine = CachedBufferer.partialFacing(ClockWorkPartials.ENGINE, te.getBlockState());

        CombustionEngineBlockEntity combustionEngineBlockEntity = (CombustionEngineBlockEntity) te;

        // Assume the pistons are spinning 4x faster than the engine shaft
        final float PISTON_SHAFT_GEAR_RATIO = 4.0f;
        float visualSpeed = combustionEngineBlockEntity.visualSpeed.getValue(partialTicks) * (3 / 10f) * PISTON_SHAFT_GEAR_RATIO;
        float visualAngle = (combustionEngineBlockEntity.angle * PISTON_SHAFT_GEAR_RATIO) + visualSpeed * partialTicks;

        renderPistons(ms, buffer, blockState, light, visualAngle);
        shakeEngine(engine, speed, partialTicks, te).light(light).renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));
    }

    private SuperByteBuffer shakeEngine(SuperByteBuffer buffer, float speed, float partialTicks, CombustionEngineBlockEntity te) {
        // Clamp speed to be at most 48 (because of clipping issues)
        speed = Mth.clamp(speed, -48.0f, 48.0f);
        float offsetX = (float) ((((Math.sin(partialTicks) + Math.sin(partialTicks * 2) + Math.sin(partialTicks * -0.5)) * 2) / 3) / 100) * (speed / 32);
        float offsetY = (float) ((((Math.sin(partialTicks) + Math.sin(partialTicks * 0.5) + Math.sin(partialTicks * 3)) * 2) / 3) / 100) * (speed / 32);
        float offsetZ = (float) ((((Math.sin(partialTicks) + Math.sin(partialTicks * 4) + Math.sin(partialTicks * -1)) * 2) / 3) / 100) * (speed / 32);

        Direction.Axis axis = te.getBlockState().getValue(BlockStateProperties.FACING).getAxis();
        if (axis == Direction.Axis.X) {
            offsetX = 0;
        } else if (axis == Direction.Axis.Y) {
            offsetY = 0;
        } else if (axis == Direction.Axis.Z) {
            offsetZ = 0;
        }
        buffer.translate(offsetX, offsetY, offsetZ);
        return buffer;
    }

    @Override
    protected BlockState getRenderedBlockState(CombustionEngineBlockEntity te) {
        return shaft(getRotationAxisOf(te));
    }

    private static void renderPistons(final PoseStack ms, final MultiBufferSource buffer, final BlockState blockState, final int light, final double engineAngleDegrees) {
        final Direction direction = blockState.getValue(BlockStateProperties.FACING);

        ms.pushPose();
        ms.translate(0.5, 0.5, 0.5);
        ms.mulPose(Quaternion.fromXYZ(0.0f, (float) Math.toRadians(-180.0), 0.0f));
        switch (direction) {
            case SOUTH -> ms.mulPose(Vector3f.YP.rotationDegrees(180));
            case WEST -> ms.mulPose(Vector3f.YP.rotationDegrees(90));
            case NORTH -> ms.mulPose(Vector3f.YP.rotationDegrees(0));
            case EAST -> ms.mulPose(Vector3f.YP.rotationDegrees(270));
            case UP -> ms.mulPose(Vector3f.XP.rotationDegrees(90));
            case DOWN -> ms.mulPose(Vector3f.XN.rotationDegrees(90));
        }
        ms.translate(-0.5, -0.5, -0.5);

        for (final PistonData pistonData : PistonData.values()) {
            SuperByteBuffer pistonModel = CachedBufferer.partialFacing(ClockWorkPartials.SINGLE_ENGINE_PISTON, blockState.setValue(BlockStateProperties.FACING, Direction.SOUTH));

            pistonModel.translate(pistonData.pistonRotationCenter.x() / 16.0, pistonData.pistonRotationCenter.y() / 16.0, pistonData.pistonRotationCenter.z() / 16.0);
            pistonModel.rotateZ(pistonData.zAngle);

            // Add the offset to the piston
            pistonModel.translate(0.0, pistonData.getPistonOffset(engineAngleDegrees), 0.0);

            pistonModel.translate(-pistonData.pistonRotationCenter.x() / 16.0, -pistonData.pistonRotationCenter.y() / 16.0, -pistonData.pistonRotationCenter.z() / 16.0);

            // Subtract 1.0, 1.0, 1.0 since that's the center of SINGLE_ENGINE_PISTON
            pistonModel.translate((pistonData.pistonCenter.x() - 1.0) / 16.0, (pistonData.pistonCenter.y() - 1.0) / 16.0, (pistonData.pistonCenter.z() - 1.0) / 16.0);

            pistonModel.light(light).renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));
        }
        ms.popPose();
    }

    private enum PistonData {
        SECOND(new Vector3d(1.0, 14.0, 3.0), new Vector3d(1.0, 15.0, 3.0), 45.0, 0.0),
        FIRST(new Vector3d(1.0, 14.0, 8.0), new Vector3d(1.0, 15.0, 8.0), 45.0, 120.0),
        THIRD(new Vector3d(1.0, 14.0, 13.0), new Vector3d(1.0, 15.0, 13.0), 45.0, 240.0),
        SIXTH(new Vector3d(15.0, 14.0, 3.0), new Vector3d(15.0, 15.0, 3.0), -45.0, 180.0),
        FOURTH(new Vector3d(15.0, 14.0, 8.0), new Vector3d(15.0, 15.0, 8.0), -45.0, 300.0),
        FIFTH(new Vector3d(15.0, 14.0, 13.0), new Vector3d(15.0, 15.0, 13.0), -45.0, 60.0);

        private final Vector3dc pistonCenter;
        private final Vector3dc pistonRotationCenter;
        private final double zAngle;

        // Piston offset, in degrees
        private final double pistonOffsetAngle;

        PistonData(final Vector3dc pistonCenter, final Vector3dc pistonRotationCenter, final double zAngle, final double pistonOffsetAngle) {
            this.pistonCenter = pistonCenter;
            this.pistonRotationCenter = pistonRotationCenter;
            this.zAngle = zAngle;
            this.pistonOffsetAngle = pistonOffsetAngle;
        }

        public double getPistonOffset(final double engineAngleDegrees) {
            final double pistonOffsetMagnitude = 0.75 / 32.0;
            return pistonOffsetMagnitude * Math.sin(Math.toRadians(engineAngleDegrees + pistonOffsetAngle)) - (0.75 / 32.0);
        }
    }
}
