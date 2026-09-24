package dev.identify.fabric;

import dev.identify.client.Ids;
import dev.identify.hud.Canvas;
import dev.identify.hud.IdentifyHudRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;

final class HudHook {
    private HudHook() {
    }

    static void register() {
        HudElementRegistry.addLast(Ids.of("look_info"), (graphics, deltaTracker) ->
                IdentifyHudRenderer.render(new Canvas(graphics), deltaTracker));
    }
}
