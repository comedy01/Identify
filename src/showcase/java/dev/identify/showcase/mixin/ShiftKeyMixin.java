package dev.identify.showcase.mixin;

import dev.identify.showcase.Showcase;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
abstract class ShiftKeyMixin {
    @Inject(method = "hasShiftDown", at = @At("HEAD"), cancellable = true)
    private void idShowcase$shift(CallbackInfoReturnable<Boolean> cir) {
        if (Showcase.shiftDown()) {
            cir.setReturnValue(true);
        }
    }
}
