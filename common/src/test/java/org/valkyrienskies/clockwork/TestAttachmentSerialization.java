package org.valkyrienskies.clockwork;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.minecraft.core.Direction;
import org.joml.Vector2d;
import org.joml.Vector3d;
import org.joml.Vector3i;
import org.junit.jupiter.api.Test;
import org.valkyrienskies.clockwork.content.propulsion.afterblazer.AfterblazerCreateData;
import org.valkyrienskies.clockwork.content.propulsion.ballooner.BalloonCreateData;
import org.valkyrienskies.clockwork.content.contraptions.propellor.PropellorCreatePhysData;
import org.valkyrienskies.clockwork.content.forces.AfterblazerController;
import org.valkyrienskies.clockwork.content.forces.BalloonController;
import org.valkyrienskies.clockwork.content.forces.PropellorController;
import org.valkyrienskies.clockwork.util.blocktype.LiquidFuelType;
import org.valkyrienskies.core.impl.util.serialization.VSJacksonUtil;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestAttachmentSerialization {
    @Test
    public void testSerializePropellerController() throws IOException {
        final PropellorController propellorController = new PropellorController();
        propellorController.addPropellor(
                new PropellorCreatePhysData(
                        new Vector3d(1.0, 2.0, 3.0),
                        new Vector3d(4.0, 5.0, 6.0),
                        7.0,
                        8.0,
                        List.of(
                                new Vector3i(9, 10, 11),
                                new Vector3i(12, 13, 14)
                        ),
                        false
                )
        );

        final ObjectMapper mapper = VSJacksonUtil.INSTANCE.getDtoMapper();

        // Convert to bytes
        final byte[] asBytes = mapper.writeValueAsBytes(propellorController);
        final PropellorController fromBytes = mapper.readValue(asBytes, PropellorController.class);

        // Verify that they're equal
        assertEquals(propellorController, fromBytes);
    }

//    @Test
//    public void testSerializeAfterblazerController() throws IOException {
//        final AfterblazerController afterblazerController = new AfterblazerController();
//        afterblazerController.addAfterblazer(
//                new AfterblazerCreateData(
//                        Direction.NORTH,
//                        1.0,
//                        LiquidFuelType.GOURMET,
//                        2,
//                        new Vector3d(3.0, 4.0, 5.0),
//                        new Vector2d(6.0, 7.0)
//                )
//        );
//
//        final ObjectMapper mapper = VSJacksonUtil.INSTANCE.getDtoMapper();
//
//        // Convert to bytes
//        final byte[] asBytes = mapper.writeValueAsBytes(afterblazerController);
//        final AfterblazerController fromBytes = mapper.readValue(asBytes, AfterblazerController.class);
//
//        // Verify that they're equal
//        assertEquals(afterblazerController, fromBytes);
//    }

    @Test
    public void testSerializeBalloonController() throws IOException {
        final BalloonController balloonController = new BalloonController();
        balloonController.addBalloon(
                new BalloonCreateData(
                        new Vector3d(1.0, 2.0, 3.0),
                        new HashSet<>(Set.of(new Vector3d(4.0, 5.0, 6.0))),
                        7.0f,
                        8.0,
                        LiquidFuelType.PLAIN
                )
        );

        final ObjectMapper mapper = VSJacksonUtil.INSTANCE.getDtoMapper();

        // Convert to bytes
        final byte[] asBytes = mapper.writeValueAsBytes(balloonController);
        final BalloonController fromBytes = mapper.readValue(asBytes, BalloonController.class);

        // Verify that they're equal
        assertEquals(balloonController, fromBytes);
    }
}
