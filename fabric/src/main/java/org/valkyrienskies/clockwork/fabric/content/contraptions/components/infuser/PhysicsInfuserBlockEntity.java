package org.valkyrienskies.clockwork.fabric.content.contraptions.components.infuser;

import com.simibubi.create.foundation.tileEntity.SmartTileEntity;
import com.simibubi.create.foundation.tileEntity.TileEntityBehaviour;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class PhysicsInfuserBlockEntity extends SmartTileEntity {

    public boolean isAssembled = false;

    public boolean assembling = false;
    public boolean disassembling = false;

    public Animation animationType;
    public LerpedFloat assemblyProgress = LerpedFloat.linear();
    public LerpedFloat disassemblyProgress = LerpedFloat.linear();
    public LerpedFloat idleProgress = LerpedFloat.linear();
    float coreAngle = 0;
    float previousCoreAngle = 0;

    private boolean sendAnimationUpdate;

    enum Animation {
        ASSEMBLY, DISASSEMBLY, IDLE;
    }

    public PhysicsInfuserBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void tick() {
        if (assembling) {
            assemble();
        } else if (disassembling) {
            disassemble();
        }
        startAnimation(Animation.IDLE);
    }


    //Ship Assembly Handlers
    public void startAssembly() {
        assembling = true;
    }
    public void startDisassembly() {
        disassembling = true;
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

    private void playSound(SoundEvent sound, float volume, float pitch) {

    }
    //Create Behaviors

    @Override
    public void addBehaviours(List<TileEntityBehaviour> behaviours) {
    }
}
