package dev.identify.look;

import dev.identify.client.IdentifyClient;
import dev.identify.info.TickTime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.text.TextFormatting;

import java.util.List;
import java.util.Locale;

public final class ItemDetails {
    private static final int FUEL_TICKS_PER_SMELT = 200;

    private ItemDetails() {
    }

    public static void append(ItemStack stack, List<String> lines, boolean advanced) {
        if (stack.isEmpty()) {
            return;
        }

        if (stack.isItemStackDamageable() && !(advanced && stack.isItemDamaged())) {
            int max = stack.getMaxDamage();
            int left = max - stack.getItemDamage();
            int percent = TickTime.percent(left, max);
            lines.add(durabilityColor(percent) + I18n.format("identify.item.durability", left, max, percent));
        }

        if (stack.getItem() instanceof ItemFood) {
            ItemFood food = (ItemFood) stack.getItem();
            int nutrition = food.getHealAmount(stack);
            float saturation = nutrition * food.getSaturationModifier(stack) * 2.0F;
            lines.add(TextFormatting.GOLD + I18n.format(
                    "identify.item.food", nutrition, String.format(Locale.ROOT, "%.1f", saturation)));
        }

        int burn = TileEntityFurnace.getItemBurnTime(stack);
        if (burn > 0) {
            double smelts = (double) burn / FUEL_TICKS_PER_SMELT;
            String shown = Math.abs(smelts - Math.round(smelts)) < 0.005D
                    ? Long.toString(Math.round(smelts))
                    : String.format(Locale.ROOT, "%.1f", smelts);
            lines.add(TextFormatting.RED + I18n.format("identify.item.fuel", shown));
        }

        if (IdentifyClient.config().compareItems()) {
            ItemCompare.append(Minecraft.getMinecraft(), stack, lines);
        }
    }

    private static TextFormatting durabilityColor(int percent) {
        if (percent > 50) {
            return TextFormatting.GREEN;
        }
        if (percent > 20) {
            return TextFormatting.YELLOW;
        }
        return TextFormatting.RED;
    }
}
