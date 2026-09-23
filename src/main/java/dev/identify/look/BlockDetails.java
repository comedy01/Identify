package dev.identify.look;

import dev.identify.info.TickTime;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BlockDetails {
    private BlockDetails() {
    }

    public static List<Component> of(BlockState state) {
        List<Component> lines = new ArrayList<>(3);

        IntegerProperty age = ageProperty(state);
        if (age != null) {
            int value = state.getValue(age);
            int max = Collections.max(age.getPossibleValues());
            if (value >= max) {
                lines.add(Component.translatable("identify.crop.mature"));
            } else {
                lines.add(Component.translatable("identify.crop.growth", value, max, TickTime.percent(value, max)));
            }
        }

        if (state.is(Blocks.REDSTONE_WIRE)) {
            lines.add(Component.translatable("identify.redstone.power", state.getValue(BlockStateProperties.POWER)));
        }

        Component tool = toolLine(state);
        if (tool != null) {
            lines.add(tool);
        }
        return lines;
    }

    private static IntegerProperty ageProperty(BlockState state) {
        if (!(state.getBlock() instanceof CropBlock
                || state.getBlock() instanceof NetherWartBlock
                || state.getBlock() instanceof SweetBerryBushBlock
                || state.getBlock() instanceof CocoaBlock)) {
            return null;
        }
        for (Property<?> property : state.getProperties()) {
            if (property instanceof IntegerProperty integer && property.getName().equals("age")) {
                return integer;
            }
        }
        return null;
    }

    private static Component toolLine(BlockState state) {
        String tool = null;
        if (state.is(BlockTags.MINEABLE_WITH_PICKAXE)) {
            tool = "pickaxe";
        } else if (state.is(BlockTags.MINEABLE_WITH_AXE)) {
            tool = "axe";
        } else if (state.is(BlockTags.MINEABLE_WITH_SHOVEL)) {
            tool = "shovel";
        } else if (state.is(BlockTags.MINEABLE_WITH_HOE)) {
            tool = "hoe";
        }
        if (tool == null) {
            return null;
        }
        Component toolName = Component.translatable("identify.tool." + tool);
        String tier = null;
        if (state.is(BlockTags.NEEDS_DIAMOND_TOOL)) {
            tier = "diamond";
        } else if (state.is(BlockTags.NEEDS_IRON_TOOL)) {
            tier = "iron";
        } else if (state.is(BlockTags.NEEDS_STONE_TOOL)) {
            tier = "stone";
        }
        if (tier == null) {
            return Component.translatable("identify.tool.best", toolName);
        }
        return Component.translatable("identify.tool.best_tier", toolName, Component.translatable("identify.tier." + tier));
    }
}
