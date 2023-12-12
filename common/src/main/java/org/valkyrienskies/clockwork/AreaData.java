package org.valkyrienskies.clockwork;

import net.minecraft.core.BlockPos;
import org.joml.Vector3ic;
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.SelectedAreaToolkit;

import java.util.Optional;

public interface AreaData {

    static Optional<AreaData> of(Object context) {
        if (context instanceof AreaData) {
            return Optional.of(((AreaData) context));
        }
        return Optional.empty();
    }

    void setArea(SelectedAreaToolkit load);

    SelectedAreaToolkit getArea();

    Optional<BlockPos> getFirstPos();

    void setFirstPos(Optional<BlockPos> pos);

    Optional<BlockPos> getSecondPos();

    void setSecondPos(Optional<BlockPos> pos);

    void shouldReset(boolean reset);
}
