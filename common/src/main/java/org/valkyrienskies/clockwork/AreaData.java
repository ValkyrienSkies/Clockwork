package org.valkyrienskies.clockwork;

import org.joml.Vector3ic;
import org.joml.primitives.AABBic;
import org.valkyrienskies.clockwork.content.curiosities.tools.auric.designator.SelectedAreaToolkit;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public interface AreaData {

    static Optional<AreaData> of(Object context) {
        if (context instanceof AreaData) {
            return Optional.of(((AreaData) context));
        }
        return Optional.empty();
    }

    void setArea(SelectedAreaToolkit load);

    SelectedAreaToolkit getArea();

    Optional<Vector3ic> getFirstPos();

    void setFirstPos(Optional<Vector3ic> pos);

    Optional<Vector3ic> getSecondPos();

    void setSecondPos(Optional<Vector3ic> pos);

    void shouldReset(boolean reset);
}
