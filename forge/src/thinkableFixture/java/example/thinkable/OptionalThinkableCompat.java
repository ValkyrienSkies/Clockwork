package example.thinkable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.valkyrienskies.clockwork.api.thinkable.Thinkable;
import org.valkyrienskies.clockwork.api.thinkable.ThinkableEntrypoint;
import org.valkyrienskies.clockwork.api.thinkable.ThinkableRegistry;
import org.valkyrienskies.clockwork.api.thinkable.ThinkableReading;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public final class OptionalThinkableCompat implements ThinkableEntrypoint {
    private final Map<BlockPos, Double> inputs = new HashMap<>();

    @Override
    public void registerThinkables(ThinkableRegistry registry) {
        registry.registerBlock(Blocks.BONE_BLOCK, context -> output(
                context.state().getValue(BlockStateProperties.AXIS) == Direction.Axis.X ? Double.NaN : -12.375));
        registry.registerBlock(Blocks.HAY_BLOCK, context -> new Thinkable() {
            @Override public boolean acceptsThinkable(Direction side) { return side == Direction.DOWN; }
            @Override public boolean providesThinkable(Direction side) { return side == Direction.UP; }
            @Override public Double thinkableValue(Direction side) { return inputs.get(context.pos()); }
            @Override public void receiveThinkable(Direction side, Double value) {
                if (value == null) inputs.remove(context.pos());
                else inputs.put(context.pos(), value);
            }
        });
        registry.registerBlock(Blocks.DAYLIGHT_DETECTOR, context -> output(99.75));
        registry.registerBlockEntity(BlockEntityType.DAYLIGHT_DETECTOR, (context, entity) -> {
            var power = context.state().getValue(BlockStateProperties.POWER);
            return output(power == 15 ? Double.NaN : power + 0.125);
        });
    }

    private static Thinkable output(double value) {
        return new Thinkable() {
            @Override public boolean providesThinkable(Direction side) { return true; }
            @Override public Double thinkableValue(Direction side) { return value; }
            @Override public List<ThinkableReading> thinkableReadings(Direction side) {
                return List.of(new ThinkableReading("doubled", Component.literal("Doubled value")));
            }
            @Override public Double thinkableValue(Direction side, String reading) {
                if (reading.isEmpty()) return value;
                if (reading.equals("doubled")) return value * 2;
                return null;
            }
            @Override public Component thinkablePortName(Direction side) { return Component.literal("Fixture output"); }
        };
    }
}
