package org.valkyrienskies.clockwork.content.contraptions.propellor.stream;

import javax.annotation.Nullable;

import com.simibubi.create.foundation.config.AllConfigs;
import com.simibubi.create.foundation.config.CKinetics;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import org.joml.Vector3d;

@MethodsReturnNonnullByDefault
public interface IPropStreamSource {
    @Nullable
    PropStream getStream();

    @Nullable
    Level getStreamWorld();

    BlockPos getStreamPos();

    float getSpeed();

    Direction getStreamOriginSide();

    @Nullable
    Direction getStreamDirection();

    Vector3d getStreamScale();

    default float getMaxDistance() {
        float speed = Math.abs(this.getSpeed());
        CKinetics config = AllConfigs.SERVER.kinetics;
        float distanceFactor = Math.min(speed / config.fanRotationArgmax.get(), 1);
        float pushDistance = Mth.lerp(distanceFactor, 3, config.fanPushDistance.get());
        float pullDistance = Mth.lerp(distanceFactor, 3f, config.fanPullDistance.get());
        return this.getSpeed() > 0 ? pushDistance : pullDistance;
    }

    boolean isSourceRemoved();
}
