package org.valkyrienskies.clockwork;

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

    Optional<Vector3ic> getFirstPos();

    void setFirstPos(Optional<Vector3ic> pos);

    Optional<Vector3ic> getSecondPos();

    void setSecondPos(Optional<Vector3ic> pos);

    void shouldReset(boolean reset);
}
