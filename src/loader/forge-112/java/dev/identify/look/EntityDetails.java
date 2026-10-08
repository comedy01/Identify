package dev.identify.look;

import dev.identify.info.HorseStats;
import dev.identify.info.Numbers;
import dev.identify.info.TickTime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.passive.AbstractHorse;
import net.minecraft.entity.passive.EntityLlama;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class EntityDetails {
    private static final int MAX_LINES = 4;
    private static final int MAX_EFFECT_NAMES = 2;
    private static final int MAX_POTENCY_LABEL = 5;

    private EntityDetails() {
    }

    public static List<String> of(Minecraft mc, Entity entity) {
        List<String> lines = new ArrayList<>(MAX_LINES + 2);
        EntityLivingBase living = entity instanceof EntityLivingBase ? (EntityLivingBase) entity : null;
        Entity twin = serverTwin(mc, entity);

        if (living != null && living.getMaxHealth() > 0.0F) {
            lines.add(healthLine(living));
        }

        if (entity instanceof EntityAgeable) {
            if (twin instanceof EntityAgeable) {
                int age = ((EntityAgeable) twin).getGrowingAge();
                if (age < 0) {
                    lines.add(I18n.format("identify.entity.baby_grows", TickTime.clock(-age)));
                } else if (age > 0) {
                    lines.add(I18n.format("identify.entity.breed_cooldown", TickTime.clock(age)));
                }
            } else if (living != null && living.isChild()) {
                lines.add(I18n.format("identify.entity.baby"));
            }
        }

        addIfPresent(lines, ownerLine(mc, entity));
        addIfPresent(lines, horseLine(entity));

        if (living != null) {
            if (twin instanceof EntityLivingBase) {
                addIfPresent(lines, effectsLine((EntityLivingBase) twin));
            }
            addIfPresent(lines, heldLine(living));
        }

        return lines.size() > MAX_LINES ? new ArrayList<>(lines.subList(0, MAX_LINES)) : lines;
    }

    private static void addIfPresent(List<String> lines, String line) {
        if (line != null) {
            lines.add(line);
        }
    }

    private static String healthLine(EntityLivingBase living) {
        String health = Numbers.trim(living.getHealth());
        String max = Numbers.trim(living.getMaxHealth());
        int armor = living.getTotalArmorValue();
        if (armor > 0) {
            return I18n.format("identify.entity.health_armor", health, max, armor);
        }
        return I18n.format("identify.entity.health", health, max);
    }

    private static String ownerLine(Minecraft mc, Entity entity) {
        UUID owner;
        if (entity instanceof EntityTameable && ((EntityTameable) entity).isTamed()) {
            owner = ((EntityTameable) entity).getOwnerId();
        } else if (entity instanceof AbstractHorse && ((AbstractHorse) entity).isTame()) {
            owner = ((AbstractHorse) entity).getOwnerUniqueId();
        } else {
            return null;
        }
        String name = owner == null ? null : playerName(mc, owner);
        if (name != null) {
            return I18n.format("identify.entity.owner", name);
        }
        return I18n.format("identify.entity.tamed");
    }

    private static String playerName(Minecraft mc, UUID uuid) {
        NetHandlerPlayClient connection = mc.getConnection();
        if (connection == null) {
            return null;
        }
        NetworkPlayerInfo info = connection.getPlayerInfo(uuid);
        return info == null ? null : info.getGameProfile().getName();
    }

    private static String horseLine(Entity entity) {
        if (!(entity instanceof AbstractHorse) || entity instanceof EntityLlama) {
            return null;
        }
        AbstractHorse horse = (AbstractHorse) entity;
        double speed = HorseStats.blocksPerSecond(
                horse.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getAttributeValue());
        double jump = HorseStats.jumpHeight(horse.getHorseJumpStrength());
        return I18n.format("identify.entity.horse", Numbers.trim(speed), Numbers.trim(jump));
    }

    private static String effectsLine(EntityLivingBase living) {
        List<PotionEffect> effects = new ArrayList<>();
        try {
            for (PotionEffect effect : living.getActivePotionEffects()) {
                if (effect.doesShowParticles()) {
                    effects.add(effect);
                }
            }
        } catch (RuntimeException e) {
            return null;
        }
        if (effects.isEmpty()) {
            return null;
        }

        StringBuilder names = new StringBuilder();
        int shown = Math.min(effects.size(), MAX_EFFECT_NAMES);
        for (int i = 0; i < shown; i++) {
            if (i > 0) {
                names.append(", ");
            }
            names.append(effectName(effects.get(i)));
        }
        if (effects.size() > shown) {
            names.append(" +").append(effects.size() - shown);
        }
        return I18n.format("identify.entity.effects", names.toString());
    }

    private static String effectName(PotionEffect effect) {
        String name = I18n.format(effect.getEffectName());
        int amplifier = effect.getAmplifier();
        if (amplifier >= 1 && amplifier <= MAX_POTENCY_LABEL) {
            return name + " " + I18n.format("potion.potency." + amplifier);
        }
        return name;
    }

    private static String heldLine(EntityLivingBase living) {
        ItemStack held = living.getHeldItemMainhand();
        if (held.isEmpty()) {
            return null;
        }
        return I18n.format("identify.entity.holding", held.getDisplayName());
    }

    private static Entity serverTwin(Minecraft mc, Entity entity) {
        World clientWorld = mc.world;
        if (mc.getIntegratedServer() == null || clientWorld == null) {
            return null;
        }
        try {
            WorldServer serverWorld = DimensionManager.getWorld(clientWorld.provider.getDimension());
            if (serverWorld == null) {
                return null;
            }
            return serverWorld.getEntityByID(entity.getEntityId());
        } catch (RuntimeException e) {
            return null;
        }
    }
}
