package org.valkyrienskies.clockwork.util.compat;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import static org.valkyrienskies.mod.common.util.VectorConversionsMCKt.toJOML;
import static org.valkyrienskies.mod.common.util.VectorConversionsMCKt.toMinecraft;

public class CompatUtil {
    public static Vector3d toSameSpaceAs(Level level, Vector3d position, Vector3d target) {
        Ship ship;
        if (VSGameUtilsKt.isBlockInShipyard(level, position.x, position.y, position.z) && !VSGameUtilsKt.isBlockInShipyard(level, target.x, target.y, target.z)) {
            ship = VSGameUtilsKt.getShipManagingPos(level, position);
            ship.getShipToWorld().transformPosition(position);
        }
        if (!VSGameUtilsKt.isBlockInShipyard(level, position.x, position.y, position.z) && VSGameUtilsKt.isBlockInShipyard(level, target.x, target.y, target.z)) {
            ship = VSGameUtilsKt.getShipManagingPos(level, target);
            ship.getWorldToShip().transformPosition(position);
        }
        return position;
    }

    public static Vec3 toSameSpaceAs(Level level, Vec3 position, Vec3 target) {
        return toMinecraft(toSameSpaceAs(level, toJOML(position), toJOML(target)));
    }

    public static Vec3 toSameSpaceAs(Level level, double px, double py, double pz, Vec3 target) {
        return toMinecraft(toSameSpaceAs(level, new Vector3d(px,py,pz), toJOML(target)));
    }
}
