package dev.identify.look;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

final class FuelCompat {
    private FuelCompat() {
    }

    static int burnTicks(ItemStack stack) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return 0;
        }
        return mc.level.fuelValues().burnDuration(stack);
    }
}
