package dev.identify.look;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.ItemStack;

final class EquipCompat {
    private EquipCompat() {
    }

    static EquipmentSlot armorSlot(ItemStack stack) {
        Equipable equipable = Equipable.get(stack);
        if (equipable != null && equipable.getEquipmentSlot().isArmor()) {
            return equipable.getEquipmentSlot();
        }
        return null;
    }
}
