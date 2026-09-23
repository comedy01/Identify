package dev.identify.look;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public record LookTarget(Component name, ItemStack icon, String namespace, List<Component> details) {
}
