package org.valkyrienskies.clockwork.forge.content.curiosities.tools.gravitron.tool;


import org.valkyrienskies.clockwork.ClockworkPackets;
import org.valkyrienskies.clockwork.forge.content.curiosities.tools.gravitron.GravitronGrabPacket;

public class GrabssembleTool extends GravitronToolBase {

    @Override
    public boolean handleRightClick() {
        updateTargetPos();
        ClockworkPackets.sendToServer(new GravitronGrabPacket(clickedPos, clickedLocation, GRABSSEMBLE));
        return true;
    }

    @Override
    public boolean handleMouseWheel(double delta) {
        return false;
    }
}