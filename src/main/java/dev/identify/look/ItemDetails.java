package dev.identify.look;

import dev.identify.client.IdentifyClient;
import dev.identify.info.TickTime;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Locale;

public final class ItemDetails {
    private static final int FUEL_TICKS_PER_SMELT = 200;

    private ItemDetails() {
    }

    public static void append(ItemStack stack, List<Component> lines) {
        if (stack.isEmpty()) {
            return;
        }

        if (stack.isDamageableItem()) {
            int max = stack.getMaxDamage();
            int left = max - stack.getDamageValue();
            int percent = TickTime.percent(left, max);
            lines.add(Component.translatable("identify.item.durability", left, max, percent)
                    .withStyle(durabilityColor(percent)));
        }

        FoodProperties food = stack.get(DataComponents.FOOD);
        if (food != null) {
            lines.add(Component.translatable(
                    "identify.item.food",
                    food.nutrition(),
                    String.format(Locale.ROOT, "%.1f", food.saturation()))
                    .withStyle(ChatFormatting.GOLD));
        }

        int burn = FuelCompat.burnTicks(stack);
        if (burn > 0) {
            double smelts = (double) burn / FUEL_TICKS_PER_SMELT;
            String shown = Math.abs(smelts - Math.round(smelts)) < 0.005D
                    ? Long.toString(Math.round(smelts))
                    : String.format(Locale.ROOT, "%.1f", smelts);
            lines.add(Component.translatable("identify.item.fuel", shown).withStyle(ChatFormatting.RED));
        }

        if (IdentifyClient.config().compareItems()) {
            ItemCompare.append(Minecraft.getInstance(), stack, lines);
        }
    }

    private static ChatFormatting durabilityColor(int percent) {
        if (percent > 50) {
            return ChatFormatting.GREEN;
        }
        if (percent > 20) {
            return ChatFormatting.YELLOW;
        }
        return ChatFormatting.RED;
    }
}
