package dev.identify.selftest.forge;

import dev.identify.selftest.IdentifySelfTest;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

@Mod("identify_selftest")
public final class IdentifySelfTestForge {
    public IdentifySelfTestForge() {
        IdentifySelfTest test = new IdentifySelfTest(FMLPaths.CONFIGDIR.get(), ConfigScreens::open);
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END) {
                test.tick();
            }
        });
    }
}
