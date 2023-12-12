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
/*
    public static void tryAssembleAndGrabShip(Level level, Player player, BlockPos clickedPos, Vec3 clickLocation) {
        var bl = GravitronItem.Companion.grabssemble(level, player, clickedPos, clickLocation, true);
        if (level instanceof ServerLevel serverLevel) {
            if (!bl) {
                var bl2 = GrabTool.tryGrabShip(serverLevel, player, clickedPos, clickLocation);
                if (!bl2) {
                    getState(player).setGrabbing(false);
                    getState(player).setShipID(null);
                }
            } else {
                MixinPlayerDuck p = (MixinPlayerDuck) player;
                GravitronItem.Companion.GravitronState s = p.cw_getGravitronState();
                p.cw_setGravitronState(s);
            }
        }
    }

 */
}