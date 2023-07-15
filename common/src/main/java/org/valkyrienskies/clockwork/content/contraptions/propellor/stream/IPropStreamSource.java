package org.valkyrienskies.clockwork.content.contraptions.propellor.stream;

import com.simibubi.create.infrastructure.config.AllConfigs;
import com.simibubi.create.infrastructure.config.CKinetics;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import org.joml.Vector3d;

import javax.annotation.Nullable;

@MethodsReturnNonnullByDefault
public interface IPropStreamSource {
    @Nullable
    PropStream getStream();

    @Nullable
    Level getStreamWorld();

    BlockPos getStreamPos();

    float getSpeed();
    float getRotspeed();
    void onRotspeedChanged();

    Direction getStreamOriginSide();

    @Nullable
    Direction getStreamDirection();

    Vector3d getStreamScale();

    int getSailCount();

    default float getMaxDistance() {
        float speed = Math.abs(this.getSpeed());
        CKinetics config = AllConfigs.server().kinetics;
        float distanceFactor = Math.min(speed / config.fanRotationArgmax.get(), 1);
        float pushDistance = Mth.lerp(distanceFactor, 3, config.fanPushDistance.get());
        float pullDistance = Mth.lerp(distanceFactor, 3f, config.fanPullDistance.get());
        return this.getSpeed() > 0 ? pushDistance : pullDistance;
    }

    boolean isSourceRemoved();
}
