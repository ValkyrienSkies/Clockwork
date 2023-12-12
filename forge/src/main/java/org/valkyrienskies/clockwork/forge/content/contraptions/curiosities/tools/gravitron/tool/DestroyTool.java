package org.valkyrienskies.clockwork.forge.content.contraptions.curiosities.tools.gravitron.tool;

import org.valkyrienskies.clockwork.ClockworkPackets;
import org.valkyrienskies.clockwork.forge.content.contraptions.curiosities.tools.gravitron.GravitronDestroyPacket;

public class DestroyTool extends GravitronToolBase{

    @Override
    public boolean handleRightClick() {
        updateTargetPos();

        ClockworkPackets.sendToServer(new GravitronDestroyPacket(clickedPos));

        return true;
    }

    @Override
    public boolean handleMouseWheel(double delta) {
        return false;
    }
}
