package dev.identify.look;

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
        double sum = 0.0D;
        for (AttributeModifier modifier : stack.getAttributeModifiers(slot).get(attribute(stat))) {
            if (modifier.getOperation() == AttributeModifier.Operation.ADDITION) {
                sum += modifier.getAmount();
            }
        }
        return sum;
    }

    private static Attribute attribute(String stat) {
        return switch (stat) {
            case "armor" -> Attributes.ARMOR;
            case "toughness" -> Attributes.ARMOR_TOUGHNESS;
            case "damage" -> Attributes.ATTACK_DAMAGE;
            default -> Attributes.ATTACK_SPEED;
        };
    }
}
