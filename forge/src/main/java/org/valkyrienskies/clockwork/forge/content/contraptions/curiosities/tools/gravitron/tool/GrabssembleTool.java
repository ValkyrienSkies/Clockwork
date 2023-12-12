package org.valkyrienskies.clockwork.forge.content.contraptions.curiosities.tools.gravitron.tool;


import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.valkyrienskies.clockwork.ClockworkPackets;
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronItem;
import org.valkyrienskies.clockwork.forge.content.contraptions.curiosities.tools.gravitron.GravitronGrabPacket;
import org.valkyrienskies.clockwork.mixinduck.MixinPlayerDuck;

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
}