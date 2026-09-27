package dev.identify.look;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;

final class EquipCompat {
    private EquipCompat() {
    }

    static EquipmentSlot armorSlot(ItemStack stack) {
        EquipmentSlot slot = Mob.getEquipmentSlotForItem(stack);
        return slot.getType() == EquipmentSlot.Type.ARMOR ? slot : null;
    }
}
