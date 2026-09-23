package dev.identify.look;

import dev.identify.info.TickTime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class EntityDetails {
    private EntityDetails() {
    }

    public static List<Component> of(Minecraft mc, Entity entity) {
        List<Component> lines = new ArrayList<>(3);

        if (entity instanceof LivingEntity living && living.getMaxHealth() > 0.0F) {
            lines.add(Component.translatable(
                    "identify.entity.health",
                    format(living.getHealth()),
                    format(living.getMaxHealth())));
        }

        if (entity instanceof AgeableMob ageable) {
            Integer exactAge = serverAge(mc, entity);
            if (exactAge != null) {
                if (exactAge < 0) {
                    lines.add(Component.translatable("identify.entity.baby_grows", TickTime.clock(-exactAge)));
                } else if (exactAge > 0) {
                    lines.add(Component.translatable("identify.entity.breed_cooldown", TickTime.clock(exactAge)));
                }
            } else if (ageable.isBaby()) {
                lines.add(Component.translatable("identify.entity.baby"));
            }
        }
        return lines;
    }

    private static Integer serverAge(Minecraft mc, Entity entity) {
        MinecraftServer server = mc.getSingleplayerServer();
        ClientLevel clientLevel = mc.level;
        if (server == null || clientLevel == null) {
            return null;
        }
        ServerLevel serverLevel = server.getLevel(clientLevel.dimension());
        if (serverLevel == null) {
            return null;
        }
        Entity twin = serverLevel.getEntity(entity.getId());
        if (twin instanceof AgeableMob ageable) {
            return ageable.getAge();
        }
        return null;
    }

    private static String format(float value) {
        if (Math.abs(value - Math.round(value)) < 0.05F) {
            return Integer.toString(Math.round(value));
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }
}
