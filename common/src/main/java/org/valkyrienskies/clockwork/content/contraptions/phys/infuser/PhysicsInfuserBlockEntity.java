package org.valkyrienskies.clockwork.content.contraptions.phys.infuser;

import com.simibubi.create.AllEntityTypes;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.AssemblyException;
import com.simibubi.create.content.contraptions.actors.seat.SeatEntity;
import com.simibubi.create.content.contraptions.glue.SuperGlueEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.primitives.AABBic;
import org.valkyrienskies.clockwork.ClockWorkMod;
import org.valkyrienskies.clockwork.ClockWorkPackets;
import org.valkyrienskies.clockwork.ClockWorkSounds;
import org.valkyrienskies.clockwork.client.render.scanner.ScannerRenderer;
import org.valkyrienskies.clockwork.content.curiosities.tools.auric_designator.AreaDesignatorItem;
import org.valkyrienskies.clockwork.platform.api.GlueType;
import org.valkyrienskies.clockwork.util.assemble.GlueAssembler;
import org.valkyrienskies.core.api.ships.ClientShip;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.core.impl.datastructures.DenseBlockPosSet;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.assembly.ShipAssemblyKt;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.valkyrienskies.clockwork.content.contraptions.phys.infuser.PhysicsInfuserRenderer.ScanManager.SCAN_GROWTH_DURATION;
import static org.valkyrienskies.clockwork.util.animation.EaseHelper.easeInBounce;

public class PhysicsInfuserBlockEntity extends SmartBlockEntity implements WorldlyContainer {

    public static final int ASSEMBLY_TIME = 500;
    public static final int DISASSEMBLY_TIME = 1000;
    private final Vec3 thisposition = VectorConversionsMCKt.toMinecraft(VectorConversionsMCKt.toJOMLD(worldPosition));
    public boolean isAssembled = false;
    public boolean assembling = false;
    public boolean disassembling = false;
    public Animation animationType = Animation.IDLE;
    public LerpedFloat assemblyProgress = LerpedFloat.linear();
    public LerpedFloat disassemblyProgress = LerpedFloat.linear();
    public LerpedFloat idleProgress = LerpedFloat.linear();
    protected AssemblyException lastException;
    boolean skippingAssembly = false;
    float coreAngle = 0;
    float previousCoreAngle = 0;
    int useCooldown = 0;
    boolean onCooldown = false;
    boolean initPlayed = false;
    private Ship ship = null;

    private Set<Ship> createdShips = new HashSet<>();
    private boolean sendAnimationUpdate;

    private Set<Set<AABBic>> toDump = new HashSet<>();


    public boolean shouldEjectDesignator = false;
    NonNullList<ItemStack> inventory = NonNullList.withSize(1, ItemStack.EMPTY);

    @Override
    public int[] getSlotsForFace(Direction side) {
        return new int[0];
    }

    @Override
    public boolean canPlaceItemThroughFace(int index, ItemStack itemStack, @Nullable Direction direction) {
        return itemStack.getItem() instanceof AreaDesignatorItem;
    }

    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
        return true;
    }

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return inventory.get(0).isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return inventory.get(0);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return inventory.remove(0);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return inventory.remove(0);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        inventory.set(0, stack);
    }

    @Override
    public void setChanged() {
        ClockWorkPackets.sendToNear(getLevel(), worldPosition, 64, new PhysicsInfuserSyncPacket(this));
        super.setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        inventory.set(0, ItemStack.EMPTY);
    }

    public PhysicsInfuserBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public static void playInitializeSound(Level world, Vec3 location) {
        ClockWorkSounds.PHYSICS_INFUSER_INITIALIZE.playAt(world, location, 1, 1, false);
    }

    public static void playWindupSound(Level world, Vec3 location) {
        ClockWorkSounds.PHYSICS_INFUSER_WINDUP.playAt(world, location, 1, 1, false);
    }

    public static void playZapSound(Level world, Vec3 location, Random rand) {
        float pitch = 0.6F + rand.nextFloat() * 0.4F;
        ClockWorkSounds.PHYSICS_INFUSER_LIGHTNING.playAt(world, location, 1, 1, false);
    }

    public static void playFinishSound(Level world, Vec3 location) {
        ClockWorkSounds.PHYSICS_INFUSER_FINISH.playAt(world, location, 1, 1, false);
    }

    public static void spawnParticlesAssembly(Level world, Vec3 pos, Random rand) {
        double degrees = rand.nextDouble() * 360;

        double angle = Math.toRadians(degrees);

        double radius = 2.0D;

        double x = radius * Math.cos(angle);
        double y = 0.5d;
        double z = radius * Math.sin(angle);
    }

    public void initialize(Vec3 center, float scanRadius, int scanComputeDuration) {
    }

    public Ship getConnectedShip() {
        return ship;
    }

    @Override
    public void tick() {
        super.tick();

        if (level == null) return;

        if (shouldEjectDesignator) {
            shouldEjectDesignator = false;
            if (inventory.get(0).isEmpty()) return;
            int launchForce = 0;
            for (Set<AABBic> cluster : toDump) {
                AreaDesignatorItem adi = (AreaDesignatorItem) inventory.get(0).getItem();
                adi.dumpCluster(cluster);
                launchForce++;
            }
            toDump.clear();
            ItemEntity ejected = new ItemEntity(level, getBlockPos().getX(), getBlockPos().getY()+1, getBlockPos().getZ(), inventory.get(0));
            inventory.set(0, ItemStack.EMPTY);
            ejected.setDeltaMovement(new Vec3(0, launchForce, 0));
            level.addFreshEntity(ejected);
        }

        if (level instanceof ServerLevel s) {
            ship = VSGameUtilsKt.getShipObjectManagingPos(s, worldPosition);
        }

        if (useCooldown > 0) {
            onCooldown = true;
            useCooldown--;
        }

        if (useCooldown == 0) {
            onCooldown = false;
        }

        if (animationType == null) {
            animationType = Animation.IDLE;
        }
        if (animationType == Animation.IDLE) {
            startAnimation(Animation.IDLE);
        }

        if (assembling) {
            assemblyProgress.setValue(assemblyProgress.getValue() + 1);
        }
//        if (disassembling) {
//            disassemblyProgress.setValue(disassemblyProgress.getValue() + 1);
//        }
        Random rand = level.getRandom();
        //client sounds

        ClockWorkMod.LOGGER.info(inventory.get(0).toString());

        if (assembling) {
            if (assemblyProgress.getValue() == 0) {
                playInitializeSound(level, thisposition);
            }
            if (assemblyProgress.getValue() == 100) {
                playWindupSound(level, thisposition);
            }
            if (assemblyProgress.getValue() == 160 || assemblyProgress.getValue() == 220 || assemblyProgress.getValue() == 240 || assemblyProgress.getValue() == 300 || assemblyProgress.getValue() == 320 || assemblyProgress.getValue() == 360 || assemblyProgress.getValue() == 400 || assemblyProgress.getValue() == 410 || assemblyProgress.getValue() == 420) {
                playZapSound(level, thisposition, rand);
            }
            if (assemblyProgress.getValue() == 455) {
                assemble();
            }
            if (assemblyProgress.getValue() == 460) {
                playFinishSound(level, thisposition);
                if (level.isClientSide) {
                    for (Ship cship : createdShips) {
                        ScannerRenderer.INSTANCE.ping((ClientShip) cship, thisposition, this);
                        createdShips.remove(cship);
                    }
                }
            }
            if (assemblyProgress.getValue() == 500) {
                resetAfterAssemble();
            }
        }
    }

    private void resetAfterAssemble() {
//        isAssembled = true;
        assembling = false;
        skippingAssembly = false;
        initPlayed = false;
        animationType = Animation.IDLE;
        startAnimation(Animation.IDLE);
        assemblyProgress.startWithValue(0);
        useCooldown = 400;

        shouldEjectDesignator = true;
    }

    //Ship Assembly Handlers
    public void startAssembly() {
        assembling = true;
        this.animationType = Animation.ASSEMBLY;
        startAnimation(Animation.ASSEMBLY);
    }

    public void skipAssembly() {
        skippingAssembly = true;
    }

    public void startDisassembly() {
        disassembling = true;
        this.animationType = Animation.DISASSEMBLY;
        startAnimation(Animation.DISASSEMBLY);
    }

    public double getPulseRange() {
        if (this.ship != null) {
            AABBic shipAABB = ship.getShipAABB();
            Vector3d max = new Vector3d(shipAABB.maxX(), shipAABB.maxY(), shipAABB.maxZ());
            Vector3d min = new Vector3d(shipAABB.minX(), shipAABB.minY(), shipAABB.minZ());

            double range = max.distance(min);

            return range;
        } else {
            return Minecraft.getInstance().gameRenderer.getRenderDistance();
        }
    }

    //Animation Jargon

    public int getScanGrowthDuration() {
        if (this.ship != null) {
            double range = getPulseRange();
            return SCAN_GROWTH_DURATION * (int) range / 12;
        }
        return SCAN_GROWTH_DURATION * Minecraft.getInstance().options.renderDistance / 12;
    }

    public float computeRadius(final long start, final float duration) {
        // Scan wave speeds up exponentially. To avoid the initial speed being
        // near zero due to that we offset the time and adjust the remaining
        // parameters accordingly. Base equation is:
        //   r = a + (t + b)^2 * c
        // with r := 0 and target radius and t := 0 and target time this yields:
        //   c = r1/((t1 + b)^2 - b*b)
        //   a = -r1*b*b/((t1 + b)^2 - b*b)

        final float r1 = (float) getPulseRange();
        final float t1 = duration;
        final float b = 200;
        final float n = 1f / ((t1 + b) * (t1 + b) - b * b);
        final float a = -r1 * b * b * n;
        final float c = r1 * n;

        final float t = (float) (System.currentTimeMillis() - start);

        return 10 + a + (t + b) * (t + b) * c;
    }

    public void assemble() {
        if (getLevel().isClientSide()) return;

        if (!(inventory.get(0).getItem() instanceof AreaDesignatorItem)) return;

        AreaDesignatorItem item = (AreaDesignatorItem) inventory.get(0).getItem();
        item.selectionClusters.forEach((cluster) -> {
            DenseBlockPosSet selection;
            Set<Entity> caughtEntities;
            if (level instanceof ServerLevel sLevel) {
                selection = item.denseBlocksFromCluster(cluster);
                caughtEntities = item.entitiesFromCluster(cluster, sLevel);
                if (selection == null) return;

                ship = ShipAssemblyKt.createNewShipWithBlocks(worldPosition, selection, sLevel);
                // TODO: relocate entities
                if (caughtEntities != null) {
                    caughtEntities.forEach(entity -> {
                        if (entity instanceof AbstractContraptionEntity || entity instanceof SuperGlueEntity || entity instanceof SeatEntity) {
                            Vector3dc newPos = ship.getTransform().getWorldToShip().transformPosition(VectorConversionsMCKt.toJOML(entity.position()));
                            entity.setPos(VectorConversionsMCKt.toMinecraft(newPos));
                        }
                    });
                }

                createdShips.add(ship);
            }
            toDump.add(cluster);
        });
    }

    public void disassemble() {

    }

    public void startAnimation(Animation animation) {
        animationType = animation;
        if (animation == Animation.ASSEMBLY) {
            assemblyProgress.startWithValue(0);
        } else if (animation == Animation.DISASSEMBLY) {
            disassemblyProgress.startWithValue(0);
        } else if (animation == Animation.IDLE) {
            idleProgress.startWithValue(0);
        }
        sendAnimationUpdate = true;

        sendData();
    }

    public float getInterpolatedCoreAngle(float partialTicks) {

        previousCoreAngle = coreAngle;

        coreAngle++;

        if (coreAngle == 360) {
            coreAngle = 0;
        }

        if (isVirtual())
            return Mth.lerp(partialTicks + .5f, previousCoreAngle, coreAngle);

        return Mth.lerp(partialTicks, coreAngle, coreAngle + 4f);


    }

    public float getCoreOffset(float partialTicks) {
        if (animationType == Animation.IDLE) {
            return 0;
        } else if (animationType == Animation.ASSEMBLY) {
            int runningTicks = (int) Math.abs(this.assemblyProgress.getValue());
            int prevRunningTicks = (int) Math.abs(this.assemblyProgress.getValue() - 1);
            float ticks = Mth.lerp(partialTicks, prevRunningTicks, runningTicks);
            if (runningTicks < (ASSEMBLY_TIME * 3) / 4) {
                return (float) Mth.clamp(Math.pow(ticks / ASSEMBLY_TIME * 3, 4), 0, 1);
            }
            return easeInBounce(Mth.clamp((ASSEMBLY_TIME - ticks) / ASSEMBLY_TIME * 8, 0, 1));
        } else if (animationType == Animation.DISASSEMBLY) {
            return disassemblyProgress.getValue(partialTicks);
        }
        return 0f;
    }

    @Override
    public void write(CompoundTag compound, boolean clientPacket) {
        compound.putString("animationState", animationType.toString());
        compound.putFloat("assemblyProgress", assemblyProgress.getValue());
        compound.putFloat("disassemblyProgress", disassemblyProgress.getValue());
        compound.putFloat("idleProgress", idleProgress.getValue());
        compound.putBoolean("isAssembled", isAssembled);
        compound.putBoolean("assembling", assembling);
        compound.putBoolean("disassembling", disassembling);

        ContainerHelper.saveAllItems(compound, inventory);
        super.write(compound, clientPacket);
    }

    //NBT stuff

    @Override
    protected void read(CompoundTag compound, boolean clientPacket) {
        animationType = compound.getString("animationState").equals("ASSEMBLY") ? Animation.ASSEMBLY : compound.getString("animationState").equals("DISASSEMBLY") ? Animation.DISASSEMBLY : Animation.IDLE;
        assemblyProgress.setValueNoUpdate(compound.getFloat("assemblyProgress"));
        disassemblyProgress.setValueNoUpdate(compound.getFloat("disassemblyProgress"));
        idleProgress.setValueNoUpdate(compound.getFloat("idleProgress"));
        isAssembled = compound.getBoolean("isAssembled");
        assembling = compound.getBoolean("assembling");
        disassembling = compound.getBoolean("disassembling");

        this.inventory = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(compound, inventory);
        super.read(compound, clientPacket);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    //Create Behaviors

    enum Animation {
        ASSEMBLY, DISASSEMBLY, IDLE
    }
}
