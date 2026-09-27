package dev.identify.look;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;

final class FoodCompat {
    private FoodCompat() {
    }

    static FoodValues of(ItemStack stack) {
        FoodProperties food = stack.getItem().getFoodProperties();
        if (food == null) {
            return null;
        }
        return new FoodValues(food.getNutrition(), food.getNutrition() * food.getSaturationModifier() * 2.0F);
    }
}
