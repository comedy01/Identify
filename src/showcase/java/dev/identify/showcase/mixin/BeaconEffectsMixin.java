package dev.identify.showcase.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BeaconBlockEntity.class)
abstract class BeaconEffectsMixin {
    @Inject(method = "applyEffects", at = @At("HEAD"), cancellable = true)
    private static void idShowcase$noEffects(Level level, BlockPos pos, int levels, Holder<MobEffect> primary,
            Holder<MobEffect> secondary, CallbackInfo ci) {
        ci.cancel();
    }
}
