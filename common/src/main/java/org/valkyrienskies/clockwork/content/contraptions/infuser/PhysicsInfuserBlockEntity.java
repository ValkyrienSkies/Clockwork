package org.valkyrienskies.clockwork.content.contraptions.infuser;

import com.simibubi.create.foundation.tileEntity.SmartTileEntity;
import com.simibubi.create.foundation.tileEntity.TileEntityBehaviour;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.primitives.AABBic;
import org.valkyrienskies.clockwork.ClockWorkSounds;
import org.valkyrienskies.clockwork.client.render.assemblyscan.ScannerRenderer;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import java.util.List;
import java.util.Random;

import static org.valkyrienskies.clockwork.content.contraptions.infuser.PhysicsInfuserRenderer.ScanManager.SCAN_GROWTH_DURATION;

public class PhysicsInfuserBlockEntity extends SmartTileEntity {

    public boolean isAssembled = false;

    public boolean assembling = false;
    public boolean disassembling = false;

    public boolean skippedAssembly = false;
    boolean skippingAssembly = false;

    public Animation animationType = Animation.IDLE;
    public LerpedFloat assemblyProgress = LerpedFloat.linear();
    public LerpedFloat disassemblyProgress = LerpedFloat.linear();
    public LerpedFloat idleProgress = LerpedFloat.linear();
    float coreAngle = 0;
    float previousCoreAngle = 0;

    int useCooldown = 0;
    boolean onCooldown = false;

    private Vec3 thisposition = VectorConversionsMCKt.toMinecraft(VectorConversionsMCKt.toJOMLD(worldPosition));
    boolean initPlayed=false;

    public static final int ASSEMBLY_TIME = 500;
    public static final int DISASSEMBLY_TIME = 1000;

    private boolean sendAnimationUpdate;

    public ServerShip ship;

    public void initialize(Vec3 center, float scanRadius, int scanComputeDuration) {
    }

    enum Animation {
        ASSEMBLY, DISASSEMBLY, IDLE;
    }

    public PhysicsInfuserBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void tick() {
        super.tick();

        if (level instanceof ServerLevel s) {
            ship = VSGameUtilsKt.getShipManagingPos(s, worldPosition);
        }

        if (ship != null) {
            isAssembled = true;
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

        if(assembling) {
            assemblyProgress.setValue(assemblyProgress.getValue() + 1);
            if (skippingAssembly) {
                assemblyProgress.setValueNoUpdate(400);
                skippingAssembly = false;
            }
        }
        if (disassembling) {
            disassemblyProgress.setValue(disassemblyProgress.getValue() + 1);
        }
        Random rand = level.getRandom();
        //client sounds

        if (assembling) {
            if (!initPlayed) {
                playInitializeSound(level, thisposition);
                initPlayed=true;
            }
            if (assemblyProgress.getValue() == 100) {
                playWindupSound(level, thisposition);
            }
            if (assemblyProgress.getValue() == 160 || assemblyProgress.getValue() == 220 || assemblyProgress.getValue() == 240 || assemblyProgress.getValue() == 300 || assemblyProgress.getValue() == 320 || assemblyProgress.getValue() == 360 || assemblyProgress.getValue() == 400 || assemblyProgress.getValue() == 410 || assemblyProgress.getValue() == 420) {
                playZapSound(level, thisposition, rand);
            }
            if (assemblyProgress.getValue() == 460) {
                playFinishSound(level, thisposition);
                if (level.isClientSide) {
                    Vec3 funnypos;
                    if (ship != null) {
                        funnypos = VectorConversionsMCKt.toMinecraft(ship.getTransform().getShipToWorld().transformPosition(VectorConversionsMCKt.toJOML(thisposition)));
                    } else {
                        funnypos = thisposition;
                    }
                    ScannerRenderer.INSTANCE.ping(funnypos, this);
                }
            }
            if (assemblyProgress.getValue() == 500) {
                isAssembled = true;
                assembling = false;
                initPlayed=false;
                animationType = Animation.IDLE;
                assemblyProgress.setValue(0);
                skippedAssembly = false;
                useCooldown = 400;
            }
        }
    }


    //Ship Assembly Handlers
    public void startAssembly() {
        assembling = true;
        this.animationType = Animation.ASSEMBLY;
        startAnimation(Animation.ASSEMBLY);
    }

    public void skipAssembly() {
        skippedAssembly = true;
        skippingAssembly = true;
    }
    public void startDisassembly() {
        disassembling = true;
        this.animationType = Animation.DISASSEMBLY;
        startAnimation(Animation.DISASSEMBLY);
    }

    public float getPulseRange() {
        if (this.ship != null) {
            AABBic shipAABB = ship.getShipAABB();
            double distanceToMax = thisposition.distanceToSqr(shipAABB.maxX(), shipAABB.maxY(), shipAABB.maxZ());
            double distanceToMin = thisposition.distanceToSqr(shipAABB.minX(), shipAABB.minY(), shipAABB.minZ());

            if (distanceToMax > distanceToMin) {
                return (float) Math.sqrt(distanceToMax);
            } else {
                return (float) Math.sqrt(distanceToMin);
            }
        } else {
            return Minecraft.getInstance().gameRenderer.getRenderDistance();
        }
    }

    public int getScanGrowthDuration() {
        if (this.ship != null) {
            float range = getPulseRange();
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

        final float r1 = getPulseRange();
        final float t1 = duration;
        final float b = 200;
        final float n = 1f / ((t1 + b) * (t1 + b) - b * b);
        final float a = -r1 * b * b * n;
        final float c = r1 * n;

        final float t = (float) (System.currentTimeMillis() - start);

        return 10 + a + (t + b) * (t + b) * c;
    }

    public void assemble() {
        //INSERT ASSEMBLY LOGIC TROL
    }

    public void disassemble() {

    }

    //Animation Jargon

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

    public float getCoreOffset (float partialTicks) {
        if (animationType == Animation.IDLE) {
            return 0;
        }
        else if (animationType == Animation.ASSEMBLY) {
            int runningTicks = (int) Math.abs(this.assemblyProgress.getValue());
            int prevRunningTicks = (int) Math.abs(this.assemblyProgress.getValue() - 1);
            float ticks = Mth.lerp(partialTicks, prevRunningTicks, runningTicks);
            if (runningTicks < (ASSEMBLY_TIME*3)/4) {
                return (float) Mth.clamp(Math.pow(ticks/ASSEMBLY_TIME*3,4),0,1);
            }
            return easeInBounce(Mth.clamp((ASSEMBLY_TIME - ticks) / ASSEMBLY_TIME * 8,0,1));
        }
        else if (animationType == Animation.DISASSEMBLY) {
            return disassemblyProgress.getValue(partialTicks);
        }
        return 0f;
    }

    public float easeInBounce(float x) {
        return 1 - easeOutBounce(1 - x);

    }
    public float easeOutBounce(float x) {
        double n1 = 7.5625;
        double d1 = 2.75;

        if (x < 1 / d1) {
            return (float) (n1 * x * x);
        } else if (x < 2 / d1) {
            return (float) (n1 * (x -= 1.5 / d1) * x + 0.75);
        } else if (x < 2.5 / d1) {
            return (float) (n1 * (x -= 2.25 / d1) * x + 0.9375);
        } else {
            return (float) (n1 * (x -= 2.625 / d1) * x + 0.984375);
        }

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

        double x = radius* Math.cos(angle);
        double y = 0.5d;
        double z = radius* Math.sin(angle);
    }

    //NBT stuff

    @Override
    public void write(CompoundTag compound, boolean clientPacket) {
        compound.putString("animationState", animationType.toString());
        compound.putFloat("assemblyProgress", assemblyProgress.getValue());
        compound.putFloat("disassemblyProgress", disassemblyProgress.getValue());
        compound.putFloat("idleProgress", idleProgress.getValue());
        compound.putBoolean("isAssembled", isAssembled);
        compound.putBoolean("assembling", assembling);
        compound.putBoolean("disassembling", disassembling);
        compound.putBoolean("skippedAssembly", skippedAssembly);
        super.write(compound, clientPacket);
    }

    @Override
    protected void read(CompoundTag compound, boolean clientPacket) {
        animationType = compound.getString("animationState").equals("ASSEMBLY") ? Animation.ASSEMBLY : compound.getString("animationState").equals("DISASSEMBLY") ? Animation.DISASSEMBLY : Animation.IDLE;
        assemblyProgress.setValueNoUpdate(compound.getFloat("assemblyProgress"));
        disassemblyProgress.setValueNoUpdate(compound.getFloat("disassemblyProgress"));
        idleProgress.setValueNoUpdate(compound.getFloat("idleProgress"));
        isAssembled = compound.getBoolean("isAssembled");
        assembling = compound.getBoolean("assembling");
        disassembling = compound.getBoolean("disassembling");
        skippedAssembly = compound.getBoolean("skippedAssembly");
        super.read(compound, clientPacket);
    }

    //Create Behaviors

    @Override
    public void addBehaviours(List<TileEntityBehaviour> behaviours) {
    }
}
