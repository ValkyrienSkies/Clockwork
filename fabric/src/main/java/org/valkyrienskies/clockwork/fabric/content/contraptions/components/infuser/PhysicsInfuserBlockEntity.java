package org.valkyrienskies.clockwork.fabric.content.contraptions.components.infuser;

import com.simibubi.create.foundation.tileEntity.SmartTileEntity;
import com.simibubi.create.foundation.tileEntity.TileEntityBehaviour;
import com.simibubi.create.foundation.utility.VecHelper;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.valkyrienskies.clockwork.fabric.AllClockworkSounds;
import org.valkyrienskies.clockwork.fabric.content.curiosities.particles.PhysLightningParticle;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;
import java.util.Random;
import java.util.List;

public class PhysicsInfuserBlockEntity extends SmartTileEntity {

    public boolean isAssembled = false;

    public boolean assembling = false;
    public boolean disassembling = false;

    public Animation animationType;
    public LerpedFloat assemblyProgress = LerpedFloat.linear();
    public float slamProgress;
    public LerpedFloat disassemblyProgress = LerpedFloat.linear();
    public LerpedFloat idleProgress = LerpedFloat.linear();
    float coreAngle = 0;
    float previousCoreAngle = 0;

    private Vec3 thisposition = VectorConversionsMCKt.toMinecraft(VectorConversionsMCKt.toJOMLD(worldPosition));
    boolean initPlayed=false;

    public static final int ASSEMBLY_TIME = 500;
    public static final int DISASSEMBLY_TIME = 1000;

    private boolean sendAnimationUpdate;

    enum Animation {
        ASSEMBLY, DISASSEMBLY, IDLE;
    }

    public PhysicsInfuserBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void tick() {
        super.tick();

        if (animationType == null) {
            animationType = Animation.IDLE;
        }
        if (animationType == Animation.IDLE) {
            startAnimation(Animation.IDLE);
        }

        if(assembling) {
            assemblyProgress.setValue(assemblyProgress.getValue() + 1);
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
            }
        }
    }


    //Ship Assembly Handlers
    public void startAssembly() {
        assembling = true;
        this.animationType = Animation.ASSEMBLY;
        startAnimation(Animation.ASSEMBLY);
    }
    public void startDisassembly() {
        disassembling = true;
        this.animationType = Animation.DISASSEMBLY;
        startAnimation(Animation.DISASSEMBLY);
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
        AllClockworkSounds.PHYSICS_INFUSER_INITIALIZE.playAt(world, location, 1, 1, false);
    }

    public static void playWindupSound(Level world, Vec3 location) {
        AllClockworkSounds.PHYSICS_INFUSER_WINDUP.playAt(world, location, 1, 1, false);
    }

    public static void playZapSound(Level world, Vec3 location, Random rand) {
        float pitch = 0.6F + rand.nextFloat() * 0.4F;
        AllClockworkSounds.PHYSICS_INFUSER_LIGHTNING.playAt(world, location, 1, 1, false);
    }

    public static void playFinishSound(Level world, Vec3 location) {
        AllClockworkSounds.PHYSICS_INFUSER_FINISH.playAt(world, location, 1, 1, false);
    }

    public static void spawnParticlesAssembly(Level world, Vec3 pos, Random rand) {
        double degrees = rand.nextDouble() * 360;

        double angle = Math.toRadians(degrees);

        double radius = 2.0D;

        double x = radius* Math.cos(angle);
        double y = 0.5d;
        double z = radius* Math.sin(angle);
    }
    //Create Behaviors

    @Override
    public void addBehaviours(List<TileEntityBehaviour> behaviours) {
    }
}
