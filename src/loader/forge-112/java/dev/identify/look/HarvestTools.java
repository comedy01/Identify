package dev.identify.look;

import net.minecraft.block.state.IBlockState;

import java.util.Arrays;
import java.util.List;

final class HarvestTools {
    private static final List<String> TOOLS = Arrays.asList("pickaxe", "axe", "shovel", "hoe");

    private HarvestTools() {
    }

    static String tool(IBlockState state) {
        String tool = state.getBlock().getHarvestTool(state);
        return TOOLS.contains(tool) ? tool : null;
    }

    static String tier(IBlockState state) {
        switch (state.getBlock().getHarvestLevel(state)) {
            case 3:
                return "diamond";
            case 2:
                return "iron";
            case 1:
                return "stone";
            default:
                return null;
        }
    }
}
