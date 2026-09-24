package dev.identify.look;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerDataHolder;
import net.minecraft.world.entity.npc.VillagerProfession;

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
        if (entity instanceof TamableAnimal pet) {
            return pet.getOwnerUUID();
        }
        if (entity instanceof AbstractHorse horse) {
            return horse.getOwnerUUID();
        }
        return null;
    }

    static boolean isRideable(Entity entity) {
        return entity instanceof AbstractHorse && !(entity instanceof Llama);
    }

    static Component villagerLine(Entity entity) {
        if (!(entity instanceof VillagerDataHolder holder)) {
            return null;
        }
        VillagerData data = holder.getVillagerData();
        VillagerProfession job = data.getProfession();
        if (job == VillagerProfession.NONE) {
            boolean baby = entity instanceof LivingEntity living && living.isBaby();
            return baby ? null : Component.translatable("identify.entity.unemployed");
        }
        Component profession = Component.translatable(EntityType.VILLAGER.getDescriptionId() + "."
                + BuiltInRegistries.VILLAGER_PROFESSION.getKey(job).getPath());
        if (job == VillagerProfession.NITWIT) {
            return profession;
        }
        return Component.translatable(
                "identify.entity.villager", profession, Component.translatable("merchant.level." + data.getLevel()));
    }
}
