package org.valkyrienskies.clockwork.fabric.content.curiosities.tools.bluperglue;

import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.contraptions.components.structureMovement.BlockMovementChecks;
import com.simibubi.create.content.schematics.ISpecialEntityItemRequirement;
import com.simibubi.create.content.schematics.ItemRequirement;
import com.simibubi.create.content.schematics.ItemRequirement.ItemUseType;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.VecHelper;
import io.github.fabricators_of_create.porting_lib.entity.ExtraSpawnDataEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.valkyrienskies.clockwork.fabric.AllClockworkEntities;
import org.valkyrienskies.clockwork.fabric.AllClockworkItems;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BluperGlueEntity extends Entity
        implements ExtraSpawnDataEntity, ISpecialEntityItemRequirement {

    public BluperGlueEntity(EntityType<?> type, Level world) {
        super(type, world);
    }

    public BluperGlueEntity(Level world, AABB boundingBox) {
        this(AllClockworkEntities.BLUPERGLUE.get(), world);
        setBoundingBox(boundingBox);
        resetPositionToBB();
    }

    public static AABB span(BlockPos startPos, BlockPos endPos) {
        return new AABB(startPos, endPos).expandTowards(1, 1, 1);
    }

    public static boolean isBluGlued(LevelAccessor level, BlockPos blockPos, Direction direction,
                                     Set<BluperGlueEntity> cached) {
        BlockPos targetPos = blockPos.relative(direction);
        if (cached != null)
            for (BluperGlueEntity glueEntity : cached)
                if (glueEntity.contains(blockPos) && glueEntity.contains(targetPos))
                    return true;
        for (BluperGlueEntity glueEntity : level.getEntitiesOfClass(BluperGlueEntity.class,
                span(blockPos, targetPos).inflate(16))) {
            if (!glueEntity.contains(blockPos) || !glueEntity.contains(targetPos))
                continue;
            if (cached != null)
                cached.add(glueEntity);
            return true;
        }
        return false;
    }

    public static List<BluperGlueEntity> collectCropped(Level level, AABB bb) {
        List<BluperGlueEntity> glue = new ArrayList<>();
        for (BluperGlueEntity glueEntity : level.getEntitiesOfClass(BluperGlueEntity.class, bb)) {
            AABB glueBox = glueEntity.getBoundingBox();
            AABB intersect = bb.intersect(glueBox);
            if (intersect.getXsize() * intersect.getYsize() * intersect.getZsize() == 0)
                continue;
            if (Mth.equal(intersect.getSize(), 1))
                continue;
            glue.add(new BluperGlueEntity(level, intersect));
        }
        return glue;
    }

    public static boolean isValidFace(Level world, BlockPos pos, Direction direction) {
        BlockState state = world.getBlockState(pos);
        if (BlockMovementChecks.isBlockAttachedTowards(state, world, pos, direction))
            return true;
        if (!BlockMovementChecks.isMovementNecessary(state, world, pos))
            return false;
        if (BlockMovementChecks.isNotSupportive(state, direction))
            return false;
        return true;
    }

    public static void writeBoundingBox(CompoundTag compound, AABB bb) {
        compound.put("From", VecHelper.writeNBT(new Vec3(bb.minX, bb.minY, bb.minZ)));
        compound.put("To", VecHelper.writeNBT(new Vec3(bb.maxX, bb.maxY, bb.maxZ)));
    }

    public static AABB readBoundingBox(CompoundTag compound) {
        Vec3 from = VecHelper.readNBT(compound.getList("From", Tag.TAG_DOUBLE));
        Vec3 to = VecHelper.readNBT(compound.getList("To", Tag.TAG_DOUBLE));
        return new AABB(from, to);
    }

    public static FabricEntityTypeBuilder<?> build(FabricEntityTypeBuilder<?> builder) {
//		@SuppressWarnings("unchecked")
//		EntityType.Builder<BluperGlueEntity> entityBuilder = (EntityType.Builder<BluperGlueEntity>) builder;
        return builder;
    }

    public void resetPositionToBB() {
        AABB bb = getBoundingBox();
        setPosRaw(bb.getCenter().x, bb.minY, bb.getCenter().z);
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (getBoundingBox().getXsize() == 0)
            discard();
    }

    @Override
    public void setPos(double x, double y, double z) {
        AABB bb = getBoundingBox();
        setPosRaw(x, y, z);
        Vec3 center = bb.getCenter();
        setBoundingBox(bb.move(-center.x, -bb.minY, -center.z)
                .move(x, y, z));
    }

    @Override
    public void move(MoverType typeIn, Vec3 pos) {
        if (!level.isClientSide && isAlive() && pos.lengthSqr() > 0.0D)
            discard();
    }

    @Override
    public void push(double x, double y, double z) {
        if (!level.isClientSide && isAlive() && x * x + y * y + z * z > 0.0D)
            discard();
    }

    @Override
    protected float getEyeHeight(Pose poseIn, EntityDimensions sizeIn) {
        return 0.0F;
    }

    public void playPlaceSound() {
        AllSoundEvents.SLIME_ADDED.playFrom(this, 0.5F, 0.75F);
    }

    @Override
    public void push(Entity entityIn) {
        super.push(entityIn);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        return InteractionResult.PASS;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        Vec3 position = position();
        writeBoundingBox(compound, getBoundingBox().move(position.scale(-1)));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        Vec3 position = position();
        setBoundingBox(readBoundingBox(compound).move(position));
    }

    @Override
    protected boolean repositionEntityAfterLoad() {
        return false;
    }

    @Override
    public float rotate(Rotation transformRotation) {
        AABB bb = getBoundingBox().move(position().scale(-1));
        if (transformRotation == Rotation.CLOCKWISE_90 || transformRotation == Rotation.COUNTERCLOCKWISE_90)
            setBoundingBox(new AABB(bb.minZ, bb.minY, bb.minX, bb.maxZ, bb.maxY, bb.maxX).move(position()));
        return super.rotate(transformRotation);
    }

    @Override
    public float mirror(Mirror transformMirror) {
        return super.mirror(transformMirror);
    }

    @Override
    public void thunderHit(ServerLevel world, LightningBolt lightningBolt) {
    }

    @Override
    public void refreshDimensions() {
    }

    @Override
    public Packet<?> getAddEntityPacket() {
        return ExtraSpawnDataEntity.createExtraDataSpawnPacket(this);
    }

    @Override
    public void writeSpawnData(FriendlyByteBuf buffer) {
        CompoundTag compound = new CompoundTag();
        addAdditionalSaveData(compound);
        buffer.writeNbt(compound);
    }

    @Override
    public void readSpawnData(FriendlyByteBuf additionalData) {
        readAdditionalSaveData(additionalData.readNbt());
    }

    @Override
    public ItemRequirement getRequiredItems() {
        return new ItemRequirement(ItemUseType.DAMAGE, AllClockworkItems.BLUPERGLUE.get());
    }

    @Override
    public boolean isIgnoringBlockTriggers() {
        return true;
    }

    public boolean contains(BlockPos pos) {
        return getBoundingBox().contains(Vec3.atCenterOf(pos));
    }

    @Override
    public PushReaction getPistonPushReaction() {
        return PushReaction.IGNORE;
    }

    @Override
    public PortalInfo findDimensionEntryPoint(ServerLevel pDestination) {
        portalEntrancePos = blockPosition();
        return super.findDimensionEntryPoint(pDestination);
    }

    public void spawnParticles() {
        AABB bb = getBoundingBox();
        Vec3 origin = new Vec3(bb.minX, bb.minY, bb.minZ);
        Vec3 extents = new Vec3(bb.getXsize(), bb.getYsize(), bb.getZsize());

        if (!(level instanceof ServerLevel slevel))
            return;

        for (Axis axis : Iterate.axes) {
            AxisDirection positive = AxisDirection.POSITIVE;
            double max = axis.choose(extents.x, extents.y, extents.z);
            Vec3 normal = Vec3.atLowerCornerOf(Direction.fromAxisAndDirection(axis, positive)
                    .getNormal());
            for (Axis axis2 : Iterate.axes) {
                if (axis2 == axis)
                    continue;
                double max2 = axis2.choose(extents.x, extents.y, extents.z);
                Vec3 normal2 = Vec3.atLowerCornerOf(Direction.fromAxisAndDirection(axis2, positive)
                        .getNormal());
                for (Axis axis3 : Iterate.axes) {
                    if (axis3 == axis2 || axis3 == axis)
                        continue;
                    double max3 = axis3.choose(extents.x, extents.y, extents.z);
                    Vec3 normal3 = Vec3.atLowerCornerOf(Direction.fromAxisAndDirection(axis3, positive)
                            .getNormal());

                    for (int i = 0; i <= max * 2; i++) {
                        for (int o1 : Iterate.zeroAndOne) {
                            for (int o2 : Iterate.zeroAndOne) {
                                Vec3 v = origin.add(normal.scale(i / 2f))
                                        .add(normal2.scale(max2 * o1))
                                        .add(normal3.scale(max3 * o2));

                                slevel.sendParticles(ParticleTypes.ITEM_SLIME, v.x, v.y, v.z, 1, 0, 0, 0, 0);

                            }
                        }
                    }
                    break;
                }
                break;
            }
        }
    }
}
