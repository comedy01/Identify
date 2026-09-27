package dev.identify.showcase;

import net.fabricmc.api.ClientModInitializer;

public final class IdentifyShowcaseFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Showcase.start();
    }
}
