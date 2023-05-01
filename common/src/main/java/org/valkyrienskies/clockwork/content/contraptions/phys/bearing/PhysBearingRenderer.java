package org.valkyrienskies.clockwork.content.contraptions.phys.bearing;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import com.simibubi.create.content.contraptions.base.KineticTileEntity;
import com.simibubi.create.content.contraptions.base.KineticTileEntityRenderer;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.SuperByteBuffer;
import com.simibubi.create.foundation.utility.AngleHelper;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.valkyrienskies.clockwork.ClockWorkPartials;

public class PhysBearingRenderer extends KineticTileEntityRenderer {
    public PhysBearingRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(KineticTileEntity te, float partialTicks, PoseStack ms, MultiBufferSource buffer,
                              int light, int overlay) {
        if (!(te instanceof PhysBearingBlockEntity))
            return;


        VertexConsumer vb = buffer.getBuffer(RenderType.translucent());

        PhysBearingBlockEntity pte = (PhysBearingBlockEntity) te;

        final Direction ogfacing = te.getBlockState()
                .getValue(BlockStateProperties.FACING);

        final Direction facing = Direction.UP;
        ms.pushPose();
        ms.translate(0.5, 0.5, 0.5);
        ms.mulPose(Quaternion.fromXYZ(0.0f, (float) Math.toRadians(-180.0), 0.0f));
        switch (ogfacing) {
            case SOUTH -> ms.mulPose(Vector3f.XP.rotationDegrees(270));
            case WEST -> ms.mulPose(Vector3f.ZP.rotationDegrees(270));
            case NORTH -> ms.mulPose(Vector3f.XP.rotationDegrees(90));
            case EAST -> ms.mulPose(Vector3f.ZP.rotationDegrees(90));
            case UP -> ms.mulPose(Vector3f.XP.rotationDegrees(0));
            case DOWN -> ms.mulPose(Vector3f.XN.rotationDegrees(180));
        }
        ms.translate(-0.5, -0.5, -0.5);
        BlockState blockState = te.getBlockState();

        SuperByteBuffer core = CachedBufferer.partial(ClockWorkPartials.PHYSICS_CORE, blockState);
        SuperByteBuffer flapNorth = CachedBufferer.partial(ClockWorkPartials.PHYSFLAP_NORTH, blockState);
        SuperByteBuffer flapSouth = CachedBufferer.partial(ClockWorkPartials.PHYSFLAP_SOUTH, blockState);
        SuperByteBuffer flapEast = CachedBufferer.partial(ClockWorkPartials.PHYSFLAP_EAST, blockState);
        SuperByteBuffer flapWest = CachedBufferer.partial(ClockWorkPartials.PHYSFLAP_WEST, blockState);

        SuperByteBuffer cornerNE = CachedBufferer.partial(ClockWorkPartials.PHYSCORNER_NE, blockState);
        SuperByteBuffer cornerNW = CachedBufferer.partial(ClockWorkPartials.PHYSCORNER_NW, blockState);
        SuperByteBuffer cornerSE = CachedBufferer.partial(ClockWorkPartials.PHYSCORNER_SE, blockState);
        SuperByteBuffer cornerSW = CachedBufferer.partial(ClockWorkPartials.PHYSCORNER_SW, blockState);

        idleRotateCore(core, pte.getCoreOffset(partialTicks)+4/16f, pte);

        rotateFlap(flapNorth, pte, 1, facing);
        rotateFlap(flapEast, pte, 2, facing);
        rotateFlap(flapSouth, pte, 3, facing);
        rotateFlap(flapWest, pte, 4, facing);

        translateCorner(cornerNE, pte, 1, facing);
        translateCorner(cornerNW, pte, 2, facing);
        translateCorner(cornerSE, pte, 3, facing);
        translateCorner(cornerSW, pte, 4, facing);

        if (facing.getAxis().isHorizontal()) {
            core.rotateCentered(Direction.UP,
                    AngleHelper.rad(AngleHelper.horizontalAngle(facing.getOpposite())));
            flapNorth.rotateCentered(Direction.UP,
                    AngleHelper.rad(AngleHelper.horizontalAngle(facing.getOpposite())));
            flapEast.rotateCentered(Direction.UP,
                    AngleHelper.rad(AngleHelper.horizontalAngle(facing.getOpposite())));
            flapSouth.rotateCentered(Direction.UP,
                    AngleHelper.rad(AngleHelper.horizontalAngle(facing.getOpposite())));
            flapWest.rotateCentered(Direction.UP,
                    AngleHelper.rad(AngleHelper.horizontalAngle(facing.getOpposite())));
            cornerNE.rotateCentered(Direction.UP,
                    AngleHelper.rad(AngleHelper.horizontalAngle(facing.getOpposite())));
            cornerNW.rotateCentered(Direction.UP,
                    AngleHelper.rad(AngleHelper.horizontalAngle(facing.getOpposite())));
            cornerSE.rotateCentered(Direction.UP,
                    AngleHelper.rad(AngleHelper.horizontalAngle(facing.getOpposite())));
            cornerSW.rotateCentered(Direction.UP,
                    AngleHelper.rad(AngleHelper.horizontalAngle(facing.getOpposite())));
        }

        core.rotateCentered(Direction.EAST, AngleHelper.rad(-90 - AngleHelper.verticalAngle(facing)));

        flapEast.rotateCentered(Direction.EAST, AngleHelper.rad(-90 - AngleHelper.verticalAngle(facing)));
        flapNorth.rotateCentered(Direction.EAST, AngleHelper.rad(-90 - AngleHelper.verticalAngle(facing)));
        flapSouth.rotateCentered(Direction.EAST, AngleHelper.rad(-90 - AngleHelper.verticalAngle(facing)));
        flapWest.rotateCentered(Direction.EAST, AngleHelper.rad(-90 - AngleHelper.verticalAngle(facing)));

        cornerNE.rotateCentered(Direction.EAST, AngleHelper.rad(-90 - AngleHelper.verticalAngle(facing)));
        cornerSW.rotateCentered(Direction.EAST, AngleHelper.rad(-90 - AngleHelper.verticalAngle(facing)));
        cornerSE.rotateCentered(Direction.EAST, AngleHelper.rad(-90 - AngleHelper.verticalAngle(facing)));
        cornerNW.rotateCentered(Direction.EAST, AngleHelper.rad(-90 - AngleHelper.verticalAngle(facing)));

        core.renderInto(ms, vb);

        flapNorth.renderInto(ms, vb);
        flapEast.renderInto(ms, vb);
        flapSouth.renderInto(ms, vb);
        flapWest.renderInto(ms, vb);

        cornerNE.renderInto(ms, vb);
        cornerNW.renderInto(ms, vb);
        cornerSE.renderInto(ms, vb);
        cornerSW.renderInto(ms, vb);

        ms.popPose();
    }

    private SuperByteBuffer idleRotateCore(SuperByteBuffer buffer, float offset, PhysBearingBlockEntity bearing) {
        LerpedFloat speen = LerpedFloat.linear();
        float interpolatedAngle = bearing.getInterpolatedCoreAngle(AnimationTickHolder.getPartialTicks() - 1);
        buffer.rotateCentered(Direction.UP, (float) (interpolatedAngle / 180 * Math.PI)).translate(0, offset, 0);
        return buffer;
    }

    private SuperByteBuffer rotateFlap(SuperByteBuffer buffer, PhysBearingBlockEntity bearing, int ordinal, Direction facing) {

        float pivotX = 8f;
        float pivotY = 8f;
        float pivotZ = 8f;

        Direction direction = bearing.getBlockState().getValue(BlockStateProperties.FACING);

        switch (ordinal) {
            case 1 -> {
                pivotX = 0;
                pivotY = 2/16f;
                pivotZ = -6/16f;
                direction = Direction.WEST;
            }
            case 2 -> {
                pivotX = 5/16f;
                pivotY = 2/16f;
                pivotZ = 0;
                direction = Direction.NORTH;
            }
            case 3 -> {
                pivotX = 0;
                pivotY = 2/16f;
                pivotZ = 6/16f;
                direction = Direction.EAST;
            }
            case 4 -> {
                pivotX = -5/16f;
                pivotY = 2/16f;
                pivotZ = 0;
                direction = Direction.SOUTH;
            }
        }

        final Vec3 pivot = switch (facing) {
            case UP -> new Vec3(pivotX, pivotY, pivotZ);
            case NORTH -> new Vec3(pivotX, pivotZ, pivotY);
            case SOUTH -> new Vec3(pivotX, pivotZ, -pivotY);
            case EAST -> new Vec3(pivotY, pivotX, pivotZ);
            case WEST -> new Vec3(-pivotY, pivotX, pivotZ);
            case DOWN -> new Vec3(-pivotX, -pivotY, -pivotZ);
        };

        float interpolatedAngle = bearing.getFlapRotOffset(AnimationTickHolder.getPartialTicks() - 1);
//        buffer.translate(-pivotX, -pivotY, -pivotZ);
        buffer.translate(pivot);
        buffer.rotateCentered(direction, (float) (interpolatedAngle / 180 * Math.PI));
        buffer.translateBack(pivot);
        return buffer;
    }

    private SuperByteBuffer translateCorner(SuperByteBuffer buffer, PhysBearingBlockEntity bearing, int ordinal, Direction facing) {
        int xSwitch = switch (ordinal) {
          case 1 -> 1;
          case 2 -> -1;
          case 3 -> 1;
          case 4 -> -1;
          default -> 1;
        };

        int zSwitch = switch (ordinal) {
            case 1 -> -1;
            case 2 -> -1;
            case 3 -> 1;
            case 4 -> 1;
            default -> 1;
        };

        float interpolatedHorizontalOffset = bearing.getCornerHorizontalOffset(AnimationTickHolder.getPartialTicks() - 1);
        float interpolatedVerticalOffset = bearing.getCornerVerticalOffset(AnimationTickHolder.getPartialTicks() - 1);
        Vec3 translate = switch(facing) {
            case UP -> new Vec3(interpolatedHorizontalOffset * xSwitch, interpolatedVerticalOffset, interpolatedHorizontalOffset * zSwitch);
            case NORTH -> new Vec3(interpolatedHorizontalOffset * xSwitch, interpolatedHorizontalOffset * zSwitch, -interpolatedVerticalOffset);
            case SOUTH -> new Vec3(interpolatedHorizontalOffset * zSwitch, interpolatedHorizontalOffset * xSwitch, interpolatedVerticalOffset);
            case EAST -> new Vec3(-interpolatedVerticalOffset, interpolatedHorizontalOffset * xSwitch, interpolatedHorizontalOffset * zSwitch);
            case WEST -> new Vec3(interpolatedVerticalOffset, interpolatedHorizontalOffset * zSwitch, interpolatedHorizontalOffset * xSwitch);
            case DOWN -> new Vec3(interpolatedHorizontalOffset * xSwitch, -interpolatedVerticalOffset, interpolatedHorizontalOffset * zSwitch);
        };
        buffer.translate(translate);
        return buffer;
    }
}
