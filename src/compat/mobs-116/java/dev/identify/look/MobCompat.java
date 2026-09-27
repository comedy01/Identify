package dev.identify.look;

import dev.identify.Texts;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.AgableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerDataHolder;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;

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

    static boolean isAgeable(Entity entity) {
        return entity instanceof AgableMob;
    }

    static Integer age(Entity entity) {
        return entity instanceof AgableMob ageable ? ageable.getAge() : null;
    }

    static ItemStack pickResult(Entity entity) {
        return entity.getPickedResult(new EntityHitResult(entity));
    }

    static Component villagerLine(Entity entity) {
        if (!(entity instanceof VillagerDataHolder holder)) {
            return null;
        }
        VillagerData data = holder.getVillagerData();
        VillagerProfession job = data.getProfession();
        if (job == VillagerProfession.NONE) {
            boolean baby = entity instanceof LivingEntity living && living.isBaby();
            return baby ? null : Texts.translatable("identify.entity.unemployed");
        }
        Component profession = Texts.translatable(EntityType.VILLAGER.getDescriptionId() + "."
                + Registry.VILLAGER_PROFESSION.getKey(job).getPath());
        if (job == VillagerProfession.NITWIT) {
            return profession;
        }
        return Texts.translatable(
                "identify.entity.villager", profession, Texts.translatable("merchant.level." + data.getLevel()));
    }
}
