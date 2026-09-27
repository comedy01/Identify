package dev.identify.look;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;

final class FoodCompat {
    private FoodCompat() {
    }

    static FoodValues of(ItemStack stack) {
        FoodProperties food = stack.get(DataComponents.FOOD);
        return food == null ? null : new FoodValues(food.nutrition(), food.saturation());
    }
}
