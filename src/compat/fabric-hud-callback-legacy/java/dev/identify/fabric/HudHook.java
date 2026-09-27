package dev.identify.fabric;

import dev.identify.hud.Canvas;
import dev.identify.hud.IdentifyHudRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

final class HudHook {
    private HudHook() {
    }

    static void register() {
        HudRenderCallback.EVENT.register((graphics, tickDelta) ->
                IdentifyHudRenderer.render(new Canvas(graphics), tickDelta));
    }
}
