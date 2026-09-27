package dev.identify.look;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;

final class HarvestTools {
    private HarvestTools() {
    }

    static String tool(BlockState state) {
        if (state.is(BlockTags.MINEABLE_WITH_PICKAXE)) {
            return "pickaxe";
        } else if (state.is(BlockTags.MINEABLE_WITH_AXE)) {
            return "axe";
        } else if (state.is(BlockTags.MINEABLE_WITH_SHOVEL)) {
            return "shovel";
        } else if (state.is(BlockTags.MINEABLE_WITH_HOE)) {
            return "hoe";
        }
        return null;
    }

    static String tier(BlockState state) {
        if (state.is(BlockTags.NEEDS_DIAMOND_TOOL)) {
            return "diamond";
        } else if (state.is(BlockTags.NEEDS_IRON_TOOL)) {
            return "iron";
        } else if (state.is(BlockTags.NEEDS_STONE_TOOL)) {
            return "stone";
        }
        return null;
    }
}
