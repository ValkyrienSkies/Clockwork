package org.valkyrienskies.clockwork;

import org.valkyrienskies.clockwork.content.curiosities.tools.auric.designator.SelectedAreaToolkit;

import java.util.HashSet;
import java.util.Optional;

public interface AreaData {

    static Optional<AreaData> of(Object context) {
        if (context instanceof AreaData) {
            return Optional.of(((AreaData) context));
        }
        return Optional.empty();
    }

    void setArea(SelectedAreaToolkit load);

    void removeArea(SelectedAreaToolkit list);

    SelectedAreaToolkit getArea();
}
