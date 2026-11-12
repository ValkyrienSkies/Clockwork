package org.valkyrienskies.clockwork;

import net.minecraft.core.Direction;

/**
 * This rather sus class exists for the purpose of storing two public static variables
 * to be set in MixinLinkedControllerItem and accessed from MixinLinkedControllerClientHandler
 */
public class LinkedControllerClientHandlerMixinStorage {
    public static Direction face;
    /**
     * True if we're linking a flap bearing, false if we're linking a gimbal bearing
     */
    public static boolean doingFlaps = true;

    public static void doNothing() {

    }
}
