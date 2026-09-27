package dev.identify.showcase.mixin;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MouseHandler.class)
public interface MouseHandlerAccessor {
    @Accessor("xpos")
    void idShowcase$setX(double x);

    @Accessor("ypos")
    void idShowcase$setY(double y);
}
