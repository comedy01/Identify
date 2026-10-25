package dev.identify.look;

import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

import java.util.ArrayList;
import java.util.List;

final class FluidCompat {
    private FluidCompat() {
    }

    static List<FluidTank> read(BlockEntity entity) {
        Level level = entity.getLevel();
        if (level == null) {
            return null;
        }
        try {
            ResourceHandler<FluidResource> handler = level.getCapability(Capabilities.Fluid.BLOCK, entity.getBlockPos(), null);
            for (int i = 0; handler == null && i < Direction.values().length; i++) {
                handler = level.getCapability(Capabilities.Fluid.BLOCK, entity.getBlockPos(), Direction.values()[i]);
            }
            if (handler == null) {
                return null;
            }
            List<FluidTank> tanks = new ArrayList<>(2);
            for (int i = 0; i < handler.size(); i++) {
                FluidResource resource = handler.getResource(i);
                long capacity = handler.getCapacityAsLong(i, resource);
                if (capacity <= 0) {
                    continue;
                }
                tanks.add(new FluidTank(resource.isEmpty() ? null : resource.getHoverName().copy(),
                        handler.getAmountAsLong(i), capacity));
            }
            return tanks.isEmpty() ? null : tanks;
        } catch (RuntimeException e) {
            return null;
        }
    }
}
