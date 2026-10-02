package dev.identify.look;

import net.minecraft.item.ItemStack;

import java.util.List;

public final class LookTarget {
    private final String name;
    private final ItemStack icon;
    private final String namespace;
    private final List<String> details;

    LookTarget(String name, ItemStack icon, String namespace, List<String> details) {
        this.name = name;
        this.icon = icon;
        this.namespace = namespace;
        this.details = details;
    }

    public String name() {
        return name;
    }

    public ItemStack icon() {
        return icon;
    }

    public String namespace() {
        return namespace;
    }

    public List<String> details() {
        return details;
    }
}
