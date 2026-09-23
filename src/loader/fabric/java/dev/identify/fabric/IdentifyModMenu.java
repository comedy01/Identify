package dev.identify.fabric;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.identify.client.gui.IdentifySettingsScreen;
import net.minecraft.client.Minecraft;

public final class IdentifyModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new IdentifySettingsScreen(parent, Minecraft.getInstance().options);
    }
}
