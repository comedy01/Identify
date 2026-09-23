package dev.identify.look;

import dev.identify.info.HorseStats;
import dev.identify.info.Numbers;
import dev.identify.info.TickTime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.Llama;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerDataHolder;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class EntityDetails {
    private static final int MAX_LINES = 4;
    private static final int MAX_EFFECT_NAMES = 2;
    private static final int MAX_POTENCY_LABEL = 5;

    private EntityDetails() {
    }

    public static List<Component> of(Minecraft mc, Entity entity) {
        List<Component> lines = new ArrayList<>(MAX_LINES + 2);

        if (entity instanceof LivingEntity living && living.getMaxHealth() > 0.0F) {
            lines.add(healthLine(living));
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

        addIfPresent(lines, ownerLine(mc, entity));
        addIfPresent(lines, villagerLine(entity));
        addIfPresent(lines, horseLine(entity));

        if (entity instanceof LivingEntity living) {
            if (serverTwin(mc, entity) instanceof LivingEntity twin) {
                addIfPresent(lines, effectsLine(twin));
            }
            addIfPresent(lines, heldLine(living));
        }

        return lines.size() > MAX_LINES ? new ArrayList<>(lines.subList(0, MAX_LINES)) : lines;
    }

    private static void addIfPresent(List<Component> lines, Component line) {
        if (line != null) {
            lines.add(line);
        }
    }

    private static Component healthLine(LivingEntity living) {
        String health = Numbers.trim(living.getHealth());
        String max = Numbers.trim(living.getMaxHealth());
        int armor = living.getArmorValue();
        if (armor > 0) {
            return Component.translatable("identify.entity.health_armor", health, max, armor);
        }
        return Component.translatable("identify.entity.health", health, max);
    }

    private static Component ownerLine(Minecraft mc, Entity entity) {
        boolean tame;
        EntityReference<LivingEntity> owner;
        if (entity instanceof TamableAnimal pet) {
            tame = pet.isTame();
            owner = pet.getOwnerReference();
        } else if (entity instanceof AbstractHorse horse) {
            tame = horse.isTamed();
            owner = horse.getOwnerReference();
        } else {
            return null;
        }
        if (!tame) {
            return null;
        }
        String name = owner == null ? null : playerName(mc, owner.getUUID());
        if (name != null) {
            return Component.translatable("identify.entity.owner", name);
        }
        return Component.translatable("identify.entity.tamed");
    }

    private static String playerName(Minecraft mc, UUID uuid) {
        ClientPacketListener connection = mc.getConnection();
        if (connection == null) {
            return null;
        }
        PlayerInfo info = connection.getPlayerInfo(uuid);
        return info == null ? null : info.getProfile().name();
    }

    private static Component villagerLine(Entity entity) {
        if (!(entity instanceof VillagerDataHolder holder)) {
            return null;
        }
        VillagerData data = holder.getVillagerData();
        if (data.profession().is(VillagerProfession.NONE)) {
            boolean baby = entity instanceof LivingEntity living && living.isBaby();
            return baby ? null : Component.translatable("identify.entity.unemployed");
        }
        Component profession = data.profession().value().name();
        if (data.profession().is(VillagerProfession.NITWIT)) {
            return profession;
        }
        return Component.translatable(
                "identify.entity.villager", profession, Component.translatable("merchant.level." + data.level()));
    }

    private static Component horseLine(Entity entity) {
        if (!(entity instanceof AbstractHorse horse) || horse instanceof Llama) {
            return null;
        }
        double speed = HorseStats.blocksPerSecond(horse.getAttributeValue(Attributes.MOVEMENT_SPEED));
        double jump = HorseStats.jumpHeight(horse.getAttributeValue(Attributes.JUMP_STRENGTH));
        return Component.translatable("identify.entity.horse", Numbers.trim(speed), Numbers.trim(jump));
    }

    private static Component effectsLine(LivingEntity living) {
        List<MobEffectInstance> effects = new ArrayList<>();
        for (MobEffectInstance effect : living.getActiveEffects()) {
            if (effect.isVisible()) {
                effects.add(effect);
            }
        }
        if (effects.isEmpty()) {
            return null;
        }

        MutableComponent names = Component.empty();
        int shown = Math.min(effects.size(), MAX_EFFECT_NAMES);
        for (int i = 0; i < shown; i++) {
            if (i > 0) {
                names.append(", ");
            }
            names.append(effectName(effects.get(i)));
        }
        if (effects.size() > shown) {
            names.append(" +" + (effects.size() - shown));
        }
        return Component.translatable("identify.entity.effects", names);
    }

    private static Component effectName(MobEffectInstance effect) {
        Component name = effect.getEffect().value().getDisplayName();
        int amplifier = effect.getAmplifier();
        if (amplifier >= 1 && amplifier <= MAX_POTENCY_LABEL) {
            return Component.translatable("potion.withAmplifier", name, Component.translatable("potion.potency." + amplifier));
        }
        return name;
    }

    private static Component heldLine(LivingEntity living) {
        ItemStack held = living.getMainHandItem();
        if (held.isEmpty()) {
            return null;
        }
        return Component.translatable("identify.entity.holding", held.getHoverName());
    }

    private static Integer serverAge(Minecraft mc, Entity entity) {
        if (serverTwin(mc, entity) instanceof AgeableMob ageable) {
            return ageable.getAge();
        }
        return null;
    }

    private static Entity serverTwin(Minecraft mc, Entity entity) {
        MinecraftServer server = mc.getSingleplayerServer();
        ClientLevel clientLevel = mc.level;
        if (server == null || clientLevel == null) {
            return null;
        }
        ServerLevel serverLevel = server.getLevel(clientLevel.dimension());
        if (serverLevel == null) {
            return null;
        }
        return serverLevel.getEntity(entity.getId());
    }
}
