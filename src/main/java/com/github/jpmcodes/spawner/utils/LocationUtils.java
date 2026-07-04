package com.github.jpmcodes.spawner.utils;

import com.github.jpmcodes.spawner.JSpawnerPlugin;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.StringJoiner;
import net.minecraft.server.v1_5_R3.EntityLiving;
import net.minecraft.server.v1_5_R3.PathfinderGoalSelector;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_5_R3.entity.CraftEntity;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class LocationUtils {
    private static Field F_GOAL;
    private static Field F_TARGET;

    public static String toString(Location value) {
        if (value == null || value.getWorld() == null)
            return "";
        return value.getWorld().getName() + ";" + value
                .getBlockX() + ";"
                + value
                        .getBlockY()
                + ";" + value
                        .getBlockZ()
                + ";" + value
                        .getYaw()
                + ";" + value
                        .getPitch();
    }

    private static Field F_SEL_A;
    private static Field F_SEL_B;

    public static Location fromString(String value) {
        if (value == null || value.isEmpty())
            return null;

        String[] split = value.split(";");
        if (split.length < 6)
            return null;

        return new Location(
                Bukkit.getWorld(split[0]),
                Double.parseDouble(split[1]),
                Double.parseDouble(split[2]),
                Double.parseDouble(split[3]),
                Float.parseFloat(split[4]),
                Float.parseFloat(split[5]));
    }

    public static String serialize(Location location, boolean yawAndPitch) {
        if (location == null || location.getWorld() == null) {
            return null;
        }
        StringJoiner joiner = new StringJoiner(";");
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
        if (location == null || location.getWorld() == null)
            return ChatColor.RED + "Localização inválida";
        String text = String.format("%s, %d, %d, %d",
                new Object[] { location.getWorld().getName(), Integer.valueOf(location.getBlockX()),
                        Integer.valueOf(location.getBlockY()), Integer.valueOf(location.getBlockZ()) });
        return ChatColor.GRAY + text;
    }

    public static LivingEntity getNearbyLivingEntity(Location location, double radius, EntityType type) {
        World world = location.getWorld();
        if (world == null)
            return null;

        double radiusSquared = radius * radius;
        int chunkRadius = (int) Math.ceil(radius / 16.0D);
        int centerChunkX = location.getBlockX() >> 4;
        int centerChunkZ = location.getBlockZ() >> 4;

        for (int x = centerChunkX - chunkRadius; x <= centerChunkX + chunkRadius; x++) {
            for (int z = centerChunkZ - chunkRadius; z <= centerChunkZ + chunkRadius; z++) {
                // Forçamos o carregamento do chunk para garantir que o mob "exista" para o Bukkit.
                org.bukkit.Chunk chunk = world.getChunkAt(x, z);
                if (chunk != null) {
                    for (Entity near : chunk.getEntities()) {
                        if (near instanceof LivingEntity && !near.isDead() && near.getType() == type) {
                            if (near.getLocation().distanceSquared(location) <= radiusSquared) {
                                return (LivingEntity) near;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    public static java.util.List<LivingEntity> getNearbyLivingEntities(Location location, double radius, EntityType type) {
        World world = location.getWorld();
        java.util.List<LivingEntity> entities = new java.util.ArrayList<>();
        if (world == null)
            return entities;

        double radiusSquared = radius * radius;
        int chunkRadius = (int) Math.ceil(radius / 16.0D);
        int centerChunkX = location.getBlockX() >> 4;
        int centerChunkZ = location.getBlockZ() >> 4;

        for (int x = centerChunkX - chunkRadius; x <= centerChunkX + chunkRadius; x++) {
            for (int z = centerChunkZ - chunkRadius; z <= centerChunkZ + chunkRadius; z++) {
                // Forçamos o carregamento do chunk para garantir que o mob "exista" para o Bukkit.
                org.bukkit.Chunk chunk = world.getChunkAt(x, z);
                if (chunk != null) {
                    for (Entity near : chunk.getEntities()) {
                        if (near instanceof LivingEntity && !near.isDead() && near.getType() == type) {
                            if (near.getLocation().distanceSquared(location) <= radiusSquared) {
                                entities.add((LivingEntity) near);
                            }
                        }
                    }
                }
            }
        }
        return entities;
    }

    static {
        try {
            F_GOAL = EntityLiving.class.getDeclaredField("goalSelector");
            F_TARGET = EntityLiving.class.getDeclaredField("targetSelector");
            F_GOAL.setAccessible(true);
            F_TARGET.setAccessible(true);

            F_SEL_A = PathfinderGoalSelector.class.getDeclaredField("a");
            F_SEL_A.setAccessible(true);
            try {
                F_SEL_B = PathfinderGoalSelector.class.getDeclaredField("b");
                F_SEL_B.setAccessible(true);
            } catch (NoSuchFieldException noSuchFieldException) {
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void freeze(LivingEntity le, Plugin plugin) {
        if (le == null) {
            return;
        }
        if (le.hasMetadata("frozen-ai")) {
            return;
        }

        le.setMetadata("frozen-ai", (MetadataValue) new FixedMetadataValue(plugin, Boolean.valueOf(true)));
        le.setMetadata("freeze", (MetadataValue) new FixedMetadataValue(plugin, Boolean.valueOf(true)));
        le.setRemoveWhenFarAway(false);

        if (le instanceof Creature) {
            ((Creature) le).setTarget(null);
        }

        le.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 2147483647, 127, false), true);
        le.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 2147483647, 127, false), true);

        try {
            net.minecraft.server.v1_5_R3.Entity nms = ((CraftEntity) le).getHandle();
            if (nms instanceof EntityLiving) {
                EntityLiving el = (EntityLiving) nms;

                PathfinderGoalSelector goal = (PathfinderGoalSelector) F_GOAL.get(el);
                PathfinderGoalSelector target = (PathfinderGoalSelector) F_TARGET.get(el);

                clearSelector(goal);
                clearSelector(target);
            }
        } catch (Exception exception) {
        }

        le.setVelocity(new Vector(0, 0, 0));
    }

    private static void clearSelector(PathfinderGoalSelector sel) {
        try {
            Object a = F_SEL_A.get(sel);
            if (a instanceof Collection)
                ((Collection) a).clear();

            if (F_SEL_B != null) {
                Object b = F_SEL_B.get(sel);
                if (b instanceof Collection)
                    ((Collection) b).clear();
            }
        } catch (Exception exception) {
        }
    }

    public static void setNoAI(Entity bukkitEntity) {
        if (bukkitEntity instanceof LivingEntity) {
            freeze((LivingEntity) bukkitEntity, (Plugin) JSpawnerPlugin.getInstance());
        }
    }

    public static void updateCustomName(LivingEntity entity, String name) {
        if (entity == null || name == null)
            return;
        String current = entity.getCustomName();
        if (current == null || !current.equals(name)) {
            entity.setCustomName(name);
            entity.setCustomNameVisible(true);
        }
    }
}