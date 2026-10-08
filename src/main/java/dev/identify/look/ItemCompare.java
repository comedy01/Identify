package dev.identify.look;

import dev.identify.Texts;
import dev.identify.info.Numbers;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class ItemCompare {
    private static final List<String> STATS = List.of("armor", "toughness", "damage", "speed");

    private ItemCompare() {
    }

    public static void append(Minecraft mc, ItemStack stack, List<Component> lines) {
        LocalPlayer player = mc.player;
        if (player == null || stack.isEmpty()) {
            return;
        }

        EquipmentSlot slot = slotFor(stack);
        Component parts = difference(stack, player.getItemBySlot(slot), slot);
        if (parts == null) {
            return;
        }

        if (ShiftKey.isDown(mc)) {
            lines.add(Texts.translatable("identify.compare.line", parts).withStyle(ChatFormatting.GRAY));
        } else {
            lines.add(Texts.translatable("identify.compare.hint").withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    public static Component difference(ItemStack stack, ItemStack equipped, EquipmentSlot slot) {
        if (ItemStack.matches(stack, equipped)) {
            return null;
        }

        double[] now = new double[STATS.size()];
        boolean hasStats = false;
        for (int i = 0; i < STATS.size(); i++) {
            now[i] = ModifierCompat.total(stack, slot, STATS.get(i));
            hasStats |= now[i] != 0.0D;
        }
        if (!hasStats) {
            return null;
        }

        MutableComponent parts = Texts.empty();
        boolean any = false;
        for (int i = 0; i < STATS.size(); i++) {
            String stat = STATS.get(i);
            double delta = now[i] - ModifierCompat.total(equipped, slot, stat);
            if (!Numbers.significant(delta)) {
                continue;
            }
            if (any) {
                parts.append(Texts.literal(", ").withStyle(ChatFormatting.GRAY));
            }
            parts.append(Texts.literal(Numbers.signed(delta) + " ")
                    .append(Texts.translatable("identify.compare." + stat))
                    .withStyle(delta > 0.0D ? ChatFormatting.GREEN : ChatFormatting.RED));
            any = true;
        }
        return any ? parts : null;
    }

    public static EquipmentSlot slotFor(ItemStack stack) {
        EquipmentSlot armor = EquipCompat.armorSlot(stack);
        return armor == null ? EquipmentSlot.MAINHAND : armor;
    }
}
