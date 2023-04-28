package org.valkyrienskies.clockwork.integration.cc;

import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.valkyrienskies.clockwork.content.contraptions.sequenced_seat.InputKey;
import org.valkyrienskies.clockwork.content.contraptions.sequenced_seat.SequencedSeatBlockEntity;
import org.valkyrienskies.clockwork.content.contraptions.sequenced_seat.SequencedSeatRule;
import org.valkyrienskies.clockwork.content.contraptions.sequenced_seat.SequencedSeatRuleList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class CommandSeatPeripheral implements IPeripheral {
    private final SequencedSeatBlockEntity seat;

    public CommandSeatPeripheral(SequencedSeatBlockEntity seat) {
        this.seat = seat;
    }

    @NotNull
    @Override
    public String getType() {
        return "command_seat";
    }

    @Override
    public boolean equals(@Nullable IPeripheral iPeripheral) {
        return iPeripheral instanceof CommandSeatPeripheral;
    }

    @Override
    public void attach(@NotNull IComputerAccess computer) {
        this.seat.computerHandler.attachComputer(computer);
    }

    @Override
    public void detach(@NotNull IComputerAccess computer) {
        this.seat.computerHandler.detachComputer(computer);
    }

    @LuaFunction
    public final HashMap<Integer, List<Object>> getForwardRules() {
        return translateRules(this.seat.getForwardRules());
    }

    @LuaFunction
    public final HashMap<Integer, List<Object>> getBackwardRules() {
        return translateRules(this.seat.getBackwardRules());
    }

    @LuaFunction
    public final HashMap<Integer, List<Object>> getLeftRules() {
        return translateRules(this.seat.getLeftRules());
    }

    @LuaFunction
    public final HashMap<Integer, List<Object>> getRightRules() {
        return translateRules(this.seat.getRightRules());
    }

    private HashMap<Integer, List<Object>> translateRules(SequencedSeatRuleList ruleList) {
        HashMap<Integer, List<Object>> rules = new HashMap<>(SequencedSeatRuleList.MAX_RULES);

        for (int i = 0; i < SequencedSeatRuleList.MAX_RULES; i++) {
            SequencedSeatRule rule = ruleList.getRule(i);

            rules.put(i, List.of(
                    translateInputKeys(rule.getInputKeys()),
                    rule.getValue() == null ? "nil" : rule.getValue().asComponent().getContents(),
                    rule.getOperation() == null ? "nil" : rule.getOperation().name()
            ));
        }

        return rules;
    }

    private List<String> translateInputKeys(Set<InputKey> input) {
        List<String> keys = new ArrayList<>();

        input.forEach(key -> keys.add(key.name()));

        return keys;
    }
}
