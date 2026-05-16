package com.github.jpmcodes.spawner.utils;

import net.minecraft.server.v1_5_R3.EntityLiving;
import net.minecraft.server.v1_5_R3.NBTTagCompound;
import net.minecraft.server.v1_5_R3.PathfinderGoalSelector;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_5_R3.entity.CraftEntity;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.StringJoiner;

public class LocationUtils {

    public static String toString(Location value) {
        if (value == null || value.getWorld() == null) return "";
        return value.getWorld().getName() + ";" +
                value.getBlockX() + ";" +
                value.getBlockY() + ";" +
                value.getBlockZ() + ";" +
                value.getYaw() + ";" +
                value.getPitch();
    }

    public static Location fromString(String value) {
        if(value == null || value.isEmpty()) return null;

        final String[] split = value.split(";");
        if (split.length < 6) return null;

        return new Location(
                Bukkit.getWorld(split[0]),
                Double.parseDouble(split[1]),
                Double.parseDouble(split[2]),
                Double.parseDouble(split[3]),
                Float.parseFloat(split[4]),
                Float.parseFloat(split[5])
        );
    }


    public static String serialize(Location location, boolean yawAndPitch) {
        if (location == null || location.getWorld() == null)
            return null;

        final StringJoiner joiner = new StringJoiner(";");
        joiner.add(location.getWorld().getName());
        joiner.add(String.valueOf(location.getX()));
        joiner.add(String.valueOf(location.getY()));
        joiner.add(String.valueOf(location.getZ()));

        if (yawAndPitch) {
            joiner.add(String.valueOf(location.getYaw()));
            joiner.add(String.valueOf(location.getPitch()));
        }

        return joiner.toString();
    }

    public static String getFormattedText(Location location) {
        if (location == null || location.getWorld() == null) return ChatColor.RED + "Localização inválida";
        String text = String.format("%s, %d, %d, %d", location.getWorld().getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
        return ChatColor.GRAY + text;
    }

    /**
     * Busca uma entidade viva específica próxima a uma localização.
     * * @param location A localização central da busca.
     *
     * @param radius O raio de distância máximo em blocos.
     * @param type   O tipo da entidade que estamos procurando.
     * @return A LivingEntity encontrada, ou null se não encontrar nenhuma.
     */
    public static LivingEntity getNearbyLivingEntity(Location location, double radius, EntityType type) {
        World world = location.getWorld();
        double radiusSquared = radius * radius;

        int centerChunkX = location.getBlockX() >> 4;
        int centerChunkZ = location.getBlockZ() >> 4;
        int chunkRadius = (int) Math.ceil(radius / 16.0);

        for (int x = centerChunkX - chunkRadius; x <= centerChunkX + chunkRadius; x++) {
            for (int z = centerChunkZ - chunkRadius; z <= centerChunkZ + chunkRadius; z++) {

                if (!world.isChunkLoaded(x, z)) continue;

                for (Entity near : world.getChunkAt(x, z).getEntities()) {
                    if (near.getLocation().distanceSquared(location) > radiusSquared) continue;
                    if (!(near instanceof LivingEntity) || near.isDead()) continue;

                    // Se encontrou o tipo que estamos procurando, retorna ele na hora!
                    if (near.getType() == type) {
                        return (LivingEntity) near;
                    }
                }
            }
        }
        return null;
    }


    public static void setNoAI(Entity bukkitEntity) {
        // 1. Tenta limpar a AI via NMS (1.5.2)
        try {
            net.minecraft.server.v1_5_R3.Entity nmsEntity = ((CraftEntity) bukkitEntity).getHandle();
            if (nmsEntity instanceof EntityLiving) {
                EntityLiving living = (EntityLiving) nmsEntity;

                // Em 1.5.2, os seletores estão no EntityLiving
                Field goalSelectorField = EntityLiving.class.getDeclaredField("goalSelector");
                goalSelectorField.setAccessible(true);
                PathfinderGoalSelector goalSelector = (PathfinderGoalSelector) goalSelectorField.get(living);

                Field targetSelectorField = EntityLiving.class.getDeclaredField("targetSelector");
                targetSelectorField.setAccessible(true);
                PathfinderGoalSelector targetSelector = (PathfinderGoalSelector) targetSelectorField.get(living);

                // O campo 'a' no PathfinderGoalSelector é a lista de tarefas (UnsafeList)
                Field listField = PathfinderGoalSelector.class.getDeclaredField("a");
                listField.setAccessible(true);

                ((Collection) listField.get(goalSelector)).clear();
                ((Collection) listField.get(targetSelector)).clear();
            }
        } catch (Exception ignored) {
        }

        // 2. Mantém o NBT como fallback (útil se o servidor tiver patches de versões superiores)
        try {
            net.minecraft.server.v1_5_R3.Entity nmsEntity = ((CraftEntity) bukkitEntity).getHandle();
            NBTTagCompound tag = new NBTTagCompound();
            nmsEntity.c(tag);
            tag.setInt("NoAI", 1);
            nmsEntity.f(tag);
        } catch (Exception ignored) {
        }
    }

}