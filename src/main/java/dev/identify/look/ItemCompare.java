package dev.identify.look;

import dev.identify.info.Numbers;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class ItemCompare {
    private record Stat(Holder<Attribute> attribute, String key) {
    }

    private static final List<Stat> STATS = List.of(
            new Stat(Attributes.ARMOR, "armor"),
            new Stat(Attributes.ARMOR_TOUGHNESS, "toughness"),
            new Stat(Attributes.ATTACK_DAMAGE, "damage"),
            new Stat(Attributes.ATTACK_SPEED, "speed"));

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
            lines.add(Component.translatable("identify.compare.line", parts).withStyle(ChatFormatting.GRAY));
        } else {
            lines.add(Component.translatable("identify.compare.hint").withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    public static Component difference(ItemStack stack, ItemStack equipped, EquipmentSlot slot) {
        if (ItemStack.matches(stack, equipped)) {
            return null;
        }

        MutableComponent parts = Component.empty();
        boolean any = false;
        for (Stat stat : STATS) {
            double now = total(stack, slot, stat.attribute());
            double delta = now - total(equipped, slot, stat.attribute());
            if (now == 0.0D || !Numbers.significant(delta)) {
                continue;
            }
            if (any) {
                parts.append(Component.literal(", ").withStyle(ChatFormatting.GRAY));
            }
            parts.append(Component.literal(Numbers.signed(delta) + " ")
                    .append(Component.translatable("identify.compare." + stat.key()))
                    .withStyle(delta > 0.0D ? ChatFormatting.GREEN : ChatFormatting.RED));
            any = true;
        }
        return any ? parts : null;
    }

    public static EquipmentSlot slotFor(ItemStack stack) {
        EquipmentSlot armor = EquipCompat.armorSlot(stack);
        return armor == null ? EquipmentSlot.MAINHAND : armor;
    }

    private static double total(ItemStack stack, EquipmentSlot slot, Holder<Attribute> attribute) {
        if (stack.isEmpty()) {
            return 0.0D;
        }
        double[] sum = {0.0D};
        stack.forEachModifier(slot, (holder, modifier) -> {
            if (holder.equals(attribute) && modifier.operation() == AttributeModifier.Operation.ADD_VALUE) {
                sum[0] += modifier.amount();
            }
        });
        return sum[0];
    }
}
