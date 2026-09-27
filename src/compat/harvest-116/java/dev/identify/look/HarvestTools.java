package dev.identify.look;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ToolType;

final class HarvestTools {
    private HarvestTools() {
    }

    static String tool(BlockState state) {
        ToolType tool = state.getHarvestTool();
        if (tool == ToolType.PICKAXE) {
            return "pickaxe";
        } else if (tool == ToolType.AXE) {
            return "axe";
        } else if (tool == ToolType.SHOVEL) {
            return "shovel";
        } else if (tool == ToolType.HOE) {
            return "hoe";
        }
        return null;
    }

    static String tier(BlockState state) {
        switch (state.getHarvestLevel()) {
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
