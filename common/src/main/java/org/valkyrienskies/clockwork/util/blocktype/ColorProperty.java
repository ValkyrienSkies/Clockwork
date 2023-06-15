package org.valkyrienskies.clockwork.util.blocktype;

import com.google.common.collect.ImmutableSet;
import kotlin.text.Regex;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

public class ColorProperty extends Property<String> {
    private final ImmutableSet<String> values;

    protected ColorProperty(String name) {
        super(name, String.class);

        HashSet<String> set = new HashSet<>();

//        int count = -1;
//        while (count < 0xFFFFFF) {
//            set.add(convertToHexidecimalString(count));
//            count++;
//        }

        Pattern pattern = Pattern.compile("0[xX][0-9a-fA-F][0-9a-fA-F][0-9a-fA-F][0-9a-fA-F][0-9a-fA-F][0-9a-fA-F]");


        this.values = ImmutableSet.copyOf(set);
    }

    @Override
    public Collection<String> getPossibleValues() {
        return this.values;
    }

    @Override
    public String getName(String value) {
        return value;
    }

    @Override
    public Optional<String> getValue(String value) {
        return Optional.empty();
    }
}
