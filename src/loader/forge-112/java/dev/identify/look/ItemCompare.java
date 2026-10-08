package dev.identify.look;

import dev.identify.info.Numbers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;

import java.util.Arrays;
import java.util.List;

public final class ItemCompare {
    private static final List<String> STATS = Arrays.asList("armor", "toughness", "damage", "speed");
    private static final int ADDITION = 0;

    private ItemCompare() {
    }

    public static void append(Minecraft mc, ItemStack stack, List<String> lines) {
        EntityPlayerSP player = mc.player;
        if (player == null || stack.isEmpty()) {
            return;
        }

        EntityEquipmentSlot slot = slotFor(stack);
        String parts = difference(stack, player.getItemStackFromSlot(slot), slot);
        if (parts == null) {
            return;
        }

        if (GuiScreen.isShiftKeyDown()) {
            lines.add(TextFormatting.GRAY + I18n.format("identify.compare.line", parts));
        } else {
            lines.add(TextFormatting.DARK_GRAY + I18n.format("identify.compare.hint"));
        }
    }

    public static String difference(ItemStack stack, ItemStack equipped, EntityEquipmentSlot slot) {
        if (ItemStack.areItemStacksEqual(stack, equipped)) {
            return null;
        }

        double[] now = new double[STATS.size()];
        boolean hasStats = false;
        for (int i = 0; i < STATS.size(); i++) {
            now[i] = total(stack, slot, STATS.get(i));
            hasStats |= now[i] != 0.0D;
        }
        if (!hasStats) {
            return null;
        }

        StringBuilder parts = new StringBuilder();
        for (int i = 0; i < STATS.size(); i++) {
            String stat = STATS.get(i);
            double delta = now[i] - total(equipped, slot, stat);
            if (!Numbers.significant(delta)) {
                continue;
            }
            if (parts.length() > 0) {
                parts.append(TextFormatting.GRAY).append(", ");
            }
            parts.append(delta > 0.0D ? TextFormatting.GREEN : TextFormatting.RED)
                    .append(Numbers.signed(delta))
                    .append(' ')
                    .append(I18n.format("identify.compare." + stat));
        }
        return parts.length() > 0 ? parts.toString() : null;
    }

    public static EntityEquipmentSlot slotFor(ItemStack stack) {
        EntityEquipmentSlot slot = EntityLiving.getSlotForItemStack(stack);
        return slot.getSlotType() == EntityEquipmentSlot.Type.ARMOR ? slot : EntityEquipmentSlot.MAINHAND;
    }

    private static double total(ItemStack stack, EntityEquipmentSlot slot, String stat) {
        if (stack.isEmpty()) {
            return 0.0D;
        }
        double sum = 0.0D;
        for (AttributeModifier modifier : stack.getAttributeModifiers(slot).get(attribute(stat).getName())) {
            if (modifier.getOperation() == ADDITION) {
                sum += modifier.getAmount();
            }
        }
        return sum;
    }

    private static IAttribute attribute(String stat) {
        switch (stat) {
            case "armor":
                return SharedMonsterAttributes.ARMOR;
            case "toughness":
                return SharedMonsterAttributes.ARMOR_TOUGHNESS;
            case "damage":
                return SharedMonsterAttributes.ATTACK_DAMAGE;
            default:
                return SharedMonsterAttributes.ATTACK_SPEED;
        }
    }
}
