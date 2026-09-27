package dev.identify.look;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

final class ModifierCompat {
    private ModifierCompat() {
    }

    static double total(ItemStack stack, EquipmentSlot slot, String stat) {
        if (stack.isEmpty()) {
            return 0.0D;
        }
        Holder<Attribute> attribute = attribute(stat);
        double[] sum = {0.0D};
        stack.forEachModifier(slot, (holder, modifier) -> {
            if (holder.equals(attribute) && modifier.operation() == AttributeModifier.Operation.ADD_VALUE) {
                sum[0] += modifier.amount();
            }
        });
        return sum[0];
    }

    private static Holder<Attribute> attribute(String stat) {
        return switch (stat) {
            case "armor" -> Attributes.ARMOR;
            case "toughness" -> Attributes.ARMOR_TOUGHNESS;
            case "damage" -> Attributes.ATTACK_DAMAGE;
            default -> Attributes.ATTACK_SPEED;
        };
    }
}
