package org.valkyrienskies.clockwork;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.joml.Vector3d;
import org.joml.Vector3i;
import org.junit.jupiter.api.Test;
import org.valkyrienskies.clockwork.content.contraptions.propellor.PropellorCreatePhysData;
import org.valkyrienskies.clockwork.content.forces.PropellorController;
import org.valkyrienskies.core.impl.util.serialization.VSJacksonUtil;

import java.io.IOException;
import java.util.List;

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
}
