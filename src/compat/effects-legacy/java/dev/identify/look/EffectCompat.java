package dev.identify.look;

import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;

final class EffectCompat {
    private EffectCompat() {
    }

    static Component name(MobEffectInstance effect) {
        return effect.getEffect().getDisplayName();
    }
}
