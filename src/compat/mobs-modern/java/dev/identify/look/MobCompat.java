package dev.identify.look;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.Llama;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerDataHolder;
import net.minecraft.world.entity.npc.villager.VillagerProfession;

import java.util.UUID;

final class MobCompat {
    private MobCompat() {
    }

    static boolean isTamed(Entity entity) {
        if (entity instanceof TamableAnimal pet) {
            return pet.isTame();
        }
        return entity instanceof AbstractHorse horse && horse.isTamed();
    }

    static UUID owner(Entity entity) {
        EntityReference<LivingEntity> owner = null;
        if (entity instanceof TamableAnimal pet) {
            owner = pet.getOwnerReference();
        } else if (entity instanceof AbstractHorse horse) {
            owner = horse.getOwnerReference();
        }
        return owner == null ? null : owner.getUUID();
    }

    static boolean isRideable(Entity entity) {
        return entity instanceof AbstractHorse && !(entity instanceof Llama);
    }

    static Component villagerLine(Entity entity) {
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
}
